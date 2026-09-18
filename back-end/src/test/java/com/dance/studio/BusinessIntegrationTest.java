package com.dance.studio;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dance.studio.common.context.*;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.common.util.JwtUtil;
import com.dance.studio.dto.*;
import com.dance.studio.entity.*;
import com.dance.studio.mapper.*;
import com.dance.studio.service.*;
import com.dance.studio.vo.BookingVo;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(
        properties = {
            "spring.datasource.url=${TEST_DB_URL:jdbc:mysql://127.0.0.1:13316/dance_repair_test?serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false}",
            "spring.datasource.username=${TEST_DB_USER:root}",
            "spring.datasource.password=${TEST_DB_PASSWORD:dance-test-local-only}",
            "app.wechat.mock=true",
            "app.jobs.enabled=false",
            "app.jwt.secret=",
            "app.admin.initial-password=test-initial-admin-password",
            "app.storage.type=local"
        })
class BusinessIntegrationTest {
    @Autowired JdbcTemplate db;
    @Autowired AccountService accounts;
    @Autowired BookingService bookings;
    @Autowired CheckinService checkins;
    @Autowired RedeemService redeems;
    @Autowired AuthService auth;
    @Autowired ConfigService config;
    @Autowired GoodsService goodsService;
    @Autowired UserVideoService videos;
    @Autowired QaService qa;
    @Autowired IdempotencyService idempotency;
    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwt;
    @Autowired AdminMapper admins;
    @Autowired BookingMapper bookingMapper;
    @Autowired CheckinCodeService codes;
    long user, schedule;

    @BeforeEach
    void setup() {
        // Only the dedicated test database is accepted, never the workstation DB.
        assertEquals("dance_repair_test", db.queryForObject("SELECT DATABASE()", String.class));
        for (String table :
                List.of(
                        "operation_request",
                        "checkin_code",
                        "booking_hour_allocation",
                        "checkin",
                        "point_log",
                        "hour_log",
                        "course_hour_batch",
                        "recharge_order",
                        "redeem_order",
                        "like_record",
                        "user_video",
                        "qa_answer",
                        "qa_question",
                        "booking",
                        "course_schedule",
                        "course",
                        "teacher",
                        "goods",
                        "user")) db.update("DELETE FROM `" + table + "`");
        db.update("UPDATE config SET config_value='6' WHERE config_key='cancel_free_hours'");
        db.update("UPDATE config SET config_value='30' WHERE config_key='book_cutoff_min'");
        db.update("UPDATE config SET config_value='0' WHERE config_key='redeem_auto_approve'");
        db.update("INSERT INTO `user`(store_id,openid,name) VALUES(1,'test-user','测试舞者')");
        user = db.queryForObject("SELECT id FROM `user` WHERE openid='test-user'", Long.class);
        db.update("INSERT INTO teacher(store_id,name) VALUES(1,'测试教师')");
        long teacher = db.queryForObject("SELECT MAX(id) FROM teacher", Long.class);
        db.update(
                "INSERT INTO course(store_id,name,type,duration_min,cost_hours)"
                        + " VALUES(1,'测试课程','group',60,1)");
        long course = db.queryForObject("SELECT MAX(id) FROM course", Long.class);
        db.update(
                "INSERT INTO"
                    + " course_schedule(store_id,course_id,teacher_id,room,start_time,end_time,capacity,booked_count,status)"
                    + " VALUES(1,?,?,'test',DATE_ADD(NOW(),INTERVAL 2"
                    + " DAY),DATE_ADD(DATE_ADD(NOW(),INTERVAL 2 DAY),INTERVAL 1 HOUR),2,0,'open')",
                course,
                teacher);
        schedule = db.queryForObject("SELECT MAX(id) FROM course_schedule", Long.class);
        ContextHolder.set(adminSession());
    }

    @AfterEach
    void clear() {
        ContextHolder.clear();
    }

    private LoginUser adminSession() {
        var a = admins.findByUsername("admin");
        var u = new LoginUser(a.getId(), 1L, LoginUser.TYPE_ADMIN);
        u.setTokenVersion(a.getTokenVersion());
        return u;
    }

