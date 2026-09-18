package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;

/** 将旧预约转换为已扣课记录，保留原批次退款依据；数据转换可安全重试。 */
public class V5__Remove_frozen_hours extends BaseJavaMigration {
    private Connection db;

    @Override
    public void migrate(Context context) throws Exception {
        db = context.getConnection();
        if (column("course_hour_batch", "frozen")) convert();
        drop("course_hour_batch", "frozen");
        drop("user", "frozen_hours");
        drop("booking", "locked_hours");
        exec("ALTER TABLE `user` MODIFY course_balance INT NOT NULL DEFAULT 0 COMMENT '剩余课时'");
    }

    private void convert() throws SQLException {
        boolean auto = db.getAutoCommit();
        db.setAutoCommit(false);
        try {
            reject("SELECT u.id FROM `user` u WHERE u.course_balance<0 OR u.frozen_hours<0"
                    + " OR u.course_balance+u.frozen_hours<>COALESCE((SELECT SUM(remaining)"
                    + " FROM course_hour_batch WHERE user_id=u.id),0)"
                    + " OR u.frozen_hours<>COALESCE((SELECT SUM(frozen) FROM course_hour_batch"
                    + " WHERE user_id=u.id),0)", "用户课时不一致");
            reject("SELECT b.id FROM course_hour_batch b WHERE b.frozen<0 OR b.remaining<b.frozen"
                    + " OR b.frozen<>COALESCE((SELECT SUM(hours) FROM booking_hour_allocation"
                    + " WHERE batch_id=b.id AND state='locked'),0)", "批次课时不一致");
            reject("SELECT a.booking_id FROM booking_hour_allocation a"
                    + " LEFT JOIN booking b ON b.id=a.booking_id"
                    + " LEFT JOIN course_hour_batch h ON h.id=a.batch_id"
                    + " WHERE a.state='locked' AND (b.id IS NULL OR h.id IS NULL"
                    + " OR b.user_id<>h.user_id OR b.status NOT IN ('booked','waitlist'))",
                    "预约扣课归属不一致");
            reject("SELECT b.id FROM booking b WHERE b.status='booked' AND b.cost_hours<>"
                    + "COALESCE((SELECT SUM(hours) FROM booking_hour_allocation WHERE booking_id=b.id"
                    + " AND state IN ('locked','charged')),0)", "预约课时不一致");

            // 已从可用余额扣除的预约，只从批次剩余量中扣一次。
            exec("UPDATE course_hour_batch SET remaining=remaining-frozen,frozen=0");
            // 旧系统可能存在已占用课时的候补；恢复仍有效的部分，候补此后不扣课。
            exec("UPDATE course_hour_batch h JOIN (SELECT a.batch_id,SUM(a.hours) qty"
                    + " FROM booking_hour_allocation a JOIN booking b ON b.id=a.booking_id"
                    + " WHERE a.state='locked' AND b.status='waitlist' GROUP BY a.batch_id) r"
                    + " ON r.batch_id=h.id SET h.remaining=h.remaining+r.qty"
                    + " WHERE h.expire_at IS NULL OR h.expire_at>NOW()");
            exec("UPDATE `user` u JOIN (SELECT b.user_id,SUM(a.hours) qty"
                    + " FROM booking_hour_allocation a JOIN booking b ON b.id=a.booking_id"
                    + " JOIN course_hour_batch h ON h.id=a.batch_id WHERE a.state='locked'"
                    + " AND b.status='waitlist' AND (h.expire_at IS NULL OR h.expire_at>NOW())"
                    + " GROUP BY b.user_id) r ON r.user_id=u.id"
                    + " SET u.course_balance=u.course_balance+r.qty");
            exec("INSERT INTO hour_log(user_id,type,amount,balance_after,ref_type,ref_id,remark)"
                    + " SELECT b.user_id,'refund',SUM(a.hours),u.course_balance,NULL,NULL,"
                    + "'升级退回候补有效课时' FROM booking_hour_allocation a"
                    + " JOIN booking b ON b.id=a.booking_id JOIN `user` u ON u.id=b.user_id"
                    + " JOIN course_hour_batch h ON h.id=a.batch_id WHERE a.state='locked'"
                    + " AND b.status='waitlist' AND (h.expire_at IS NULL OR h.expire_at>NOW())"
                    + " GROUP BY b.user_id,u.course_balance");
            exec("UPDATE booking_hour_allocation a JOIN booking b ON b.id=a.booking_id"
                    + " JOIN course_hour_batch h ON h.id=a.batch_id SET a.state=CASE"
                    + " WHEN b.status='booked' THEN 'charged'"
                    + " WHEN h.expire_at IS NULL OR h.expire_at>NOW() THEN 'released'"
                    + " ELSE 'expired' END WHERE a.state='locked'");
            exec("UPDATE `user` SET frozen_hours=0");
            reject("SELECT u.id FROM `user` u WHERE u.course_balance<>COALESCE((SELECT SUM(remaining)"
                    + " FROM course_hour_batch WHERE user_id=u.id),0)", "转换后课时不一致");
            db.commit();
        } catch (SQLException e) {
            db.rollback();
            throw e;
        } finally {
            db.setAutoCommit(auto);
        }
    }

    private void reject(String sql, String message) throws SQLException {
        try (var s = db.createStatement(); var r = s.executeQuery(sql + " LIMIT 1")) {
            if (r.next()) throw new SQLException(message + ", id=" + r.getLong(1));
        }
    }

    private boolean column(String table, String name) throws SQLException {
        try (var r = db.getMetaData().getColumns(db.getCatalog(), null, table, name)) {
            return r.next();
        }
    }

    private void drop(String table, String name) throws SQLException {
        if (column(table, name)) exec("ALTER TABLE `" + table + "` DROP COLUMN `" + name + "`");
    }

    private void exec(String sql) throws SQLException {
        try (var s = db.createStatement()) { s.execute(sql); }
    }
}
