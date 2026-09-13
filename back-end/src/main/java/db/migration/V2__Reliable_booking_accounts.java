package db.migration;

import org.flywaydb.core.api.migration.*;

import java.sql.*;
import java.util.*;

/** 先检查旧账，再执行可重试 DDL；绝不以重算方式覆盖历史余额。 */
public class V2__Reliable_booking_accounts extends BaseJavaMigration {
    private Connection db;

    public void migrate(Context context) throws Exception {
        db = context.getConnection();
        preflight();
        add("admin", "token_version", "INT NOT NULL DEFAULT 0");
        add("course", "tag", "VARCHAR(30) NULL");
        add("course_schedule", "notice_id", "BIGINT NULL");
        exec(
                "CREATE TABLE IF NOT EXISTS notice(id BIGINT PRIMARY KEY AUTO_INCREMENT,store_id"
                    + " BIGINT NOT NULL,name VARCHAR(100) NOT NULL,content TEXT NOT NULL,sort INT"
                    + " NOT NULL DEFAULT 0,status TINYINT NOT NULL DEFAULT 1,created_at DATETIME"
                    + " DEFAULT CURRENT_TIMESTAMP,updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON"
                    + " UPDATE CURRENT_TIMESTAMP)");
        add("course_hour_batch", "frozen", "INT NOT NULL DEFAULT 0");
        add("booking", "cost_hours", "INT NOT NULL DEFAULT 1");
        add("booking", "checkin_points", "INT NOT NULL DEFAULT 0");
        add("redeem_order", "pickup_admin_id", "BIGINT NULL");
        add("redeem_order", "picked_up_at", "DATETIME NULL");
        add("redeem_order", "pickup_code", "VARCHAR(12) NULL");
        exec(
                "CREATE TABLE IF NOT EXISTS booking_hour_allocation(booking_id BIGINT NOT"
                        + " NULL,batch_id BIGINT NOT NULL,hours INT NOT NULL,state VARCHAR(20) NOT"
                        + " NULL,PRIMARY KEY(booking_id,batch_id),INDEX"
                        + " idx_batch_state(batch_id,state),CHECK(hours>0)) ENGINE=InnoDB");
        exec(
                "CREATE TABLE IF NOT EXISTS checkin_code(token_hash CHAR(64) PRIMARY"
                        + " KEY,schedule_id BIGINT NOT NULL,expires_at DATETIME NOT NULL,INDEX"
                        + " idx_expiry(expires_at)) ENGINE=InnoDB");
        exec(
                "CREATE TABLE IF NOT EXISTS operation_request(scope VARCHAR(100) NOT"
                        + " NULL,request_key VARCHAR(80) NOT NULL,payload_hash CHAR(64) NOT"
                        + " NULL,result_json LONGTEXT NOT NULL,created_at DATETIME DEFAULT"
                        + " CURRENT_TIMESTAMP,PRIMARY KEY(scope,request_key)) ENGINE=InnoDB");
        exec(
                "CREATE TABLE IF NOT EXISTS configuration_change(id BIGINT PRIMARY KEY"
                        + " AUTO_INCREMENT,admin_id BIGINT NOT NULL,config_key VARCHAR(64) NOT"
                        + " NULL,old_value VARCHAR(255),new_value VARCHAR(255),created_at DATETIME"
                        + " DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
        add(
                "booking",
                "active_unique",
                "TINYINT GENERATED ALWAYS AS (CASE WHEN status IN ('booked','waitlist','checked')"
                        + " THEN 1 ELSE NULL END) STORED");
        if (!index("booking", "uk_active_booking"))
            exec(
                    "CREATE UNIQUE INDEX uk_active_booking ON"
                            + " booking(user_id,schedule_id,active_unique)");
        // 数据迁移整体事务；MySQL DDL 在上面独立完成，失败时保留 DDL 并可用 flyway repair 后重试。
        boolean auto = db.getAutoCommit();
        db.setAutoCommit(false);
        try {
            exec(
                    "UPDATE booking b LEFT JOIN course_schedule s ON s.id=b.schedule_id LEFT JOIN"
                        + " course c ON c.id=s.course_id SET"
                        + " b.cost_hours=COALESCE(NULLIF(b.locked_hours,0),c.cost_hours,1),b.checkin_points=COALESCE((SELECT"
                        + " MAX(p.amount) FROM point_log p WHERE p.ref_id=b.id AND"
                        + " p.type='checkin'),(SELECT CAST(config_value AS UNSIGNED) FROM config"
                        + " WHERE store_id=1 AND config_key='points_per_checkin'),100)");
            allocateLegacy();
            exec(
                    "UPDATE redeem_order SET pickup_code=UPPER(LEFT(REPLACE(UUID(),'-',''),12))"
                            + " WHERE pickup_code IS NULL");
            exec("UPDATE redeem_order SET status='ready' WHERE status='to_ship'");
            // 已真实发货的历史订单也归入到店待核实，不伪造已领取。
            exec("UPDATE redeem_order SET status='ready' WHERE status='shipping'");
            exec("UPDATE config SET config_value='1' WHERE config_key='lock_on_booking'");
            exec("UPDATE config SET config_value='0' WHERE config_key='points_per_video_like'");
            exec(
                    "DELETE FROM config WHERE config_key IN"
                        + " ('claw_cost_points','points_expire_days','checkin_window_min','default_capacity_private','points_per_video_like','lock_on_booking')");
            exec(
                    "INSERT IGNORE INTO config(store_id,config_key,config_value,remark)"
                        + " VALUES(1,'checkin_before_min','30','开课前多少分钟允许签到，截止到课程结束'),(1,'expiry_reminder_days','30','临期课时提醒天数')");
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw e;
        } finally {
            db.setAutoCommit(auto);
        }
    }

    private void preflight() throws SQLException {
        try (var s = db.createStatement();
                var r =
                        s.executeQuery(
                                "SELECT u.id,u.course_balance,u.frozen_hours,COALESCE((SELECT"
                                    + " SUM(remaining) FROM course_hour_batch WHERE"
                                    + " user_id=u.id),0) total,COALESCE((SELECT SUM(locked_hours)"
                                    + " FROM booking WHERE user_id=u.id AND status IN"
                                    + " ('booked','waitlist')),0) reserved FROM `user` u")) {
            List<Long> invalid = new ArrayList<>();
            while (r.next())
                if (r.getInt("total") != r.getInt("course_balance") + r.getInt("frozen_hours")
                        || r.getInt("reserved") != r.getInt("frozen_hours")
                        || r.getInt("course_balance") < 0) invalid.add(r.getLong("id"));
            if (!invalid.isEmpty())
                throw new SQLException("历史课时/批次/预约不一致，请先对账；user ids=" + invalid);
        }
        try (var s = db.createStatement();
                var r =
                        s.executeQuery(
                                "SELECT user_id,schedule_id FROM booking WHERE status IN"
                                        + " ('booked','waitlist','checked') GROUP BY"
                                        + " user_id,schedule_id HAVING COUNT(*)>1 LIMIT 1")) {
            if (r.next())
                throw new SQLException(
                        "历史重复有效预约：user=" + r.getLong(1) + ", schedule=" + r.getLong(2));
        }
    }

    private void allocateLegacy() throws SQLException {
        List<long[]> bookings = new ArrayList<>();
        try (var s = db.createStatement();
                var r =
                        s.executeQuery(
                                "SELECT b.id,b.user_id,b.locked_hours FROM booking b WHERE status"
                                        + " IN ('booked','waitlist') AND locked_hours>0 AND NOT"
                                        + " EXISTS(SELECT 1 FROM booking_hour_allocation a WHERE"
                                        + " a.booking_id=b.id) ORDER BY b.id")) {
            while (r.next()) bookings.add(new long[] {r.getLong(1), r.getLong(2), r.getInt(3)});
        }
        for (long[] b : bookings) {
            int left = (int) b[2];
            List<long[]> batches = new ArrayList<>();
            try (var p =
                    db.prepareStatement(
                            "SELECT id,remaining-frozen FROM course_hour_batch WHERE user_id=? AND"
                                + " remaining>frozen ORDER BY (expire_at IS NULL),expire_at,id")) {
                p.setLong(1, b[1]);
                try (var r = p.executeQuery()) {
                    while (r.next()) batches.add(new long[] {r.getLong(1), r.getInt(2)});
                }
            }
            for (long[] batch : batches) {
                int qty = Math.min(left, (int) batch[1]);
                if (qty <= 0) continue;
                try (var p =
                        db.prepareStatement(
                                "INSERT INTO"
                                    + " booking_hour_allocation(booking_id,batch_id,hours,state)"
                                    + " VALUES(?,?,?,'locked')")) {
                    p.setLong(1, b[0]);
                    p.setLong(2, batch[0]);
                    p.setInt(3, qty);
                    p.executeUpdate();
                }
                try (var p =
                        db.prepareStatement(
                                "UPDATE course_hour_batch SET frozen=frozen+? WHERE id=?")) {
                    p.setInt(1, qty);
                    p.setLong(2, batch[0]);
                    p.executeUpdate();
                }
                left -= qty;
            }
            if (left != 0) throw new SQLException("无法分配历史预约的冻结课时，booking=" + b[0]);
        }
    }

    private boolean column(String table, String name) throws SQLException {
        try (var r = db.getMetaData().getColumns(db.getCatalog(), null, table, name)) {
            return r.next();
        }
    }

    private boolean index(String table, String name) throws SQLException {
        try (var r = db.getMetaData().getIndexInfo(db.getCatalog(), null, table, false, false)) {
            while (r.next()) if (name.equals(r.getString("INDEX_NAME"))) return true;
            return false;
        }
    }

    private void add(String table, String name, String type) throws SQLException {
        if (!column(table, name))
            exec("ALTER TABLE `" + table + "` ADD COLUMN `" + name + "` " + type);
    }

    private void exec(String sql) throws SQLException {
        try (var statement = db.createStatement()) {
            statement.execute(sql);
        }
    }
}
