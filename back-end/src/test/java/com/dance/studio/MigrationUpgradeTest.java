package com.dance.studio;

import static org.junit.jupiter.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.UUID;

class MigrationUpgradeTest {
    @Test
    void upgradesExistingReservedHoursWithoutChangingBalances() throws Exception {
        migrate(false);
    }

    @Test
    void refusesInconsistentLegacyBalancesBeforeChangingSchema() throws Exception {
        migrate(true);
    }

    private void migrate(boolean corrupt) throws Exception {
        migrate(corrupt, false, false, false);
    }

    @Test
    void restoresLegacyWaitlistCredit() throws Exception {
        migrate(false, true, false, false);
    }

    @Test
    void doesNotRestoreExpiredLegacyWaitlistCredit() throws Exception {
        migrate(false, true, true, false);
    }

    @Test
    void rejectsInconsistentV4BatchBeforeDroppingColumns() throws Exception {
        migrate(false, false, false, true);
    }

    private void migrate(boolean corrupt, boolean waitlist, boolean expired, boolean corruptV4) throws Exception {
        String original =
                System.getenv()
                        .getOrDefault(
                                "TEST_DB_URL",
                                "jdbc:mysql://127.0.0.1:13316/dance_repair_test?serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
        if (!original.contains("/dance_repair_test"))
            throw new IllegalStateException("Only dedicated test databases are allowed");
        String user = System.getenv().getOrDefault("TEST_DB_USER", "root"),
                password =
                        System.getenv().getOrDefault("TEST_DB_PASSWORD", "dance-test-local-only");
        String schema = "dance_migration_test_" + UUID.randomUUID().toString().replace("-", "");
        if (!schema.matches("dance_migration_test_[0-9a-f]{32}")) throw new IllegalStateException();
        try (var admin = DriverManager.getConnection(original, user, password);
                var sql = admin.createStatement()) {
            sql.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4");
            String url = original.replace("/dance_repair_test", "/" + schema);
            try {
                Flyway.configure().dataSource(url, user, password).target("1").load().migrate();
                try (var c = DriverManager.getConnection(url, user, password);
                        var s = c.createStatement()) {
                    s.execute(
                            "INSERT INTO `user`(id,store_id,openid,course_balance,frozen_hours)"
                                    + " VALUES(1,1,'legacy',"
                                    + (corrupt ? 99 : 2)
                                    + ",1)");
                    s.execute(
                            "INSERT INTO course_hour_batch(id,user_id,total,remaining,source)"
                                    + " VALUES(1,1,3,3,'recharge')");
                    s.execute(
                            "INSERT INTO course(store_id,name,type,duration_min,cost_hours)"
                                    + " VALUES(1,'legacy','group',60,1)");
                    s.execute(
                            "INSERT INTO"
                                + " course_schedule(id,store_id,course_id,start_time,end_time,capacity,booked_count,status)"
                                + " VALUES(1,1,1,DATE_ADD(NOW(),INTERVAL 2"
                                + " DAY),DATE_ADD(NOW(),INTERVAL 3 DAY),12,1,'open')");
                    s.execute(
                            "INSERT INTO"
                                + " booking(id,store_id,user_id,schedule_id,status,locked_hours)"
                                + " VALUES(1,1,1,1,'booked',1)");
                    if (waitlist) s.execute("UPDATE booking SET status='waitlist'");
                    if (expired) s.execute("UPDATE course_hour_batch SET expire_at=DATE_SUB(NOW(),INTERVAL 1 DAY)");
                }
                if (corruptV4) {
                    Flyway.configure().dataSource(url, user, password).target("4").load().migrate();
                    try (var c = DriverManager.getConnection(url, user, password); var s = c.createStatement()) {
                        s.execute("UPDATE course_hour_batch SET frozen=2");
                    }
                }
                var flyway = Flyway.configure().dataSource(url, user, password).load();
                if (corrupt || corruptV4) {
                    assertThrows(Exception.class, flyway::migrate);
                    try (var c = DriverManager.getConnection(url, user, password);
                            var s = c.createStatement();
                            var r = s.executeQuery("SELECT frozen_hours FROM `user` WHERE id=1")) {
                        assertTrue(r.next());
                        assertEquals(1, r.getInt(1));
                    }
                }
                else {
                    flyway.migrate();
                    flyway.migrate();
                    try (var c = DriverManager.getConnection(url, user, password);
                            var s = c.createStatement();
                            var r =
                                    s.executeQuery(
                                            "SELECT"
                                                + " u.course_balance,b.remaining,a.hours,a.state"
                                                + " FROM `user` u JOIN course_hour_batch b ON"
                                                + " b.user_id=u.id JOIN booking_hour_allocation a"
                                                + " ON a.batch_id=b.id")) {
                        assertTrue(r.next());
                        assertEquals(waitlist && !expired ? 3 : 2, r.getInt(1));
                        assertEquals(waitlist && !expired ? 3 : 2, r.getInt(2));
                        assertEquals(1, r.getInt(3));
                        assertEquals(waitlist ? expired ? "expired" : "released" : "charged", r.getString(4));
                    }
                    try (var c = DriverManager.getConnection(url, user, password);
                            var s = c.createStatement();
                            var r = s.executeQuery("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()"
                                    + " AND ((table_name='user' AND column_name='frozen_hours')"
                                    + " OR (table_name='course_hour_batch' AND column_name='frozen')"
                                    + " OR (table_name='booking' AND column_name='locked_hours'))")) {
                        assertTrue(r.next());
                        assertEquals(0, r.getInt(1));
                    }
                }
            } finally {
                sql.execute("DROP DATABASE `" + schema + "`");
            }
        }
    }
}
