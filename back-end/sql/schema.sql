-- ============================================================
-- 街舞工作室数字化管理系统 · 数据库表结构
-- DB: MySQL 8.x  Engine: InnoDB  Charset: utf8mb4
-- 设计原则：通用可配置可复制（所有店均带 store_id，规则进 config 表）
-- 命名：小写蛇形；金额 DECIMAL(10,2)；时间 DATETIME；逻辑删除用 status
-- 余额策略：course_balance / points_balance 为缓存值，以 *_log 流水为准；
--           课时有效期用 course_hour_batch 批次 FIFO 消耗。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `dance_studio`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `dance_studio`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 基础 / 配置
-- ============================================================

-- 门店（多门店预留，当前只有 1 家）
DROP TABLE IF EXISTS `store`;
CREATE TABLE `store` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '门店ID',
  `name`        VARCHAR(100) NOT NULL COMMENT '门店名称',
  `address`     VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `phone`       VARCHAR(30)  DEFAULT NULL COMMENT '联系电话',
  `logo`        VARCHAR(255) DEFAULT NULL COMMENT 'Logo URL',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店';

-- 管理员（后台单管理员，仍建表以便扩展/审计）
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `store_id`      BIGINT       NOT NULL COMMENT '所属门店',
  `username`      VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `password_hash` VARCHAR(100) NOT NULL COMMENT '密码哈希(BCrypt)',
  `nickname`      VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `role`          VARCHAR(20)  NOT NULL DEFAULT 'owner' COMMENT '角色 owner店主',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  `last_login_at` DATETIME     DEFAULT NULL COMMENT '最后登录时间',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员';

-- 系统配置（所有可配置业务规则；key 维度 + 门店维度）
DROP TABLE IF EXISTS `config`;
CREATE TABLE `config` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `store_id`     BIGINT       NOT NULL COMMENT '门店ID(0=全局默认)',
  `config_key`   VARCHAR(64)  NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(255) NOT NULL COMMENT '配置值',
  `remark`       VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_key` (`store_id`,`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置/业务规则';

