-- ============================================================
-- V2 升级脚本：小程序改版（资讯问答/视频投稿/点赞排行/手办兑换）
-- 变更：新增4表 + course_schedule.song + config 新增项 + 测试数据
-- 可重复执行（幂等）
-- ============================================================
USE dance_studio;

-- 1. course_schedule 增加曲目字段
SET @has_song = (SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema='dance_studio' AND table_name='course_schedule' AND column_name='song');
SET @ddl = IF(@has_song=0,
  'ALTER TABLE course_schedule ADD COLUMN song VARCHAR(100) DEFAULT NULL COMMENT ''本课次曲目'' AFTER room',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.2 dance_type 增加舞种介绍字段
SET @has_intro = (SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema='dance_studio' AND table_name='dance_type' AND column_name='intro');
SET @ddl = IF(@has_intro=0,
  'ALTER TABLE dance_type ADD COLUMN intro VARCHAR(500) DEFAULT NULL COMMENT ''舞种介绍'' AFTER name',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. 投稿视频表（公开排行榜，区别于课堂视频 video）
CREATE TABLE IF NOT EXISTS `user_video` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `store_id`        BIGINT       NOT NULL,
  `student_id`      BIGINT       NOT NULL COMMENT '投稿学员',
  `title`           VARCHAR(100) NOT NULL COMMENT '视频标题',
  `file_url`        VARCHAR(255) NOT NULL COMMENT '视频地址(私有,需签名)',
  `cover_url`       VARCHAR(255) DEFAULT NULL COMMENT '封面(预设或自定义)',
  `description`     VARCHAR(200) DEFAULT NULL COMMENT '文案说明(200字)',
  `duration`        INT          DEFAULT NULL COMMENT '时长(秒)',
  `file_size`       BIGINT       DEFAULT NULL COMMENT '字节',
  `status`          VARCHAR(10)  NOT NULL DEFAULT 'pending' COMMENT 'pending待审 approved已通过 rejected已驳回',
  `reject_reason`   VARCHAR(255) DEFAULT NULL,
  `is_recommended`  TINYINT      NOT NULL DEFAULT 0 COMMENT '1后台推荐至问答页宫格/首页横滑',
  `like_count`      INT          NOT NULL DEFAULT 0,
  `audit_admin_id`  BIGINT       DEFAULT NULL,
  `audited_at`      DATETIME     DEFAULT NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_like` (`status`,`like_count`),
  KEY `idx_student` (`student_id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学员投稿视频(点赞排行)';

-- 3. 问答-问题
CREATE TABLE IF NOT EXISTS `qa_question` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `store_id`     BIGINT       NOT NULL,
  `student_id`   BIGINT       NOT NULL COMMENT '提问学员',
  `content`      VARCHAR(500) NOT NULL COMMENT '问题内容',
  `images`       JSON         DEFAULT NULL COMMENT '配图URL数组',
  `answer_count` INT          NOT NULL DEFAULT 0,
  `like_count`   INT          NOT NULL DEFAULT 0,
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0后台隐藏',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_store_time` (`store_id`,`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答-问题';

-- 4. 问答-回答（学员或老师）
CREATE TABLE IF NOT EXISTS `qa_answer` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT       NOT NULL,
  `student_id`  BIGINT       NOT NULL COMMENT '回答学员(老师也以学员身份关联)',
  `teacher_id`  BIGINT       DEFAULT NULL COMMENT '老师回答时关联teacher',
  `is_teacher`  TINYINT      NOT NULL DEFAULT 0 COMMENT '1老师回答',
  `content`     VARCHAR(500) NOT NULL,
  `like_count`  INT          NOT NULL DEFAULT 0,
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0后台隐藏',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_question` (`question_id`,`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答-回答';

-- 5. 点赞记录（问题/回答/投稿视频 统一去重）
CREATE TABLE IF NOT EXISTS `like_record` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `student_id`  BIGINT      NOT NULL,
  `target_type` VARCHAR(10) NOT NULL COMMENT 'question/answer/video',
  `target_id`   BIGINT      NOT NULL,
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_like` (`student_id`,`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录';

-- 6. config 新增配置项（体验课/客服/获赞积分）
INSERT INTO config (store_id, config_key, config_value, remark) VALUES
  (1,'trial_title','新人体验课 · 3节课','首页体验课标题'),
  (1,'trial_subtitle','任意舞种 · 专业老师带练','首页体验课副标题'),
  (1,'trial_price','9.9','体验课现价'),
  (1,'trial_origin_price','199','体验课原价'),
  (1,'service_phone','400-888-6688','客服电话(充值/体验课)'),
  (1,'points_per_video_like','1','投稿视频每获1赞作者得积分'),
  (1,'course_notice','请提前10分钟到店更换舞蹈鞋；开课后15分钟禁止进入教室；教室内请勿携带饮料食品。','课程注意事项(详情页)')
ON DUPLICATE KEY UPDATE config_key=config_key;

-- 7. 签到积分 10 -> 100（原型口径）
UPDATE config SET config_value='100' WHERE store_id=1 AND config_key='points_per_checkin';

-- ============================================================
-- 8. 测试数据
-- ============================================================

-- 8.1 第二家门店（原型：蒲黄榆NTP店 / 亦庄大族店）
INSERT INTO store (id, name, address, phone, status) VALUES
  (2, '亦庄大族店', '北京市大兴区大族广场', '010-88886666', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name);
UPDATE store SET name='蒲黄榆NTP店' WHERE id=1;

-- 8.2 舞种介绍
UPDATE dance_type SET intro='Hip-Hop 源自纽约街头，注重律动(Beat)与身体控制，是所有街舞风格的基础。' WHERE id=3 AND intro IS NULL;
UPDATE dance_type SET intro='Jazz 讲究力量与柔美的结合，提升身体线条感与表现力。' WHERE id=1 AND intro IS NULL;
UPDATE dance_type SET intro='K-pop 韩流编舞，还原 MV 打歌舞台，节奏鲜明氛围感强。' WHERE id=2 AND intro IS NULL;
UPDATE dance_type SET intro='Urban 以音乐性为核心的都市编舞风格，融合多种舞蹈元素。' WHERE id=6 AND intro IS NULL;

-- 8.3 门店2 排课（今天/明天，相对日期保证随时可测）
DELETE FROM course_schedule WHERE id >= 200;
INSERT INTO course_schedule (id, store_id, course_id, teacher_id, room, song, start_time, end_time, capacity, booked_count, status) VALUES
  (200, 2, 10, 3, 'B教室', 'Beat It - Michael Jackson', DATE_ADD(NOW(), INTERVAL 1 HOUR),  DATE_ADD(NOW(), INTERVAL 2 HOUR),  12, 6,  'open'),
  (201, 2, 14, 6, 'A教室', 'Pink Venom - BLACKPINK',    DATE_ADD(NOW(), INTERVAL 2 HOUR),  DATE_ADD(NOW(), INTERVAL 3 HOUR),  10, 10, 'open'),
  (202, 2, 16, 7, 'A教室', 'Smart - LE SSERAFIM',       DATE_ADD(CURDATE(), INTERVAL 25 HOUR), DATE_ADD(CURDATE(), INTERVAL 26 HOUR), 12, 4, 'open'),
  (203, 2, 13, 5, 'B教室', NULL,                        DATE_ADD(CURDATE(), INTERVAL 31 HOUR), DATE_ADD(CURDATE(), INTERVAL 32 HOUR), 8,  1,  'open');

-- 门店1 部分课次补曲目
UPDATE course_schedule SET song='How You Like That - BLACKPINK' WHERE id=103 AND song IS NULL;
UPDATE course_schedule SET song='Ditto - NewJeans' WHERE id=104 AND song IS NULL;

-- 8.3 投稿视频（榜单6条 + 待审1条）
DELETE FROM user_video WHERE id < 100;
INSERT INTO user_video (id, store_id, student_id, title, file_url, cover_url, description, duration, status, is_recommended, like_count, audited_at) VALUES
  (1, 1, (SELECT id FROM student ORDER BY id LIMIT 1), 'Smart - LE SSERAFIM',        '/video/smart.mp4',        NULL, '重点动作教学 · 10 分钟', 587, 'approved', 1, 3200, NOW()),
  (2, 1, (SELECT id FROM student ORDER BY id LIMIT 1), 'Pink Venom - BLACKPINK',     '/video/pinkvenom.mp4',    NULL, '副歌翻跳 · 12 分钟',     723, 'approved', 1, 2800, NOW()),
  (3, 1, (SELECT id FROM student ORDER BY id LIMIT 1), 'Standing Next to You - Jung Kook', '/video/sny.mp4',   NULL, 'Jung Kook 翻跳 · 14 分钟', 838, 'approved', 0, 2100, NOW()),
  (4, 1, (SELECT id FROM student ORDER BY id LIMIT 1), 'How You Like That - BLACKPINK', '/video/hylt.mp4',     NULL, '完整版教学 · 18 分钟',  1042, 'approved', 0, 1500, NOW()),
  (5, 1, (SELECT id FROM student ORDER BY id LIMIT 1), 'Anti-Hero',                  '/video/antihero.mp4',     NULL, '副歌翻跳 · 13 分钟',     776, 'approved', 0, 1200, NOW()),
  (6, 1, (SELECT id FROM student ORDER BY id LIMIT 1), 'Ditto - NewJeans',           '/video/ditto.mp4',        NULL, '手势舞 · 9 分钟',        521, 'approved', 0, 980,  NOW()),
  (7, 1, (SELECT id FROM student ORDER BY id LIMIT 1), '我的翻跳视频',               '/video/mine.mp4',         NULL, '第一次投稿，求点赞',     300, 'pending',  0, 0,    NULL);

-- 8.4 问答测试数据
DELETE FROM qa_answer WHERE question_id < 100;
DELETE FROM qa_question WHERE id < 100;
INSERT INTO qa_question (id, store_id, student_id, content, answer_count, like_count, created_at) VALUES
  (1, 1, (SELECT id FROM student ORDER BY id LIMIT 1), '零基础想学 K-pop 成品舞，请问需要先练基本功吗？大概多久能跟上一支完整舞？', 2, 12, DATE_SUB(NOW(), INTERVAL 2 HOUR));
INSERT INTO qa_answer (question_id, student_id, teacher_id, is_teacher, content, like_count, created_at) VALUES
  (1, (SELECT id FROM student ORDER BY id LIMIT 1), 3, 1, '建议先上 2-4 节 HIP-HOP 基础课，把律动和 isolation 练熟，之后跟成品舞会快很多。零基础也能直接来，老师会分层教学。', 5, DATE_SUB(NOW(), INTERVAL 100 MINUTE)),
  (1, (SELECT id FROM student ORDER BY id LIMIT 1), NULL, 0, '我也是零基础开始的，一个月就能跳完一整支啦，加油！', 2, DATE_SUB(NOW(), INTERVAL 60 MINUTE));

-- 8.5 兑换商品（原型4件）
INSERT INTO goods (id, store_id, name, images, description, category, points_required, stock, status, sort) VALUES
  (10, 1, '街舞潮牌鸭舌帽',      JSON_ARRAY(), '限量款鸭舌帽', 'redeem', 680,  10, 1, 1),
  (11, 1, 'BLACKPINK 同款手环', JSON_ARRAY(), '演唱会同款手环', 'redeem', 450,  20, 1, 2),
  (12, 1, '限量潮玩公仔',        JSON_ARRAY(), '工作室联名公仔', 'redeem', 1200, 5, 1, 3),
  (13, 1, '荧光粉运动袜',        JSON_ARRAY(), '荧光粉配色运动袜', 'redeem', 320,  30, 1, 4)
ON DUPLICATE KEY UPDATE name=VALUES(name), points_required=VALUES(points_required);

SELECT 'V2 升级完成' AS result;
