-- ============================================================
-- upgrade_user_rename.sql
-- student 表更名为 user（用户表，兼容后续教师登录），student_id 列统一改 user_id
-- 适用于已按 schema.sql 初始化的存量库，保留全部数据。
-- 全新安装直接用更新后的 schema.sql，无需执行本脚本。
-- ============================================================

-- 1. 主表更名 + 预留角色字段（student学员 / teacher教师，教师登录流程后续实现）
RENAME TABLE `student` TO `user`;
ALTER TABLE `user`
  ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'student' COMMENT '角色：student学员 teacher教师(预留)' AFTER `store_id`;

-- 2. 各业务表 student_id → user_id（列 + 索引同步改名）
ALTER TABLE `course_hour_batch` RENAME COLUMN `student_id` TO `user_id`, RENAME INDEX `idx_student` TO `idx_user`;
ALTER TABLE `recharge_order`    RENAME COLUMN `student_id` TO `user_id`, RENAME INDEX `idx_student` TO `idx_user`;
ALTER TABLE `hour_log`          RENAME COLUMN `student_id` TO `user_id`, RENAME INDEX `idx_student` TO `idx_user`;
ALTER TABLE `point_log`         RENAME COLUMN `student_id` TO `user_id`, RENAME INDEX `idx_student` TO `idx_user`;
ALTER TABLE `booking`           RENAME COLUMN `student_id` TO `user_id`,
  RENAME INDEX `idx_student_schedule` TO `idx_user_schedule`,
  RENAME INDEX `idx_student` TO `idx_user`;
ALTER TABLE `checkin`           RENAME COLUMN `student_id` TO `user_id`, RENAME INDEX `idx_student` TO `idx_user`;
ALTER TABLE `redeem_order`      RENAME COLUMN `student_id` TO `user_id`, RENAME INDEX `idx_student` TO `idx_user`;
ALTER TABLE `video_visible`     RENAME COLUMN `student_id` TO `user_id`, RENAME INDEX `uk_video_student` TO `uk_video_user`;
ALTER TABLE `user_video`        RENAME COLUMN `student_id` TO `user_id`, RENAME INDEX `idx_student` TO `idx_user`;
ALTER TABLE `like_record`       RENAME COLUMN `student_id` TO `user_id`;
ALTER TABLE `notification`      RENAME COLUMN `student_id` TO `user_id`, RENAME INDEX `idx_student_read` TO `idx_user_read`;

-- qa_question / qa_answer 的 student_id 无索引，仅改列名
ALTER TABLE `qa_question` RENAME COLUMN `student_id` TO `user_id`;
ALTER TABLE `qa_answer`   RENAME COLUMN `student_id` TO `user_id`;
