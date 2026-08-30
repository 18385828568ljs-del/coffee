-- 用户画像模块补表迁移
-- 适用数据库: ruoyi
-- 说明: 本脚本只创建不存在的表，不删除或修改已有业务数据，可重复执行。

USE `ruoyi`;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `t_user_behavior_event` (
  `event_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '行为证据ID',
  `user_id` BIGINT NOT NULL COMMENT '微信用户ID',
  `event_type` VARCHAR(32) NOT NULL COMMENT '行为类型(PRODUCT_VIEW/SEARCH/CART_ADD/CART_REMOVE)',
  `scene` VARCHAR(16) NOT NULL COMMENT '业务场景(MALL/SCAN)',
  `product_id` BIGINT DEFAULT NULL COMMENT '商品ID,搜索行为可为空',
  `category_id` BIGINT DEFAULT NULL COMMENT '采集时商品分类ID',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源记录ID,加购时为购物车行ID',
  `search_keyword` VARCHAR(50) DEFAULT NULL COMMENT '搜索关键词',
  `source` VARCHAR(32) DEFAULT NULL COMMENT '行为来源(DEFAULT_LIST/PERSONALIZED_LIST/CATEGORY/SEARCH)',
  `dedup_key` VARCHAR(128) DEFAULT NULL COMMENT '行为幂等键',
  `event_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为发生时间',
  PRIMARY KEY (`event_id`),
  UNIQUE KEY `uk_behavior_dedup_key` (`dedup_key`),
  KEY `idx_behavior_user_time` (`user_id`, `event_time`),
  KEY `idx_behavior_scene_product` (`scene`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户非交易行为证据';

CREATE TABLE IF NOT EXISTS `t_user_profile` (
  `user_id` BIGINT NOT NULL COMMENT '微信用户ID',
  `order_count` INT NOT NULL DEFAULT 0 COMMENT '有效订单数',
  `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费金额',
  `avg_order_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '平均客单价',
  `preferred_price_min` DECIMAL(10,2) DEFAULT NULL COMMENT '常见消费价格下限',
  `preferred_price_max` DECIMAL(10,2) DEFAULT NULL COMMENT '常见消费价格上限',
  `last_order_time` DATETIME DEFAULT NULL COMMENT '最近有效消费时间',
  `last_active_time` DATETIME DEFAULT NULL COMMENT '最近有效行为时间',
  `evidence_count` INT NOT NULL DEFAULT 0 COMMENT '当前兴趣证据数',
  `profile_status` VARCHAR(16) NOT NULL DEFAULT 'EMPTY' COMMENT '画像状态(EMPTY/LEARNING/READY)',
  `profile_data` JSON DEFAULT NULL COMMENT '商城与扫码场景兴趣画像',
  `calculate_time` DATETIME DEFAULT NULL COMMENT '最近成功计算时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  KEY `idx_profile_status` (`profile_status`),
  KEY `idx_profile_calculate_time` (`calculate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信用户统一画像';
