-- ============================================================
-- 模拟近几天测试数据（基准日：2026-10-08，覆盖 10-01 ~ 10-14）
--
-- 内容：
--   0. 清理 demo 学员(user 1-10)旧口径账目（原冻结课时机制残留，余额与批次不匹配）
--   1. 9-28 统一充值基线：每人 30 课时 / ¥2800 / +2800 积分（有效期 365 天）
--   2. 排课 27 节：过去(10-01~10-08上午,finished) + 未来(open/full)
--   3. 约课完整链路：签到(checked)/爽约(noshow)/免费取消(refund)/私教(cost 2)/候补转正
--      与现行代码口径一致：约课即扣课(booking_hour_allocation charged → consumed/released)
--   4. 收口：batch.remaining / user 余额积分 / schedule.booked_count 与流水完全对平
--
-- 执行（Windows Git Bash）：
--   wsl -d Ubuntu -u root -- mysql -uroot -p88888888 --default-character-set=utf8mb4 \
--     < /mnt/d/wsheng/play_dance/back-end/sql/mock_recent_data.sql
--
-- 注意：只动 user 1-10 的账目与新增业务数据；user 15(真实测试号)与历史约课记录不动。
-- ============================================================
USE `dance_studio`;
SET NAMES utf8mb4;
START TRANSACTION;

-- ------------------------------------------------------------
-- 0. 清理 user 1-10 旧账（旧冻结机制残留：balance=3 但批次剩余 8~14，对不平）
-- ------------------------------------------------------------
DELETE FROM booking_hour_allocation
 WHERE batch_id IN (SELECT id FROM course_hour_batch WHERE user_id BETWEEN 1 AND 10);
DELETE FROM course_hour_batch   WHERE user_id BETWEEN 1 AND 10;
DELETE FROM hour_log            WHERE user_id BETWEEN 1 AND 10;
DELETE FROM point_log           WHERE user_id BETWEEN 1 AND 10;
DELETE FROM recharge_order      WHERE user_id BETWEEN 1 AND 10;

-- ------------------------------------------------------------
-- 1. 充值基线（2026-09-28 上午，每人错开几分钟）
-- ------------------------------------------------------------
INSERT INTO recharge_order
  (store_id, user_id, hours, gift_hours, amount, points_gain, validity_days, operator_id, remark, created_at)
SELECT 1, u.id, 30, 0, 2800.00, 2800, 365, 1, '国庆前课时卡充值',
       DATE_ADD('2026-09-28 09:00:00', INTERVAL u.id MINUTE)
  FROM user u WHERE u.id BETWEEN 1 AND 10;

INSERT INTO course_hour_batch
  (user_id, total, remaining, source, order_id, expire_at, created_at)
SELECT u.id, 30, 30, 'recharge', r.id,
       DATE_ADD(DATE_ADD('2026-09-28 09:00:00', INTERVAL u.id MINUTE), INTERVAL 365 DAY),
       r.created_at
  FROM user u
  JOIN recharge_order r ON r.user_id = u.id
       AND r.created_at = DATE_ADD('2026-09-28 09:00:00', INTERVAL u.id MINUTE)
 WHERE u.id BETWEEN 1 AND 10;

INSERT INTO hour_log
  (user_id, type, amount, balance_after, ref_type, ref_id, operator_id, remark, created_at)
SELECT u.id, 'recharge', 30, 30, 'recharge', r.id, 1, '充值含赠送 0 课时', r.created_at
  FROM user u
  JOIN recharge_order r ON r.user_id = u.id
       AND r.created_at = DATE_ADD('2026-09-28 09:00:00', INTERVAL u.id MINUTE)
 WHERE u.id BETWEEN 1 AND 10;

INSERT INTO point_log
  (user_id, type, amount, balance_after, ref_type, ref_id, operator_id, remark, created_at)
SELECT u.id, 'recharge', 2800, u.points_balance + 2800, 'recharge', r.id, 1, '充值赠送积分', r.created_at
  FROM user u
  JOIN recharge_order r ON r.user_id = u.id
       AND r.created_at = DATE_ADD('2026-09-28 09:00:00', INTERVAL u.id MINUTE)
 WHERE u.id BETWEEN 1 AND 10;

-- ------------------------------------------------------------
-- 2. 排课（10-01 ~ 10-14，时长取 course.duration_min）
--    今晚 10-08 19:30 容量 5（造满员+候补转正闭环）；10-10 19:30 容量 8（候补进行中）
-- ------------------------------------------------------------
SET @sf := (SELECT MAX(id) + 1 FROM course_schedule);