    private String adminToken() {
        return "Bearer " + jwt.generate(adminSession());
    }

    private void recharge(int hours) {
        RechargeReq r = new RechargeReq();
        r.setHours(hours);
        r.setAmount(BigDecimal.ZERO);
        r.setValidityDays(365);
        accounts.recharge(user, r, 1L);
    }

    private int value(String col) {
        return db.queryForObject("SELECT " + col + " FROM `user` WHERE id=?", Integer.class, user);
    }

    private void balanced() {
        accounts.assertBalanced(user);
    }

    @Test
    void bookingFreeCancelAndRepeatedCancelKeepBalances() {
        recharge(10);
        BookingVo b = bookings.book(user, schedule, false);
        assertEquals(9, value("course_balance"));
        assertEquals(9, db.queryForObject("SELECT SUM(remaining) FROM course_hour_batch WHERE user_id=?", Integer.class, user));
        bookings.cancel(b.getId(), user, false);
        bookings.cancel(b.getId(), user, false);
        assertEquals(10, value("course_balance"));
        balanced();
    }

    @Test
    void repeatedAndConcurrentBookingUsesOneReservation() throws Exception {
        recharge(10);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> bookings.book(user, schedule, false));
            var b = pool.submit(() -> bookings.book(user, schedule, false));
            assertEquals(a.get(15, TimeUnit.SECONDS).getId(), b.get(15, TimeUnit.SECONDS).getId());
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM booking", Integer.class));
        assertEquals(9, value("course_balance"));
        assertEquals(9, db.queryForObject("SELECT SUM(remaining) FROM course_hour_batch WHERE user_id=?", Integer.class, user));
        balanced();
    }

    @Test
    void repeatedScanOnlyConsumesOnceAndUsesPointsSnapshot() {
        recharge(3);
        var b = bookings.book(user, schedule, false);
        int points = bookingMapper.findById(b.getId()).getCheckinPoints();
        db.update("UPDATE config SET config_value='999' WHERE config_key='points_per_checkin'");
        db.update(
                "UPDATE course_schedule SET start_time=NOW(),end_time=DATE_ADD(NOW(),INTERVAL 1"
                        + " HOUR) WHERE id=?",
                schedule);
        checkins.scanCheckin(schedule, user);
        checkins.scanCheckin(schedule, user);
        assertEquals(points, value("points_balance"));
        assertEquals(2, value("course_balance"));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM checkin", Integer.class));
        balanced();
    }

    @Test
    void expiryProtectsReservedHoursButCancelDoesNotReviveExpiredCredit() {
        recharge(3);
        var b = bookings.book(user, schedule, false);
        db.update(
                "UPDATE course_hour_batch SET expire_at=DATE_SUB(NOW(),INTERVAL 1 DAY) WHERE"
                        + " user_id=?",
                user);
        accounts.expireBatches();
        accounts.expireBatches();
        assertEquals(0, value("course_balance"));
        bookings.cancel(b.getId(), user, false);
        assertEquals(0, value("course_balance"));
        balanced();
    }

    @Test
    void expiredReservationConsumesItsOwnBatchNotNewRecharge() {
        recharge(1);
        bookings.book(user, schedule, false);
        db.update("UPDATE course_hour_batch SET expire_at=DATE_SUB(NOW(),INTERVAL 1 DAY)");
        accounts.expireBatches();
        recharge(5);
        db.update(
                "UPDATE course_schedule SET start_time=NOW(),end_time=DATE_ADD(NOW(),INTERVAL 1"
                        + " HOUR) WHERE id=?",
                schedule);
        checkins.scanCheckin(schedule, user);
        assertEquals(5, value("course_balance"));
        assertEquals(
                5,
                db.queryForObject(
                        "SELECT remaining FROM course_hour_batch WHERE user_id=? ORDER BY id DESC"
                                + " LIMIT 1",
                        Integer.class,
                        user));
        balanced();
    }

    @Test
    void waitlistDoesNotChargeAndExpiresWithoutDeduction() {
        recharge(3);
        db.update("UPDATE course_schedule SET capacity=1,booked_count=1 WHERE id=?", schedule);
        var wait = bookings.book(user, schedule, false);
        assertEquals("waitlist", wait.getStatus());
        assertEquals(3, value("course_balance"));
        db.update(
                "UPDATE course_schedule SET start_time=DATE_SUB(NOW(),INTERVAL 1 MINUTE) WHERE"
                        + " id=?",
                schedule);
        bookings.finishExpired();
        assertEquals("cancelled", bookingMapper.findById(wait.getId()).getStatus());
        assertEquals(3, value("course_balance"));
        balanced();
    }

    @Test
    void userCannotCancelInsideRestrictedWindow() {
        recharge(2);
        var b = bookings.book(user, schedule, false);
        db.update(
                "UPDATE course_schedule SET start_time=DATE_ADD(NOW(),INTERVAL 1 HOUR) WHERE id=?",
                schedule);
        assertThrows(BizException.class, () -> bookings.cancel(b.getId(), user, false));
        assertEquals("booked", bookingMapper.findById(b.getId()).getStatus());
        balanced();
    }

    @Test
    void concurrentRedeemRejectRefundsOnce() throws Exception {
        accounts.adjustPoints(user, 200, "测试", 1L);
        db.update(
                "INSERT INTO goods(store_id,name,category,points_required,stock,status)"
                        + " VALUES(1,'测试商品','redeem',100,1,1)");
        long goods = db.queryForObject("SELECT MAX(id) FROM goods", Long.class);
        var order = redeems.redeem(user, goods);
        var pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> fs = new ArrayList<>();
            for (int i = 0; i < 2; i++)
                fs.add(
                        pool.submit(
                                () -> {
                                    try {
                                        redeems.reject(order.getId(), 1L, "测试驳回");
                                    } catch (BizException ignored) {
                                    }
                                }));
            for (var f : fs) f.get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(200, value("points_balance"));
        assertEquals(
                1, db.queryForObject("SELECT stock FROM goods WHERE id=?", Integer.class, goods));
    }

    @Test
    void likesAreIdempotentAndNeverAlterSpendablePoints() {
        db.update(
                "INSERT INTO user_video(store_id,user_id,title,file_url,status,like_count)"
                        + " VALUES(1,?,'测试','video/test.mp4','approved',0)",
                user);
        long id = db.queryForObject("SELECT MAX(id) FROM user_video", Long.class);
        videos.toggleLike(id, user, true);
        videos.toggleLike(id, user, true);
        assertEquals(
                1,
                db.queryForObject(
                        "SELECT like_count FROM user_video WHERE id=?", Integer.class, id));
        videos.toggleLike(id, user, false);
        videos.toggleLike(id, user, false);
        assertEquals(0, value("points_balance"));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM like_record", Integer.class));
    }

    @Test
    void qaMappersExecuteBothViewerStates() {
        qa.ask(user, 1L, "测试问题", List.of());
        assertEquals(1, qa.questions(1L, user, 1, 10).getList().size());
        assertEquals(1, qa.questions(1L, null, 1, 10).getList().size());
    }

    @Test
    void duplicateRechargeKeyDoesNotCreditAgain() {
        RechargeReq req = new RechargeReq();
        req.setHours(2);
        req.setAmount(BigDecimal.ZERO);
        for (int i = 0; i < 2; i++)
            idempotency.execute(
                    "test-recharge:" + user,
                    "repeat-request-0001",
                    req,
                    String.class,
                    () -> {
                        accounts.recharge(user, req, 1L);
                        return "done";
                    });
        assertEquals(2, value("course_balance"));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM recharge_order", Integer.class));
        balanced();
    }

    @Test
    void adminCannotCreateOrDisableUsersAndLogoutRevokesToken() throws Exception {
        String token = adminToken();
        mvc.perform(
                        post("/api/admin/users")
                                .header("Authorization", token)
                                .contentType("application/json")
                                .content("{\"name\":\"bad\"}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(
                        put("/api/admin/users/" + user)
                                .header("Authorization", token)
                                .contentType("application/json")
                                .content("{\"name\":\"updated\",\"status\":0}"))
                .andExpect(jsonPath("$.code").value(0));
        assertEquals(1, value("status"));
        mvc.perform(post("/api/admin/auth/logout").header("Authorization", token))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/admin/users").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidAndExpiredQrCodesCannotSignIn() {
        assertThrows(BizException.class, () -> codes.checkin("invalid", user));
        assertThrows(
                BizException.class, () -> codes.checkin("00000000000000000000000000000000", user));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM checkin", Integer.class));
    }

    @Test
    void pickupRequiresCorrectCodeAndCannotBeRepeated() {
        accounts.adjustPoints(user, 100, "测试", 1L);
        db.update(
                "INSERT INTO goods(store_id,name,category,points_required,stock,status)"
                        + " VALUES(1,'领取测试','redeem',100,1,1)");
        long goods = db.queryForObject("SELECT MAX(id) FROM goods", Long.class);
        var order = redeems.redeem(user, goods);
        redeems.approve(order.getId(), 1L);
        assertThrows(BizException.class, () -> redeems.complete(order.getId(), "wrong"));
        redeems.complete(order.getId(), order.getPickupCode());
        redeems.complete(order.getId(), order.getPickupCode());
        assertEquals(
                "completed",
                db.queryForObject(
                        "SELECT status FROM redeem_order WHERE id=?", String.class, order.getId()));
        assertNotNull(
                db.queryForObject(
                        "SELECT picked_up_at FROM redeem_order WHERE id=?",
                        LocalDateTime.class,
                        order.getId()));
        assertEquals(0, value("points_balance"));
        assertEquals(
                0, db.queryForObject("SELECT stock FROM goods WHERE id=?", Integer.class, goods));
    }

    @Test
    void qrTokenChecksMembershipAndIsIdempotent() throws Exception {
        recharge(2);
        bookings.book(user, schedule, false);
        db.update(
                "UPDATE course_schedule SET start_time=NOW(),end_time=DATE_ADD(NOW(),INTERVAL 1"
                        + " HOUR) WHERE id=?",
                schedule);
        String scene = "1234567890abcdef1234567890abcdef";
        String hash =
                HexFormat.of()
                        .formatHex(
                                java.security.MessageDigest.getInstance("SHA-256")
                                        .digest(
                                                scene.getBytes(
                                                        java.nio.charset.StandardCharsets.UTF_8)));
        db.update(
                "INSERT INTO checkin_code(token_hash,schedule_id,expires_at)"
                        + " VALUES(?,?,DATE_ADD(NOW(),INTERVAL 5 MINUTE))",
                hash,
                schedule);
        assertThrows(BizException.class, () -> codes.checkin(scene, user + 1000));
        codes.checkin(scene, user);
        codes.checkin(scene, user);
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM checkin", Integer.class));
        balanced();
    }

    @Test
    void invalidBatchStateRollsBackTheWholeCheckin() {
        recharge(1);
        bookings.book(user, schedule, false);
        db.update(
                "UPDATE course_schedule SET start_time=NOW(),end_time=DATE_ADD(NOW(),INTERVAL 1"
                        + " HOUR) WHERE id=?",
                schedule);
        db.update("UPDATE booking_hour_allocation SET hours=2");
        assertThrows(BizException.class, () -> checkins.scanCheckin(schedule, user));
        assertEquals("booked", db.queryForObject("SELECT status FROM booking", String.class));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM checkin", Integer.class));
    }

    @Test
    void configValidationIsAtomicAndLockedPolicyCannotChange() {
        assertThrows(
                BizException.class,
                () ->
                        config.update(
                                1L, Map.of("cancel_free_hours", "-1", "book_ahead_days", "30")));
        assertEquals(6, config.getInt(1L, "cancel_free_hours", 6));
        assertThrows(BizException.class, () -> config.update(1L, Map.of("lock_on_booking", "0")));
    }

    @Test
    void sequentialWechatLoginReusesSameRegisteredAccount() {
        MpLoginReq req = new MpLoginReq();
        req.setCode("registration-test");
        var a = auth.mpLogin(req);
        var b = auth.mpLogin(req);
        assertEquals(a.getUserId(), b.getUserId());
        assertEquals(
                1,
                db.queryForObject(
                        "SELECT COUNT(*) FROM `user` WHERE openid='mock_registration-test'",
                        Integer.class));
    }

    @Test
    void cancellingASeatPromotesWaitlistAndChargesOnlyThen() {
        recharge(3);
        db.update("UPDATE course_schedule SET capacity=1 WHERE id=?", schedule);
        var first = bookings.book(user, schedule, false);
        db.update("INSERT INTO `user`(store_id,openid,name) VALUES(1,'waiter','候补学员')");
        long waiter = db.queryForObject("SELECT id FROM `user` WHERE openid='waiter'", Long.class);
        RechargeReq r = new RechargeReq();
        r.setHours(3);
        r.setAmount(BigDecimal.ZERO);
        accounts.recharge(waiter, r, 1L);
        var waiting = bookings.book(waiter, schedule, false);
        assertEquals(3, db.queryForObject("SELECT course_balance FROM `user` WHERE id=?", Integer.class, waiter));
        bookings.cancel(first.getId(), user, false);
        assertEquals("booked", bookingMapper.findById(waiting.getId()).getStatus());
        assertEquals(
                2,
                db.queryForObject(
                        "SELECT course_balance FROM `user` WHERE id=?", Integer.class, waiter));
        accounts.assertBalanced(waiter);
        balanced();
    }

    @Test
    void insufficientHoursRollBackBookingAndSeat() {
        assertThrows(BizException.class, () -> bookings.book(user, schedule, false));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM booking", Integer.class));
        assertEquals(0, db.queryForObject("SELECT booked_count FROM course_schedule WHERE id=?", Integer.class, schedule));
        balanced();
    }

    @Test
    void noShowDoesNotDeductAgain() {
        recharge(3);
        var b = bookings.book(user, schedule, false);
        db.update("UPDATE course_schedule SET end_time=DATE_SUB(NOW(),INTERVAL 1 MINUTE) WHERE id=?", schedule);
        bookings.handleNoShow(b.getId());
        bookings.handleNoShow(b.getId());
        assertEquals("noshow", bookingMapper.findById(b.getId()).getStatus());
        assertEquals(2, value("course_balance"));
        assertEquals(2, db.queryForObject("SELECT SUM(remaining) FROM course_hour_batch WHERE user_id=?", Integer.class, user));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM hour_log WHERE user_id=? AND amount<0", Integer.class, user));
        balanced();
    }

    @Test
    void cancellationRefundsOnlyValidOriginalBatchesAndUsesOriginalCost() {
        recharge(1);
        long firstBatch = db.queryForObject("SELECT MAX(id) FROM course_hour_batch", Long.class);
        recharge(3);
        db.update("UPDATE course SET cost_hours=3");
        var b = bookings.book(user, schedule, false);
        assertEquals(3, b.getCostHours());
        assertEquals(1, value("course_balance"));
        db.update("UPDATE course_hour_batch SET expire_at=DATE_SUB(NOW(),INTERVAL 1 DAY) WHERE id=?", firstBatch);
        db.update("UPDATE course SET cost_hours=8");
        bookings.cancel(b.getId(), user, false);
        bookings.cancel(b.getId(), user, false);
        assertEquals(3, value("course_balance"));
        assertEquals(2, db.queryForObject("SELECT amount FROM hour_log WHERE ref_id=? AND type='refund'", Integer.class, b.getId()));
        balanced();
    }

    @Test
    void staleGoodsEditCannotRestoreRedeemedInventory() {
        accounts.adjustPoints(user, 100, "测试", 1L);
        db.update(
                "INSERT INTO goods(store_id,name,category,points_required,stock,status)"
                    + " VALUES(1,'库存测试','redeem',100,1,1)");
        long id = db.queryForObject("SELECT MAX(id) FROM goods", Long.class);
        redeems.redeem(user, id);
        GoodsSaveReq req = new GoodsSaveReq();
        req.setName("库存测试");
        req.setPointsRequired(100);
        req.setStock(1);
        req.setVersion(0);
        assertThrows(BizException.class, () -> goodsService.save(id, req));
        assertEquals(0, db.queryForObject("SELECT stock FROM goods WHERE id=?", Integer.class, id));
    }
}