-- ============================================================
-- 2. 学员
-- ============================================================
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '学员ID',
  `store_id`       BIGINT       NOT NULL COMMENT '所属门店',
  `openid`         VARCHAR(64)  DEFAULT NULL COMMENT '微信openid',
  `unionid`        VARCHAR(64)  DEFAULT NULL COMMENT '微信unionid',
  `name`           VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
  `phone`          VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `gender`         TINYINT      DEFAULT 0 COMMENT '性别 0未知 1男 2女',
  `birthday`       DATE         DEFAULT NULL COMMENT '生日',
  `avatar`         VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `course_balance` INT          NOT NULL DEFAULT 0 COMMENT '可用课时(缓存,不含已冻结)',
  `frozen_hours`   INT          NOT NULL DEFAULT 0 COMMENT '已约课冻结课时',
  `points_balance` INT          NOT NULL DEFAULT 0 COMMENT '积分余额(缓存)',
  `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0停用',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_store` (`store_id`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学员';

-- 课时充值批次（用于有效期 FIFO 消耗）
DROP TABLE IF EXISTS `course_hour_batch`;
CREATE TABLE `course_hour_batch` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT   NOT NULL COMMENT '学员ID',
  `total`      INT      NOT NULL COMMENT '本批课时总数',
  `remaining`  INT      NOT NULL COMMENT '本批剩余课时',
  `source`     VARCHAR(20) NOT NULL DEFAULT 'recharge' COMMENT '来源 recharge充值 gift赠送',
  `order_id`   BIGINT   DEFAULT NULL COMMENT '关联充值订单',
  `expire_at`  DATETIME DEFAULT NULL COMMENT '过期时间(NULL=永久)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课时充值批次(有效期管理)';

-- 充值订单
DROP TABLE IF EXISTS `recharge_order`;
CREATE TABLE `recharge_order` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `store_id`    BIGINT        NOT NULL,
  `student_id`  BIGINT        NOT NULL,
  `hours`       INT           NOT NULL COMMENT '充值课时',
  `gift_hours`  INT           NOT NULL DEFAULT 0 COMMENT '赠送课时',
  `amount`      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '实付金额',
  `points_gain` INT           NOT NULL DEFAULT 0 COMMENT '本次充值获得积分',
  `validity_days` INT         DEFAULT NULL COMMENT '本单有效期天数(NULL取默认)',
  `operator_id` BIGINT        DEFAULT NULL COMMENT '操作管理员',
  `remark`      VARCHAR(255)  DEFAULT NULL,
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课时充值订单';

-- 课时流水
DROP TABLE IF EXISTS `hour_log`;
CREATE TABLE `hour_log` (
  `id`            BIGINT      NOT NULL AUTO_INCREMENT,
  `student_id`    BIGINT      NOT NULL,
  `type`          VARCHAR(20) NOT NULL COMMENT 'recharge充值/gift赠送/lock约课锁定/deduct签到扣减/refund取消退回/noshow爽约扣减/expire过期',
  `amount`        INT         NOT NULL COMMENT '变动(正负)',
  `balance_after` INT         NOT NULL COMMENT '变动后可用课时',
  `ref_type`      VARCHAR(20) DEFAULT NULL COMMENT '关联类型 booking/recharge',
  `ref_id`        BIGINT      DEFAULT NULL COMMENT '关联ID',
  `operator_id`   BIGINT      DEFAULT NULL COMMENT '操作人(管理员代操作时)',
  `remark`        VARCHAR(255) DEFAULT NULL,
  `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课时流水';

-- 积分流水
DROP TABLE IF EXISTS `point_log`;
CREATE TABLE `point_log` (
  `id`            BIGINT      NOT NULL AUTO_INCREMENT,
  `student_id`    BIGINT      NOT NULL,
  `type`          VARCHAR(20) NOT NULL COMMENT 'checkin签到/recharge充值/consume消费/redeem兑换扣减/manual手动/expire过期',
  `amount`        INT         NOT NULL COMMENT '变动(正负)',
  `balance_after` INT         NOT NULL COMMENT '变动后积分',
  `ref_type`      VARCHAR(20) DEFAULT NULL COMMENT 'checkin/recharge/redeem',
  `ref_id`        BIGINT      DEFAULT NULL,
  `operator_id`   BIGINT      DEFAULT NULL,
  `remark`        VARCHAR(255) DEFAULT NULL,
  `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

-- ============================================================
-- 3. 课程 / 排课
-- ============================================================

-- 舞种分类（可增删）
DROP TABLE IF EXISTS `dance_type`;
CREATE TABLE `dance_type` (
  `id`       BIGINT      NOT NULL AUTO_INCREMENT,
  `store_id` BIGINT      NOT NULL,
  `name`     VARCHAR(50) NOT NULL COMMENT 'Jazz/Kpop/Hiphop/MVDance/Breaking…',
  `intro`    VARCHAR(500) DEFAULT NULL COMMENT '舞种介绍',
  `sort`     INT         NOT NULL DEFAULT 0 COMMENT '排序',
  `status`   TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='舞种分类';

-- 教室字典（排课下拉选择）
DROP TABLE IF EXISTS `room`;
CREATE TABLE `room` (
  `id`       BIGINT      NOT NULL AUTO_INCREMENT,
  `store_id` BIGINT      NOT NULL,
  `name`     VARCHAR(50) NOT NULL COMMENT '教室名',
  `max_capacity` INT     NOT NULL DEFAULT 0 COMMENT '最大容纳人数(0=不限制)',
  `sort`     INT         NOT NULL DEFAULT 0 COMMENT '排序',
  `status`   TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室字典';

-- 难度等级（可配）
DROP TABLE IF EXISTS `course_level`;
CREATE TABLE `course_level` (
  `id`       BIGINT      NOT NULL AUTO_INCREMENT,
  `store_id` BIGINT      NOT NULL,
  `name`     VARCHAR(50) NOT NULL COMMENT '入门/进阶/高级…',
  `sort`     INT         NOT NULL DEFAULT 0,
  `status`   TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='难度等级';

-- 课程（模板：团课/私教）
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `store_id`      BIGINT        NOT NULL,
  `name`          VARCHAR(100)  NOT NULL COMMENT '课程名',
  `type`          VARCHAR(10)   NOT NULL DEFAULT 'group' COMMENT 'group团课 private私教',
  `dance_type_id` BIGINT        DEFAULT NULL COMMENT '舞种',
  `level_id`      BIGINT        DEFAULT NULL COMMENT '难度',
  `duration_min`  INT           NOT NULL DEFAULT 60 COMMENT '时长(分钟)',
  `price`         DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '价格',
  `cost_hours`    INT           NOT NULL DEFAULT 1 COMMENT '每节消耗课时',
  `cover`         VARCHAR(255)  DEFAULT NULL COMMENT '封面',
  `description`   VARCHAR(500)  DEFAULT NULL COMMENT '简介',
  `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_dance` (`dance_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程模板';

-- 教师
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `store_id`    BIGINT       NOT NULL,
  `name`        VARCHAR(50)  NOT NULL COMMENT '教师名',
  `phone`       VARCHAR(20)  DEFAULT NULL,
  `avatar`      VARCHAR(255) DEFAULT NULL,
  `intro`       VARCHAR(500) DEFAULT NULL COMMENT '简介',
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师';

-- 排课（具体课次）
DROP TABLE IF EXISTS `course_schedule`;
CREATE TABLE `course_schedule` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT,
  `store_id`     BIGINT      NOT NULL,
  `course_id`    BIGINT      NOT NULL COMMENT '课程',
  `teacher_id`   BIGINT      DEFAULT NULL COMMENT '教师',
  `room`         VARCHAR(50) DEFAULT NULL COMMENT '教室',
  `song`         VARCHAR(100) DEFAULT NULL COMMENT '本课次曲目',
  `start_time`   DATETIME    NOT NULL COMMENT '开始时间',
  `end_time`     DATETIME    NOT NULL COMMENT '结束时间',
  `capacity`     INT         NOT NULL DEFAULT 12 COMMENT '容量(可覆盖课程默认)',
  `booked_count` INT         NOT NULL DEFAULT 0 COMMENT '已约人数',
  `status`       VARCHAR(10) NOT NULL DEFAULT 'open' COMMENT 'open报名中 full满员 finished已结束 cancelled已取消',
  `created_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_store_time` (`store_id`,`start_time`),
  KEY `idx_course` (`course_id`),
  KEY `idx_teacher_time` (`teacher_id`,`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排课/课次';

-- ============================================================
-- 4. 约课 / 签到
-- ============================================================
DROP TABLE IF EXISTS `booking`;
CREATE TABLE `booking` (
  `id`            BIGINT      NOT NULL AUTO_INCREMENT,
  `store_id`      BIGINT      NOT NULL,
  `student_id`    BIGINT      NOT NULL,
  `schedule_id`   BIGINT      NOT NULL,
  `status`        VARCHAR(12) NOT NULL DEFAULT 'booked' COMMENT 'booked待上课 checked已签到 cancelled已取消 noshow爽约 waitlist候补中',
  `locked_hours`  INT         NOT NULL DEFAULT 1 COMMENT '冻结课时',
  `waitlist_no`   INT         DEFAULT NULL COMMENT '候补序号',
  `is_admin`      TINYINT     NOT NULL DEFAULT 0 COMMENT '是否管理员代约',
  `cancel_deduct` TINYINT     NOT NULL DEFAULT 0 COMMENT '取消是否扣课时(0退回 1扣减)',
  `cancel_at`     DATETIME    DEFAULT NULL COMMENT '取消时间',
  `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student_schedule` (`student_id`,`schedule_id`),
  KEY `idx_schedule` (`schedule_id`,`status`),
  KEY `idx_student` (`student_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='约课单';

DROP TABLE IF EXISTS `checkin`;
CREATE TABLE `checkin` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `store_id`    BIGINT      NOT NULL,
  `booking_id`  BIGINT      NOT NULL,
  `student_id`  BIGINT      NOT NULL,
  `schedule_id` BIGINT      NOT NULL,
  `check_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  `method`      VARCHAR(10) NOT NULL DEFAULT 'scan' COMMENT 'scan扫码 manual代签',
  `operator_id` BIGINT      DEFAULT NULL COMMENT '代签管理员',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking` (`booking_id`),
  KEY `idx_schedule` (`schedule_id`),
  KEY `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录';

-- ============================================================
-- 5. 手办 / 兑换 / 发放
-- ============================================================
DROP TABLE IF EXISTS `goods`;
CREATE TABLE `goods` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `store_id`        BIGINT       NOT NULL,
  `name`            VARCHAR(100) NOT NULL COMMENT '手办名称',
  `images`          JSON         DEFAULT NULL COMMENT '多角度图URL数组',
  `description`     VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `category`        VARCHAR(10)  NOT NULL DEFAULT 'redeem' COMMENT 'claw可抓取款 redeem可兑换款',
  `points_required` INT          NOT NULL DEFAULT 0 COMMENT '抓取/兑换所需积分',
  `stock`           INT          NOT NULL DEFAULT 0 COMMENT '库存',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  `sort`            INT          NOT NULL DEFAULT 0,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_store_cat` (`store_id`,`category`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手办商品(抓取款/兑换款)';

-- 兑换/发放单（抓娃娃机 + 指定兑换 统一进单）
DROP TABLE IF EXISTS `redeem_order`;
CREATE TABLE `redeem_order` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `store_id`          BIGINT       NOT NULL,
  `student_id`        BIGINT       NOT NULL,
  `goods_id`          BIGINT       NOT NULL,
  `goods_name`        VARCHAR(100) DEFAULT NULL COMMENT '冗余快照',
  `source`            VARCHAR(10)  NOT NULL DEFAULT 'redeem' COMMENT 'claw抓取 redeem兑换',
  `points_cost`       INT          NOT NULL COMMENT '消耗积分',
  `status`            VARCHAR(12)  NOT NULL DEFAULT 'pending' COMMENT 'pending待审批 to_ship待发货 shipping配送中 completed已完成 rejected已驳回',
  `receiver_name`     VARCHAR(50)  DEFAULT NULL,
  `receiver_phone`    VARCHAR(20)  DEFAULT NULL,
  `address`           VARCHAR(255) DEFAULT NULL,
  `logistics_company` VARCHAR(50)  DEFAULT NULL,
  `logistics_no`      VARCHAR(50)  DEFAULT NULL,
  `audit_admin_id`    BIGINT       DEFAULT NULL,
  `audit_at`          DATETIME     DEFAULT NULL,
  `reject_reason`     VARCHAR(255) DEFAULT NULL,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`,`status`),
  KEY `idx_store_status` (`store_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换/发放单';

-- ============================================================
-- 6. 视频
-- ============================================================
DROP TABLE IF EXISTS `video`;
CREATE TABLE `video` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `store_id`    BIGINT       NOT NULL,
  `course_id`   BIGINT       DEFAULT NULL COMMENT '所属课程',
  `schedule_id` BIGINT       DEFAULT NULL COMMENT '所属课次',
  `video_date`  DATE         DEFAULT NULL COMMENT '课堂日期',
  `title`       VARCHAR(100) NOT NULL,
  `file_url`    VARCHAR(255) NOT NULL COMMENT '视频地址(私有,需签名)',
  `cover_url`   VARCHAR(255) DEFAULT NULL,
  `duration`    INT          DEFAULT NULL COMMENT '时长(秒)',
  `file_size`   BIGINT       DEFAULT NULL COMMENT '字节',
  `scope_type`  VARCHAR(12)  NOT NULL DEFAULT 'checked_in' COMMENT 'checked_in已签到 booked已约课 custom指定',
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_schedule` (`schedule_id`),
  KEY `idx_store_date` (`store_id`,`video_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课堂视频';

-- 视频可见学员（scope_type=custom 时使用；其余可运行时按签到/约课计算）
DROP TABLE IF EXISTS `video_visible`;
CREATE TABLE `video_visible` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT,
  `video_id`   BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_student` (`video_id`,`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频可见学员名单';

-- 学员投稿视频（公开排行榜，区别于课堂视频）
DROP TABLE IF EXISTS `user_video`;
CREATE TABLE `user_video` (
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

-- 问答-问题
DROP TABLE IF EXISTS `qa_question`;
CREATE TABLE `qa_question` (
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

-- 问答-回答（学员或老师）
DROP TABLE IF EXISTS `qa_answer`;
CREATE TABLE `qa_answer` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT       NOT NULL,
  `student_id`  BIGINT       NOT NULL COMMENT '回答学员',
  `teacher_id`  BIGINT       DEFAULT NULL COMMENT '老师回答时关联teacher',
  `is_teacher`  TINYINT      NOT NULL DEFAULT 0 COMMENT '1老师回答',
  `content`     VARCHAR(500) NOT NULL,
  `like_count`  INT          NOT NULL DEFAULT 0,
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0后台隐藏',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_question` (`question_id`,`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答-回答';

-- 点赞记录（问题/回答/投稿视频 统一去重）
DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `student_id`  BIGINT      NOT NULL,
  `target_type` VARCHAR(10) NOT NULL COMMENT 'question/answer/video',
  `target_id`   BIGINT      NOT NULL,
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_like` (`student_id`,`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录';

-- ============================================================
-- 7. 首页 Banner（后台可配置，小程序首页顶部轮播）
-- ============================================================
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'BannerID',
  `store_id`   BIGINT       NOT NULL COMMENT '门店ID',
  `title`      VARCHAR(100) NOT NULL COMMENT '主标题',
  `subtitle`   VARCHAR(200) DEFAULT NULL COMMENT '副标题',
  `image_url`  VARCHAR(255) NOT NULL COMMENT '图片URL',
  `link_type`  VARCHAR(20)  NOT NULL DEFAULT 'none' COMMENT '跳转类型 none/tab/url',
  `link_value` VARCHAR(255) DEFAULT NULL COMMENT '跳转目标(tab路径或外部链接)',
  `sort`       INT          NOT NULL DEFAULT 0 COMMENT '排序(小在前)',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1上架 0下架',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_store_status` (`store_id`,`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页Banner';

-- ============================================================
-- 8. 消息通知
-- ============================================================
DROP TABLE IF EXISTS `notification`;CREATE TABLE `notification` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT       NOT NULL,
  `type`       VARCHAR(20)  NOT NULL COMMENT 'booking约课 checkin签到 redeem兑换 remind提醒 system系统',
  `title`      VARCHAR(100) NOT NULL,
  `content`    VARCHAR(500) DEFAULT NULL,
  `ref_type`   VARCHAR(20)  DEFAULT NULL,
  `ref_id`     BIGINT       DEFAULT NULL,
  `is_read`    TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student_read` (`student_id`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息通知';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 9. 初始化数据（默认门店 + 配置 + 字典 + 管理员 + Banner）
-- ============================================================

-- 默认门店
INSERT INTO `store` (`id`,`name`,`address`,`phone`) VALUES
  (1, 'XX 街舞工作室', '上海市', '021-00000000');

-- 默认管理员  账号 admin / 密码 admin123
-- 注意：password_hash 为占位，后端首次启动请用 BCrypt 重置真实密码！
INSERT INTO `admin` (`id`,`store_id`,`username`,`password_hash`,`nickname`,`role`) VALUES
  (1, 1, 'admin', '$2a$10$PLACEHOLDER_REPLACE_WITH_BCRYPT_HASH', '店主', 'owner');

-- 业务规则默认配置（store_id=1）
INSERT INTO `config` (`store_id`,`config_key`,`config_value`,`remark`) VALUES
  (1,'default_capacity_group','12','团课排课默认容量'),
  (1,'default_capacity_private','1','私教排课默认容量'),
  (1,'lock_on_booking','1','约课锁定扣课时 1开0关'),
  (1,'cancel_free_hours','6','开课前N小时可免费取消'),
  (1,'book_ahead_days','7','可提前约课天数'),
  (1,'book_cutoff_min','30','开课前N分钟停止约课'),
  (1,'checkin_window_min','30','签到时间窗(开课前后N分钟)'),
  (1,'course_validity_days','365','充值课时有效期(天,0永久)'),
  (1,'points_per_checkin','100','每次签到积分'),
  (1,'points_per_yuan','1','每充值/消费1元得分'),
  (1,'points_expire_days','0','积分过期天数(0不过期)'),
  (1,'waitlist_limit','5','候补队列上限'),
  (1,'low_hours_threshold','2','课时不足提醒阈值'),
  (1,'expire_remind_days','30','临期提醒提前天数'),
  (1,'claw_cost_points','100','娃娃机每次抓取消耗积分'),
  (1,'redeem_auto_approve','0','手办发放免审批 1开0关'),
  (1,'trial_title','新人体验课 · 3节课','首页体验课标题'),
  (1,'trial_subtitle','任意舞种 · 专业老师带练','首页体验课副标题'),
  (1,'trial_price','9.9','体验课现价'),
  (1,'trial_origin_price','199','体验课原价'),
  (1,'service_phone','400-888-6688','客服电话(充值/体验课)'),
  (1,'points_per_video_like','1','投稿视频每获1赞作者得积分'),
  (1,'course_notice','请提前10分钟到店更换舞蹈鞋；开课后15分钟禁止进入教室；教室内请勿携带饮料食品。','课程注意事项(详情页)');

-- 舞种字典
INSERT INTO `dance_type` (`store_id`,`name`,`sort`) VALUES
  (1,'Jazz',1),(1,'Kpop',2),(1,'Hiphop',3),(1,'MVDance',4),(1,'Breaking',5);

-- 难度字典
INSERT INTO `course_level` (`store_id`,`name`,`sort`) VALUES
  (1,'入门',1),(1,'进阶',2),(1,'高级',3);

-- ============================================================
-- 完成。后续余额变动务必：流水(*_log) 与 缓存(student.*) 在同一事务内更新。
-- ============================================================