INSERT INTO course_schedule
  (store_id, course_id, teacher_id, room, song, start_time, end_time, capacity, booked_count, status)
SELECT 1, v.course_id, v.teacher_id, v.room, v.song, v.start_time,
       DATE_ADD(v.start_time, INTERVAL c.duration_min MINUTE), v.capacity, 0,
       CASE WHEN v.start_time < '2026-10-08 12:00:00' THEN 'finished'
            WHEN v.start_time IN ('2026-10-08 19:30:00', '2026-10-10 19:30:00') THEN 'full'
            ELSE 'open' END
FROM (
  SELECT 10 AS course_id, 3 AS teacher_id, 'Heart' AS room, 'Godzilla - Eminem'          AS song, '2026-10-01 10:00:00' AS start_time, 12 AS capacity
  UNION ALL SELECT 14, 8, '心',   'How Sweet - NewJeans',        '2026-10-01 19:30:00', 12
  UNION ALL SELECT 16, 7, 'Heart','Supernova - aespa',          '2026-10-02 16:30:00', 12
  UNION ALL SELECT 11, 4, '心',   'Batter Up - BABYMONSTER',    '2026-10-02 19:30:00', 12
  UNION ALL SELECT 15, 5, '心',   'Ditto - NewJeans',           '2026-10-03 10:00:00', 12
  UNION ALL SELECT 12, 6, 'Heart','Pink Venom - BLACKPINK',     '2026-10-03 15:00:00', 12
  UNION ALL SELECT 18, 8, '心',   'Breaking 成品私教',          '2026-10-03 19:30:00', 1
  UNION ALL SELECT 17, 7, 'Heart','APT. - ROSÉ & Bruno Mars',   '2026-10-04 10:00:00', 12
  UNION ALL SELECT 13, 3, '心',   'Bodak Yellow - Cardi B',     '2026-10-04 16:30:00', 12
  UNION ALL SELECT 14, 4, 'Heart','Magnetic - ILLIT',           '2026-10-05 19:30:00', 12
  UNION ALL SELECT 10, 5, '心',   'Chk Chk Boom - Stray Kids',  '2026-10-06 10:00:00', 12
  UNION ALL SELECT 16, 8, 'Heart','Whiplash - aespa',           '2026-10-06 19:30:00', 12
  UNION ALL SELECT 15, 6, '心',   'EASY - LE SSERAFIM',         '2026-10-07 16:30:00', 12
  UNION ALL SELECT 11, 3, 'Heart','Drill - EXO',                '2026-10-07 19:30:00', 12
  UNION ALL SELECT 17, 7, 'Heart','Mantra - JENNIE',            '2026-10-08 10:00:00', 12
  UNION ALL SELECT 14, 8, '心',   'Moonlit Floor - LISA',       '2026-10-08 19:30:00', 5
  UNION ALL SELECT 12, 4, '心',   'Super Shy - NewJeans',       '2026-10-09 16:30:00', 12
  UNION ALL SELECT 16, 7, 'Heart','Armageddon - aespa',         '2026-10-09 19:30:00', 12
  UNION ALL SELECT 15, 5, '心',   'Smart - LE SSERAFIM',        '2026-10-10 10:00:00', 12
  UNION ALL SELECT 10, 3, 'Heart','Paint The Town Red - Doja Cat','2026-10-10 19:30:00', 8
  UNION ALL SELECT 17, 6, 'Heart','Blue - Yung Kai',            '2026-10-11 10:00:00', 12
  UNION ALL SELECT 18, 8, '心',   'Breaking 成品私教',          '2026-10-11 15:00:00', 1
  UNION ALL SELECT 13, 7, '心',   'SICKO MODE - Travis Scott',  '2026-10-12 19:30:00', 12
  UNION ALL SELECT 14, 3, 'Heart','Bubble Gum - NewJeans',      '2026-10-13 16:30:00', 12
  UNION ALL SELECT 16, 8, '心',   'Supernova - aespa',          '2026-10-13 19:30:00', 12
  UNION ALL SELECT 11, 5, '心',   'Houdini - Eminem',           '2026-10-14 10:00:00', 12
  UNION ALL SELECT 17, 7, 'Heart','Cool With You - NewJeans',   '2026-10-14 16:30:00', 12
) v JOIN course c ON c.id = v.course_id;

