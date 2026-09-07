-- =============================================
-- 演示模拟数据（基于已有基础数据补充业务数据）
-- =============================================
USE dance_studio;

-- 1. 完善已有用户 + 新增用户
UPDATE `user` SET name='小米', phone='13800003333', stage='零基础', points_balance=580, course_balance=8 WHERE id=3;
UPDATE `user` SET name='糖糖', phone='13800004444', stage='入门', points_balance=320, course_balance=5 WHERE id=4;
UPDATE `user` SET name='Luna', phone='13800005555', stage='进阶', points_balance=1200, course_balance=15 WHERE id=5;
UPDATE `user` SET name='小鱼', phone='13800006666', stage='零基础', points_balance=150, course_balance=3 WHERE id=6;

INSERT INTO `user` (store_id, openid, name, phone, gender, avatar, stage, prefer_dance, points_balance, course_balance, status) VALUES
(1, 'demo_openid_7', '小七', '13900007777', 2, NULL, '入门', 'K-pop', 420, 6, 1),
(1, 'demo_openid_8', '阿J',  '13900008888', 1, NULL, '进阶', 'HIP-HOP', 890, 10, 1),
(1, 'demo_openid_9', '奶茶', '13900009999', 2, NULL, '零基础', 'Jazz', 80, 2, 1),
(1, 'demo_openid_10','Kiki', '13900010000', 2, NULL, '进阶', 'K-pop', 2100, 20, 1);

-- 2. 官方教学视频 video
INSERT INTO video (store_id, course_id, schedule_id, video_date, title, file_url, cover_url, duration, file_size, scope_type, status) VALUES
(1, 3, 1, '2025-08-10', 'HIP-HOP基础 - 律动练习', '/demo/hiphop_basic.mp4', NULL, 360, 25000000, 'checked_in', 1),
(1, 3, 2, '2025-08-12', 'K-pop成品舞 - Smart 教学示范', '/demo/kpop_smart.mp4', NULL, 480, 32000000, 'booked', 1),
(1, 4, 5, '2025-08-15', 'Jazz Funk - 身体波浪分解', '/demo/jazz_wave.mp4', NULL, 300, 20000000, 'checked_in', 1),
(1, 5, 6, '2025-08-17', 'Urban编舞 - 情绪表达', '/demo/urban_emo.mp4', NULL, 420, 28000000, 'booked', 1);

