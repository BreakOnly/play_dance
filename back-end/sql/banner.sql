-- 首页 Banner（后台可配置，小程序首页顶部轮播）
USE dance_studio;

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

-- 默认三条 Banner（与原型一致；image_url 后续可在后台上传替换）
INSERT INTO `banner` (`store_id`,`title`,`subtitle`,`image_url`,`link_type`,`link_value`,`sort`,`status`) VALUES
  (1,'Coffee Break · Game Start','积分抓娃娃机 上线啦','/files/image/banner/banner1.png','none',NULL,1,1),
  (1,'暑期集训营 火热报名','Kpop / Hiphop 双舞种特训','/files/image/banner/banner2.png','tab','/pages/booking/booking',2,1),
  (1,'新人体验课 免费预约','零基础友好 · 限时开放','/files/image/banner/banner3.png','tab','/pages/booking/booking',3,1);