SET @sl := (SELECT MAX(id) FROM course_schedule);
SET @tonight := (SELECT id FROM course_schedule WHERE start_time = '2026-10-08 19:30:00');
SET @fullc   := (SELECT id FROM course_schedule WHERE start_time = '2026-10-10 19:30:00');
SET @priv1   := (SELECT id FROM course_schedule WHERE start_time = '2026-10-03 19:30:00');
SET @priv2   := (SELECT id FROM course_schedule WHERE start_time = '2026-10-11 15:00:00');

-- ------------------------------------------------------------
-- 3. 约课记录
-- ------------------------------------------------------------
SET @bf := (SELECT MAX(id) + 1 FROM booking);

-- 3.1 过去课次（10-01~10-08 上午，团课）派生：约 40% 出勤率
--     状态：约 14% 爽约、9% 提前 1 天免费取消；掺少量管理员代约
INSERT INTO booking
  (store_id, user_id, schedule_id, status, cost_hours, checkin_points, is_admin, cancel_at, created_at)
SELECT 1, u.id, s.id,
       CASE WHEN MOD(u.id*5 + s.id, 11) = 0 THEN 'cancelled'
            WHEN MOD(u.id*3 + s.id, 7)  = 0 THEN 'noshow'
            ELSE 'checked' END,
       c.cost_hours, 100,
       IF(MOD(u.id + s.id, 9) = 0, 1, 0),
       CASE WHEN MOD(u.id*5 + s.id, 11) = 0
            THEN DATE_ADD(s.start_time, INTERVAL -1 DAY) END,
       DATE_ADD(
         DATE_ADD(s.start_time, INTERVAL -(
           CASE WHEN MOD(u.id*5 + s.id, 11) = 0 THEN 3
                ELSE 1 + MOD(u.id + s.id, 3) END) DAY),
         INTERVAL MOD(u.id*37 + s.id, 600) MINUTE)
  FROM user u
  JOIN course_schedule s ON s.id BETWEEN @sf AND @sl
       AND s.start_time < '2026-10-08 12:00:00' AND s.course_id <> 18
  JOIN course c ON c.id = s.course_id
 WHERE u.id BETWEEN 1 AND 10
   AND MOD(u.id*7 + s.id, 10) < 4;

-- 3.2 过去私教（10-03，糖糖，扣 2 课时，已签到）
INSERT INTO booking
  (store_id, user_id, schedule_id, status, cost_hours, checkin_points, is_admin, created_at)
VALUES (1, 4, @priv1, 'checked', 2, 100, 1, '2026-10-01 20:15:00');

-- 3.3 今晚 10-08 19:30 JAZZ（容量 5）满员闭环：
--     1/2/4/6/7 五人约满 → 6 于 10-07 18:00 免费取消（提前>6h）→ 10 候补第 1 位转正
INSERT INTO booking
  (store_id, user_id, schedule_id, status, cost_hours, checkin_points, is_admin, cancel_at, created_at)
VALUES (1, 1,  @tonight, 'booked',    1, 100, 0, NULL,                    '2026-10-06 20:00:00'),
       (1, 2,  @tonight, 'booked',    1, 100, 0, NULL,                    '2026-10-06 20:12:00'),
       (1, 4,  @tonight, 'booked',    1, 100, 0, NULL,                    '2026-10-06 20:25:00'),
       (1, 6,  @tonight, 'cancelled', 1, 100, 0, '2026-10-07 18:00:00',  '2026-10-06 20:40:00'),
       (1, 7,  @tonight, 'booked',    1, 100, 0, NULL,                    '2026-10-06 21:05:00'),
       (1, 10, @tonight, 'booked',    1, 100, 0, NULL,                    '2026-10-07 18:00:00');

-- 3.4 10-10 19:30 HIP-HOP（容量 8）满员：1-8 已约，9/10 候补中
INSERT INTO booking
  (store_id, user_id, schedule_id, status, cost_hours, checkin_points, is_admin, waitlist_no, created_at)
SELECT 1, u.id, @fullc, 'booked', 1, 100, 0, NULL,
       DATE_ADD('2026-10-07 20:00:00', INTERVAL u.id * 7 MINUTE)
  FROM user u WHERE u.id BETWEEN 1 AND 8;
INSERT INTO booking
  (store_id, user_id, schedule_id, status, cost_hours, checkin_points, is_admin, waitlist_no, created_at)
VALUES (1, 9,  @fullc, 'waitlist', 1, 100, 0, 1, '2026-10-08 10:20:00'),
       (1, 10, @fullc, 'waitlist', 1, 100, 0, 2, '2026-10-08 11:40:00');

-- 3.5 未来课次（10-09 起，团课）派生：约 30% 已约
INSERT INTO booking
  (store_id, user_id, schedule_id, status, cost_hours, checkin_points, is_admin, created_at)