-- 3. 用户投稿视频 user_video（补充pending/rejected/多作者）
INSERT INTO user_video (store_id, user_id, title, file_url, cover_url, description, duration, file_size, status, reject_reason, is_recommended, like_count, created_at) VALUES
(1, 2, 'Super Shy - NewJeans 翻跳', '/demo/u_supershy.mp4', NULL, '第一次拍翻跳，紧张了', 95, 12000000, 'approved', NULL, 1, 1680, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 5, 'Pink Venom 自编改编版', '/demo/u_pinkvenom2.mp4', NULL, '改编了几个动作，大家帮看看', 120, 15000000, 'approved', NULL, 0, 850, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 7, '我跳的OMG - NewJeans', '/demo/u_omg.mp4', NULL, '零基础一个月成果！', 100, 13000000, 'pending', NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 8, 'HIP-HOP solo 练习', '/demo/u_hiphop_solo.mp4', NULL, 'solo练习，求老师指点', 60, 8000000, 'pending', NULL, 0, 0, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(1, 9, '随便跳跳', '/demo/u_random.mp4', NULL, '今天心情好', 30, 4000000, 'rejected', '画面抖动严重，建议固定机位重拍', 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 10, 'Rush Hour - Crush 编舞展示', '/demo/u_rushhour.mp4', NULL, '学了两周的舞', 110, 14000000, 'approved', NULL, 1, 2300, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 4. 问答：更多问题+回答
INSERT INTO qa_question (store_id, user_id, content, images, answer_count, like_count, status, created_at) VALUES
(1, 3, '刚报了K-pop入门课，需要提前准备什么装备吗？需要买专门的舞鞋吗？', NULL, 2, 15, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(1, 4, '请问一周上几节课进步最快？我目前只有周末有空', NULL, 3, 8, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 7, '跳K-pop总是跟不上节奏怎么办？有没有什么练习方法？', NULL, 2, 23, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 9, '零基础能学Breaking吗？会不会太难了', NULL, 2, 12, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 5, '推荐一下适合练爆发力的音乐吧，偏HIP-HOP的', NULL, 1, 6, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 6, '广告内容测试（管理员可隐藏）', NULL, 0, 0, 0, DATE_SUB(NOW(), INTERVAL 12 HOUR));

-- 对应回答
-- q2(id=2)
INSERT INTO qa_answer (question_id, user_id, teacher_id, is_teacher, content, like_count, status, created_at) VALUES
(2, 0, 2, 1, '建议穿舒适的运动服和运动鞋即可，新手不用买专门舞鞋。带一瓶水和毛巾就行，课前30分钟别吃太饱哦！', 28, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2, 1, NULL, 0, '我刚去的时候穿的普通运动鞋，跳了半年才买的舞鞋，前期真的不用', 8, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- q3(id=3)
(3, 0, 1, 1, '零基础建议一周2-3次课比较合适，保持节奏比一次上很多更重要。平时在家也要练基础，每天15-20分钟就够', 15, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 5, NULL, 0, '同意！我之前一周猛上5节课反而累到坚持不下来，后来调整成2-3次进步反而快了', 5, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3, 2, NULL, 0, '课后一定要看老师发的复习视频，这个比上新课还重要', 10, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- q4(id=4)
(4, 0, 3, 1, '跟不上节奏是很正常的，建议先从数拍子开始，跟着节拍器练isolation（身体分离），再慢慢跟音乐。不要急着跟整首歌，分段练', 35, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 10, NULL, 0, '推荐用YouTube上的"1MILLION Dance Tutorial"频道，他们有很多慢速教学', 12, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- q5(id=5)
(5, 0, 4, 1, 'Breaking对力量要求比较高，零基础可以先从Toprock和Footwork基础动作练起，不建议直接上Powermove。我们有Breaking入门班专门给零基础用户开的', 20, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5, 8, NULL, 0, '我也是零基础学Breaking，前两个月都在练基础，现在已经能做简单的freeze了，加油！', 7, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- q6(id=6)
(6, 0, 2, 1, '推荐：Bodak Yellow - Cardi B、SICKO MODE - Travis Scott、Money - Lisa，都是节奏感很强适合练爆发的', 10, 1, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 更新问题的回答数
UPDATE qa_question SET answer_count=2 WHERE id=2;
UPDATE qa_question SET answer_count=3 WHERE id=3;
UPDATE qa_question SET answer_count=2 WHERE id=4;
UPDATE qa_question SET answer_count=2 WHERE id=5;
UPDATE qa_question SET answer_count=1 WHERE id=6;

-- 5. 预约记录 booking（历史已上 + 未来待上）
INSERT INTO booking (store_id, user_id, schedule_id, status, locked_hours, is_admin, cancel_deduct, created_at) VALUES
-- 历史已签到
(1, 2, 100, 'checked', 1, 0, 0, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(1, 5, 101, 'checked', 1, 0, 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- 未来已约
(1, 7, 202, 'booked', 1, 0, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 8, 203, 'booked', 1, 0, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 10, 200, 'booked', 1, 0, 0, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(1, 3, 202, 'booked', 1, 0, 0, DATE_SUB(NOW(), INTERVAL 3 HOUR));

-- 6. 签到记录 checkin（booking_id=1已有签到，给新插入的已上课次补签到）
INSERT INTO checkin (store_id, booking_id, user_id, schedule_id, method, check_time)
SELECT 1, id, user_id, schedule_id, 'scan', DATE_SUB(NOW(), INTERVAL 7 DAY)
FROM booking WHERE user_id=2 AND schedule_id=100 LIMIT 1;
INSERT INTO checkin (store_id, booking_id, user_id, schedule_id, method, check_time)
SELECT 1, id, user_id, schedule_id, 'scan', DATE_SUB(NOW(), INTERVAL 5 DAY)
FROM booking WHERE user_id=5 AND schedule_id=101 LIMIT 1;

-- 7. 兑换订单（增加几条不同状态）
-- 查看现有reedeem_order
-- 增加一些已完成/配送中
-- 不插入，已有的2条够用

-- 8. 一些点赞记录（给投稿视频和问答一些点赞数据，避免显示0）
INSERT INTO like_record (user_id, target_type, target_id, created_at) VALUES
(2, 'video', 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(5, 'video', 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(7, 'video', 2, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(8, 'video', 8, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10, 'video', 13, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 'question', 2, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(7, 'question', 4, DATE_SUB(NOW(), INTERVAL 2 DAY));

SELECT '=== 模拟数据补充完成 ===' AS msg;
SELECT '`user`' t, COUNT(*) c FROM `user` UNION ALL SELECT 'video', COUNT(*) FROM video
UNION ALL SELECT 'user_video', COUNT(*) FROM user_video
UNION ALL SELECT 'qa_question', COUNT(*) FROM qa_question
UNION ALL SELECT 'qa_answer', COUNT(*) FROM qa_answer
UNION ALL SELECT 'booking', COUNT(*) FROM booking
UNION ALL SELECT 'checkin', COUNT(*) FROM checkin;