SELECT 1, u.id, s.id, 'booked', c.cost_hours, 100, 0,
       DATE_ADD('2026-10-07 21:00:00', INTERVAL MOD(u.id*29 + s.id, 720) MINUTE)
  FROM user u
  JOIN course_schedule s ON s.id BETWEEN @sf AND @sl
       AND s.start_time >= '2026-10-09 00:00:00' AND s.course_id <> 18
       AND s.id <> @fullc
  JOIN course c ON c.id = s.course_id
 WHERE u.id BETWEEN 1 AND 10
   AND MOD(u.id*11 + s.id, 10) < 3;

-- 3.6 未来私教（10-11，奶茶，扣 2 课时）
INSERT INTO booking
  (store_id, user_id, schedule_id, status, cost_hours, checkin_points, is_admin, created_at)
VALUES (1, 9, @priv2, 'booked', 2, 100, 1, '2026-10-08 09:30:00');

SET @bl := (SELECT MAX(id) FROM booking);

-- ------------------------------------------------------------
-- 4. 签到记录（checked 的约课，开课前 5~24 分钟）
-- ------------------------------------------------------------
INSERT INTO checkin (store_id, booking_id, user_id, schedule_id, check_time, method, operator_id)
SELECT 1, b.id, b.user_id, b.schedule_id,
       DATE_ADD(s.start_time, INTERVAL -(5 + MOD(b.id, 20)) MINUTE),
       IF(MOD(b.user_id, 2) = 0, 'scan', 'manual'),
       IF(MOD(b.user_id, 2) = 0, NULL, 1)
  FROM booking b
  JOIN course_schedule s ON s.id = b.schedule_id
 WHERE b.id BETWEEN @bf AND @bl AND b.status = 'checked';

-- ------------------------------------------------------------
-- 5. 课时扣减明细（约课即扣；charged→consumed(签到/爽约)/released(已取消退回)）
-- ------------------------------------------------------------
INSERT INTO booking_hour_allocation (booking_id, batch_id, hours, state)
SELECT b.id, chb.id, b.cost_hours,
       CASE b.status WHEN 'cancelled' THEN 'released'
                     WHEN 'booked'    THEN 'charged'
                     ELSE 'consumed' END
  FROM booking b
  JOIN course_hour_batch chb ON chb.user_id = b.user_id
 WHERE b.id BETWEEN @bf AND @bl
   AND b.status <> 'waitlist'
   AND b.user_id BETWEEN 1 AND 10;

-- ------------------------------------------------------------
-- 6. 课时流水（顺序：预约扣 → 取消退 → 签到/爽约确认）
-- ------------------------------------------------------------
-- 6.1 预约扣课（候补不扣课；user 10 今晚课按转正时刻已写入 created_at）
INSERT INTO hour_log
  (user_id, type, amount, balance_after, ref_type, ref_id, operator_id, remark, created_at)
SELECT b.user_id, 'booking', -b.cost_hours, 0, 'booking', b.id, NULL, '预约扣课时', b.created_at
  FROM booking b
 WHERE b.id BETWEEN @bf AND @bl AND b.status <> 'waitlist';

-- 6.2 取消退回（提前 1 天免费取消，全部退回）
INSERT INTO hour_log
  (user_id, type, amount, balance_after, ref_type, ref_id, operator_id, remark, created_at)
SELECT b.user_id, 'refund', b.cost_hours, 0, 'booking', b.id, NULL,
       CONCAT('取消退回 ', b.cost_hours, ' 课时；已到期部分 0 课时不恢复'), b.cancel_at
  FROM booking b
 WHERE b.id BETWEEN @bf AND @bl AND b.status = 'cancelled';

-- 6.3 签到确认（预约时已扣，仅确认）
INSERT INTO hour_log
  (user_id, type, amount, balance_after, ref_type, ref_id, operator_id, remark, created_at)
SELECT b.user_id, 'deduct', 0, 0, 'booking', b.id, NULL, '签到确认，预约时已扣课时', c.check_time
  FROM booking b JOIN checkin c ON c.booking_id = b.id
 WHERE b.id BETWEEN @bf AND @bl AND b.status = 'checked';

-- 6.4 爽约确认（课次结束 5 分钟后由定时任务判定）
INSERT INTO hour_log
  (user_id, type, amount, balance_after, ref_type, ref_id, operator_id, remark, created_at)
SELECT b.user_id, 'noshow', 0, 0, 'booking', b.id, NULL, '爽约确认，预约时已扣课时',
       DATE_ADD(s.end_time, INTERVAL 5 MINUTE)
  FROM booking b JOIN course_schedule s ON s.id = b.schedule_id
 WHERE b.id BETWEEN @bf AND @bl AND b.status = 'noshow';

-- ------------------------------------------------------------
-- 7. 积分流水（签到 +100）
-- ------------------------------------------------------------
INSERT INTO point_log
  (user_id, type, amount, balance_after, ref_type, ref_id, operator_id, remark, created_at)
SELECT c.user_id, 'checkin', 100, 0, 'checkin', c.booking_id, NULL, '签到积分', c.check_time
  FROM checkin c
 WHERE c.schedule_id BETWEEN @sf AND @sl;

-- ------------------------------------------------------------
-- 8. 站内通知（候补转正成功）
-- ------------------------------------------------------------
INSERT INTO notification (user_id, type, title, content, ref_type, ref_id, is_read, created_at)
SELECT b.user_id, 'booking', '候补转正成功', '已为你预留名额并扣除课时，请按时到店。',
       'booking', b.id, 0, '2026-10-07 18:00:00'
  FROM booking b
 WHERE b.schedule_id = @tonight AND b.user_id = 10 AND b.status = 'booked';

-- ------------------------------------------------------------
-- 9. 收口：批次余额 / 用户余额积分 / 课次已约人数
-- ------------------------------------------------------------
-- 批次剩余 = 总数 - 未退回的扣减（released 已退回批次）
UPDATE course_hour_batch chb
   SET chb.remaining = chb.total - COALESCE((
       SELECT SUM(a.hours) FROM booking_hour_allocation a
        WHERE a.batch_id = chb.id AND a.state <> 'released'), 0);

UPDATE user u
   SET u.course_balance = COALESCE((
       SELECT SUM(chb.remaining) FROM course_hour_batch chb WHERE chb.user_id = u.id), 0),
       u.frozen_hours = 0
 WHERE u.id BETWEEN 1 AND 10;

UPDATE user u
  JOIN (SELECT user_id, SUM(amount) s FROM point_log GROUP BY user_id) p ON p.user_id = u.id
   SET u.points_balance = u.points_balance + p.s
 WHERE u.id BETWEEN 1 AND 10;

UPDATE course_schedule s
   SET s.booked_count = (
       SELECT COUNT(*) FROM booking b
        WHERE b.schedule_id = s.id AND b.status IN ('booked', 'checked', 'noshow'))
 WHERE s.id BETWEEN @sf AND @sl;

-- 流水 balance_after 按时间顺序重放（与真实账务一致）
UPDATE hour_log h
  JOIN (SELECT id,
               SUM(amount) OVER (PARTITION BY user_id ORDER BY created_at, id) AS rb
          FROM hour_log WHERE user_id BETWEEN 1 AND 10) t
    ON t.id = h.id
   SET h.balance_after = t.rb;
-- 积分流水重放需带上旧积分基数（旧积分仍在 user 表，本轮流水从充值起算）
UPDATE point_log p
  JOIN (SELECT id,
               SUM(amount) OVER (PARTITION BY user_id ORDER BY created_at, id) AS rb
          FROM point_log WHERE user_id BETWEEN 1 AND 10) t
    ON t.id = p.id
  JOIN (SELECT pl.user_id, u.points_balance - SUM(pl.amount) AS base
          FROM point_log pl JOIN user u ON u.id = pl.user_id
         WHERE pl.user_id BETWEEN 1 AND 10
         GROUP BY pl.user_id, u.points_balance) bs
    ON bs.user_id = p.user_id
   SET p.balance_after = t.rb + bs.base;

COMMIT;

-- ------------------------------------------------------------
-- 10. 自检输出
-- ------------------------------------------------------------
SELECT u.id, u.name, u.course_balance AS 余额,
       (SELECT SUM(chb.remaining) FROM course_hour_batch chb WHERE chb.user_id = u.id) AS 批次合计,
       u.points_balance AS 积分
  FROM user u WHERE u.id BETWEEN 1 AND 10;

SELECT s.id, c.name AS 课程, t.name AS 教师, s.start_time, s.capacity, s.booked_count, s.status
  FROM course_schedule s
  JOIN course c ON c.id = s.course_id
  JOIN teacher t ON t.id = s.teacher_id
 WHERE s.id BETWEEN @sf AND @sl ORDER BY s.start_time;

SELECT b.status, COUNT(*) FROM booking b WHERE b.id BETWEEN @bf AND @bl GROUP BY b.status;
