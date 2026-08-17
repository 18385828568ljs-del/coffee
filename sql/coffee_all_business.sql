/*
 ============================================================================
 咖啡商城完整业务表结构 - 非破坏性初始化版本
 ============================================================================

 项目名称: 咖啡商城扫码点单系统
 创建日期: 2024
 最后更新: 2026-08-06

 【执行顺序】
 1. ry_20210924.sql      - 若依框架基础表（系统表）
 2. quartz.sql           - Quartz定时任务框架表
 3. coffee_all_business.sql - 本文件（咖啡商城业务表）

 【包含内容】
 本文件包含咖啡商城基础业务表，并在末尾加载装修平台迁移脚本，共分为5个模块：

 第一部分: 商城核心业务
   - 商品相关: t_category, t_product, t_cart, t_banner
   - 订单相关: t_order, t_order_item, t_address
   - 用户相关: t_wxuser, t_user_behavior_event, t_user_profile
   - 会员钱包: t_member, t_wallet, t_wallet_log, t_recharge_record, t_recharge_template
   - 支付日志: t_payment_log
   - 营销活动: t_marketing_activity, t_marketing_activity_scope
   - 线下活动: t_offline_activity, t_offline_activity_signup

 第二部分: 扫码点单业务（7个表）
   - 点单分类: t_scan_category
   - 点单商品: t_scan_product, t_scan_product_spec, t_scan_product_spec_option
   - 桌台管理: t_scan_table_qrcode
   - 点单订单: t_scan_order, t_scan_order_item

 第三部分: 扫码购物车（1个表）
   - 购物车: t_scan_cart

 第四部分: 后台菜单配置
   - sys_menu 数据配置
   - sys_job 定时任务配置

 第五部分: 商家装修平台（15个表）
   - 租户门店: merchants, merchant_members, stores, merchant_member_stores
   - 主题发布: system_theme_templates, themes, theme_drafts, theme_versions, store_theme_bindings
   - 素材与生成: assets, theme_asset_refs, component_background_slots, ai_generation_tasks, ai_generation_results
   - 真机预览: theme_preview_sessions

 【注意事项】
 1. 本脚本可重复执行，不删除已有表和业务数据
 2. 已存在的表不会由 CREATE TABLE IF NOT EXISTS 自动补充字段，结构升级请使用独立迁移脚本
 3. 执行前仍建议备份数据库
 4. 确保已先执行 ry_20210924.sql 和 quartz.sql
 5. 字符集统一使用 utf8mb4

 【版本历史】
 - v1.0: 初始版本
 - v2.0: 添加线下活动功能
 - v3.0: 完善扫码点单履约流程（取餐号、催单等）
 - v4.0: 合并所有业务SQL文件
 - v5.0: 改为保留已有数据的可重复执行初始化脚本
 - v5.1: 统一扫码点单菜单 ID 与后台权限标识
 - v5.2: 添加统一用户画像及推荐行为字段

 ============================================================================
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `t_category` (
  `category_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

CREATE TABLE IF NOT EXISTS `t_product` (
  `product_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `origin` VARCHAR(100) DEFAULT NULL COMMENT '产地',
  `processing_method` VARCHAR(100) DEFAULT NULL COMMENT '处理法',
  `roast_level` VARCHAR(50) DEFAULT NULL COMMENT '烘焙度',
  `flavor_notes` VARCHAR(500) DEFAULT NULL COMMENT '风味描述',
  `description` TEXT COMMENT '详细描述',
  `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '主图地址',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '商品状态(0-下架,1-上架)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`product_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品';

CREATE TABLE IF NOT EXISTS `t_product_image` (
  `image_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片地址',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  `is_main` TINYINT(1) DEFAULT 0 COMMENT '是否主图(0-否,1-是)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`image_id`),
  KEY `idx_product_image_product` (`product_id`, `sort_order`),
  KEY `idx_product_image_main` (`product_id`, `is_main`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片';

INSERT INTO `t_product_image` (`product_id`, `image_url`, `sort_order`, `is_main`, `create_by`, `create_time`)
SELECT p.`product_id`, p.`image_url`, 0, 1, p.`create_by`, COALESCE(p.`create_time`, NOW())
FROM `t_product` p
WHERE p.`image_url` IS NOT NULL
  AND p.`image_url` <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM `t_product_image` pi
    WHERE pi.`product_id` = p.`product_id`
      AND pi.`image_url` = p.`image_url`
      AND pi.`is_main` = 1
  );

CREATE TABLE IF NOT EXISTS `t_cart` (
  `cart_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `spec` VARCHAR(100) DEFAULT NULL COMMENT '商品规格',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`cart_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';

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

CREATE TABLE IF NOT EXISTS `t_address` (
  `address_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `receiver_name` VARCHAR(100) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
  `province` VARCHAR(50) DEFAULT NULL COMMENT '省份',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
  `district` VARCHAR(50) DEFAULT NULL COMMENT '区县',
  `detail_address` VARCHAR(500) NOT NULL COMMENT '详细地址',
  `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否默认地址(0-否,1-是)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`address_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址';

CREATE TABLE IF NOT EXISTS `t_order` (
  `order_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `discount_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠金额',
  `freight_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '运费金额',
  `activity_summary` VARCHAR(255) DEFAULT NULL COMMENT '活动摘要',
  `receiver_name` VARCHAR(100) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
  `receiver_address` VARCHAR(500) NOT NULL COMMENT '收货地址',
  `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '订单状态(0-待支付,1-待发货,2-已发货,3-已完成,4-已取消)',
  `pay_type` VARCHAR(20) DEFAULT '' COMMENT '支付方式(wechat,balance等)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '交易信息最近变更时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间',
  `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
  `express_no` VARCHAR(64) DEFAULT NULL COMMENT '物流单号',
  `refund_status` TINYINT DEFAULT 0 COMMENT '退款状态(0-未退款,1-退款中,2-已退款,3-退款失败)',
  `refund_reason` VARCHAR(200) DEFAULT NULL COMMENT '退款原因',
  `refund_reject_reason` VARCHAR(200) DEFAULT NULL COMMENT '退款拒绝原因',
  `refund_apply_time` DATETIME DEFAULT NULL COMMENT '退款申请时间',
  `refund_time` DATETIME DEFAULT NULL COMMENT '退款完成时间',
  `refund_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '退款金额',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_order_user_update` (`user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

-- 兼容已经初始化过的数据库；本文件仍可重复执行，不另建业务迁移文件。
SET @has_order_update_time = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_order' AND COLUMN_NAME = 'update_time'
);
SET @add_order_update_time = IF(
  @has_order_update_time = 0,
  'ALTER TABLE `t_order` ADD COLUMN `update_time` DATETIME DEFAULT NULL COMMENT ''交易信息最近变更时间'' AFTER `create_time`',
  'SELECT 1'
);
PREPARE add_order_update_time_stmt FROM @add_order_update_time;
EXECUTE add_order_update_time_stmt;
DEALLOCATE PREPARE add_order_update_time_stmt;

SET @has_order_user_update_index = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_order' AND INDEX_NAME = 'idx_order_user_update'
);
SET @add_order_user_update_index = IF(
  @has_order_user_update_index = 0,
  'ALTER TABLE `t_order` ADD INDEX `idx_order_user_update` (`user_id`, `update_time`)',
  'SELECT 1'
);
PREPARE add_order_user_update_index_stmt FROM @add_order_user_update_index;
EXECUTE add_order_user_update_index_stmt;
DEALLOCATE PREPARE add_order_user_update_index_stmt;

CREATE TABLE IF NOT EXISTS `t_order_item` (
  `item_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称(快照)',
  `product_image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片(快照)',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品单价(快照)',
  `spec` VARCHAR(100) DEFAULT NULL COMMENT '商品规格(快照)',
  `quantity` INT NOT NULL COMMENT '购买数量',
  `total_price` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
  PRIMARY KEY (`item_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

CREATE TABLE IF NOT EXISTS `t_banner` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `banner_title` VARCHAR(255) DEFAULT NULL COMMENT '轮播图标题',
  `banner_url` VARCHAR(255) DEFAULT NULL COMMENT '轮播图连接',
  `banner_img` VARCHAR(255) DEFAULT NULL COMMENT '图片',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图';

CREATE TABLE IF NOT EXISTS `t_wxuser` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `nickname` VARCHAR(100) DEFAULT NULL COMMENT '微信名称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `openid` VARCHAR(100) DEFAULT NULL COMMENT '微信唯一标识符',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT 'phone',
  `gender` INT DEFAULT NULL COMMENT '性别',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wxuser_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信用户';

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

CREATE TABLE IF NOT EXISTS `t_marketing_activity` (
  `activity_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `title` VARCHAR(200) NOT NULL COMMENT '活动标题',
  `type` TINYINT(1) NOT NULL COMMENT '活动类型(1-满减,2-折扣,3-赠品,4-包邮,5-限时特价)',
  `target_type` VARCHAR(20) NOT NULL DEFAULT 'mall' COMMENT '适用场景(mall-仅商城,scan-仅点单,both-商城+点单)',
  `scope_type` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '适用范围类型(0-全部商品,1-指定分类,2-指定商品)',
  `condition_min_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '最低消费金额',
  `condition_min_quantity` BIGINT DEFAULT NULL COMMENT '最低购买数量',
  `condition_new_user_only` TINYINT(1) DEFAULT NULL COMMENT '是否仅新用户',
  `effect_mode` VARCHAR(50) DEFAULT NULL COMMENT '优惠效果类型',
  `effect_value` DECIMAL(10,2) DEFAULT NULL COMMENT '优惠效果值',
  `gift_product_id` BIGINT DEFAULT NULL COMMENT '赠品商品ID',
  `gift_quantity` BIGINT DEFAULT NULL COMMENT '赠品数量',
  `shipping_base_freight` DECIMAL(10,2) DEFAULT NULL COMMENT '基础运费',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态(0-禁用,1-启用)',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`activity_id`),
  KEY `idx_status` (`status`),
  KEY `idx_target_type` (`target_type`),
  KEY `idx_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动';

CREATE TABLE IF NOT EXISTS `t_marketing_activity_scope` (
  `scope_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '范围明细ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `scope_type` TINYINT(1) NOT NULL COMMENT '范围类型(1-分类,2-商品)',
  `scope_target_id` BIGINT NOT NULL COMMENT '范围对象ID',
  PRIMARY KEY (`scope_id`),
  UNIQUE KEY `uk_activity_scope` (`activity_id`, `scope_type`, `scope_target_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_scope_target` (`scope_type`, `scope_target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动适用范围';

CREATE TABLE IF NOT EXISTS `t_member` (
  `member_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会员ID',
  `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
  `level` TINYINT NOT NULL DEFAULT 1 COMMENT '等级(1-4)',
  `level_name` VARCHAR(32) NOT NULL COMMENT '等级名称(由 service 写入,无库默认值,避免名称漂移)',
  `discount_rate` DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '折扣率(快照,实际结算从 sys_config 读)',
  `total_spending` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费金额',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员信息';

CREATE TABLE IF NOT EXISTS `t_wallet` (
  `wallet_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
  `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
  `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '当前余额',
  `total_recharge` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计充值金额',
  `total_gift` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计赠送金额',
  `total_consumed` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费金额',
  `frozen_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`wallet_id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包余额';

CREATE TABLE IF NOT EXISTS `t_recharge_record` (
  `record_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `recharge_no` VARCHAR(64) NOT NULL COMMENT '充值单号',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实际支付金额',
  `gift_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '赠送金额',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '到账总金额',
  `template_id` BIGINT DEFAULT NULL COMMENT '充值模板ID',
  `pay_type` VARCHAR(16) DEFAULT NULL COMMENT '支付方式',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0=待支付,1=成功,2=失败)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_recharge_no` (`recharge_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值记录';

CREATE TABLE IF NOT EXISTS `t_wallet_log` (
  `log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `type` TINYINT NOT NULL COMMENT '类型(1=充值入账,2=消费扣款,3=退款返还,4=赠送)',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '变动金额',
  `balance_before` DECIMAL(10,2) NOT NULL COMMENT '变动前余额',
  `balance_after` DECIMAL(10,2) NOT NULL COMMENT '变动后余额',
  `related_order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联订单或充值单号',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注说明',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额流水';

CREATE TABLE IF NOT EXISTS `t_payment_log` (
  `log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `business_type` VARCHAR(32) DEFAULT NULL COMMENT '业务类型(SCAN_ORDER,MALL_ORDER)',
  `business_no` VARCHAR(64) DEFAULT NULL COMMENT '业务单号',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `pay_type` VARCHAR(32) DEFAULT NULL COMMENT '支付方式',
  `pay_channel` VARCHAR(32) DEFAULT NULL COMMENT '支付渠道',
  `amount` DECIMAL(10,2) DEFAULT NULL COMMENT '支付金额',
  `status` VARCHAR(32) DEFAULT NULL COMMENT '日志状态(PAID,REFUND_SUCCESS,REFUND_FAILED)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_payment_log_business` (`business_type`, `business_no`),
  KEY `idx_payment_log_user` (`user_id`),
  KEY `idx_payment_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一支付日志';

CREATE TABLE IF NOT EXISTS `t_recharge_template` (
  `template_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '充值金额',
  `gift_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '赠送金额',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '到账总金额',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0=禁用,1=启用)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值模板';

CREATE TABLE IF NOT EXISTS `t_offline_activity` (
  `activity_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '线下活动ID',
  `title` VARCHAR(120) NOT NULL COMMENT '活动标题',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图地址',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '活动简介',
  `content` TEXT DEFAULT NULL COMMENT '活动详情',
  `start_time` DATETIME NOT NULL COMMENT '活动开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '活动结束时间',
  `signup_deadline` DATETIME DEFAULT NULL COMMENT '预约截止时间',
  `location` VARCHAR(200) DEFAULT NULL COMMENT '活动地点',
  `quota` INT NOT NULL DEFAULT 1 COMMENT '人数上限',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0=下架,1=上架)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`activity_id`),
  KEY `idx_status_time` (`status`, `start_time`, `end_time`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线下活动';

CREATE TABLE IF NOT EXISTS `t_offline_activity_signup` (
  `signup_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `activity_id` BIGINT NOT NULL COMMENT '线下活动ID',
  `user_id` BIGINT NOT NULL COMMENT '微信用户ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0=已预约,1=已取消,2=已到店,3=未到店)',
  `signup_time` DATETIME DEFAULT NULL COMMENT '预约时间',
  `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
  `checkin_time` DATETIME DEFAULT NULL COMMENT '到店核对时间',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`signup_id`),
  KEY `idx_activity_status` (`activity_id`, `status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_signup_time` (`signup_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线下活动预约记录';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- quartz 定时任务: 未支付订单超时自动取消
-- 依赖 ry_20210924.sql 中的 sys_job 表已建好。
-- status: '0'=启用,'1'=暂停。这里默认启用,商家无需进后台手动开。
-- cron: 每 5 分钟扫一次,30 分钟未支付即取消(超时阈值在 Java 代码里)。
-- =============================================================================
INSERT IGNORE INTO `sys_job` VALUES (100, '商城订单超时取消',  'DEFAULT', 'orderTimeoutTask.cancelTimeoutOrders',     '0 0/5 * * * ?', '3', '1', '0', 'admin', sysdate(), '', NULL, '商城订单 30 分钟未支付自动取消并回滚库存');
INSERT IGNORE INTO `sys_job` VALUES (101, '扫码点单订单超时取消','DEFAULT', 'scanOrderTimeoutTask.cancelTimeoutOrders', '0 0/5 * * * ?', '3', '1', '0', 'admin', sysdate(), '', NULL, '扫码点单 30 分钟未支付自动取消');
INSERT IGNORE INTO `sys_job` VALUES (102, '用户画像增量刷新',    'DEFAULT', 'userProfileTask.refreshIncrementalProfiles', '0 0/10 * * * ?', '3', '1', '0', 'admin', sysdate(), '', NULL, '每 10 分钟刷新发生过新行为或交易变化的顾客画像');
INSERT IGNORE INTO `sys_job` VALUES (103, '用户画像全量校准',    'DEFAULT', 'userProfileTask.refreshAllProfiles',         '0 0 3 * * ?'  , '3', '1', '0', 'admin', sysdate(), '', NULL, '每天凌晨 03:00 校准全部画像并清理超过 180 天的行为');
-- =============================================================================
-- 第二部分: 扫码点单业务（7个表）
-- =============================================================================

/*
 咖啡扫码点单 业务数据表
 -----------------------------------------------------------------------------
 请在 ry_20210924.sql / quartz.sql / coffee_business.sql 之后执行。
 本脚本只新增"扫码点单"相关的数据表，不改动现有 t_category / t_product / t_cart / t_order 等表。
 小程序购物车沿用原有 t_cart 表，故本脚本不再建购物车表。
 所有表统一前缀 t_scan_ 以避免与现有业务冲突。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 点单分类
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_scan_category` (
  `category_id`   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` VARCHAR(100)  NOT NULL                COMMENT '分类名称',
  `icon`          VARCHAR(500)  DEFAULT NULL            COMMENT '分类图标',
  `sort_order`    INT           DEFAULT 0               COMMENT '排序(升序)',
  `status`        TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '状态(0-停用,1-启用)',
  `create_by`     VARCHAR(64)   DEFAULT ''              COMMENT '创建者',
  `create_time`   DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `update_by`     VARCHAR(64)   DEFAULT ''              COMMENT '更新者',
  `update_time`   DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `remark`        VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫码点单-分类';

-- -----------------------------------------------------------------------------
-- 2. 点单商品
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_scan_product` (
  `product_id`   BIGINT         NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `category_id`  BIGINT         NOT NULL                COMMENT '所属分类ID',
  `product_name` VARCHAR(200)   NOT NULL                COMMENT '商品名称',
  `sub_title`    VARCHAR(200)   DEFAULT NULL            COMMENT '商品副标题/描述',
  `image_url`    VARCHAR(500)   DEFAULT NULL            COMMENT '商品主图',
  `video_url`    VARCHAR(500)   DEFAULT NULL            COMMENT '商品讲解视频地址',
  `price`        DECIMAL(10, 2) NOT NULL                COMMENT '基础价格',
  `month_sales`  INT            DEFAULT 0               COMMENT '月销量(展示用)',
  `tag`          VARCHAR(50)    DEFAULT NULL            COMMENT '标签(新品/招牌/热销等)',
  `status`       TINYINT(1)     NOT NULL DEFAULT 1      COMMENT '商品状态(0-下架,1-上架)',
  `sort_order`   INT            DEFAULT 0               COMMENT '排序',
  `create_by`    VARCHAR(64)    DEFAULT ''              COMMENT '创建者',
  `create_time`  DATETIME       DEFAULT NULL            COMMENT '创建时间',
  `update_by`    VARCHAR(64)    DEFAULT ''              COMMENT '更新者',
  `update_time`  DATETIME       DEFAULT NULL            COMMENT '更新时间',
  `remark`       VARCHAR(500)   DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`product_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫码点单-商品';

-- -----------------------------------------------------------------------------
-- 3. 商品规格(组) -- 例如: 温度 / 糖度 / 杯型 / 加料
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_scan_product_spec` (
  `spec_id`      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '规格ID',
  `product_id`   BIGINT        NOT NULL                COMMENT '商品ID',
  `spec_name`    VARCHAR(100)  NOT NULL                COMMENT '规格名称',
  `spec_type`    VARCHAR(20)   NOT NULL DEFAULT 'single' COMMENT '规格类型(single-单选,multiple-多选)',
  `required`     TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否必选',
  `sort_order`   INT           DEFAULT 0               COMMENT '排序',
  `create_time`  DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `update_time`  DATETIME      DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`spec_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫码点单-商品规格组';

-- -----------------------------------------------------------------------------
-- 4. 商品规格选项 -- 例如: 热 / 冰, 正常糖 / 少糖
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_scan_product_spec_option` (
  `option_id`     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '选项ID',
  `spec_id`       BIGINT        NOT NULL                COMMENT '规格组ID',
  `product_id`    BIGINT        NOT NULL                COMMENT '商品ID(冗余,便于查询)',
  `option_name`   VARCHAR(100)  NOT NULL                COMMENT '选项名称',
  `extra_price`   DECIMAL(10, 2) DEFAULT 0.00           COMMENT '加价金额',
  `is_default`    TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否默认',
  `sort_order`    INT           DEFAULT 0               COMMENT '排序',
  `create_time`   DATETIME      DEFAULT NULL            COMMENT '创建时间',
  PRIMARY KEY (`option_id`),
  KEY `idx_spec_id` (`spec_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫码点单-规格选项';

-- -----------------------------------------------------------------------------
-- 5. 桌台/门店二维码
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_scan_table_qrcode` (
  `table_id`     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '桌台ID',
  `shop_id`      BIGINT        NOT NULL DEFAULT 1      COMMENT '门店ID',
  `shop_name`    VARCHAR(200)  DEFAULT NULL            COMMENT '门店名称',
  `table_no`     VARCHAR(50)   NOT NULL                COMMENT '桌号',
  `scene`        VARCHAR(50)   DEFAULT 'dine_in'       COMMENT '场景(dine_in-堂食,take_out-外带)',
  `qr_url`       VARCHAR(500)  DEFAULT NULL            COMMENT '二维码直链',
  `status`       TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '状态(0-停用,1-启用)',
  `create_by`    VARCHAR(64)   DEFAULT ''              COMMENT '创建者',
  `create_time`  DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `update_by`    VARCHAR(64)   DEFAULT ''              COMMENT '更新者',
  `update_time`  DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `remark`       VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`table_id`),
  UNIQUE KEY `uk_shop_table` (`shop_id`, `table_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫码点单-桌台二维码';

-- -----------------------------------------------------------------------------
-- 6. 扫码点单订单(与电商 t_order 分离,堂食场景专用)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_scan_order` (
  `order_id`         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`         VARCHAR(64)    NOT NULL                COMMENT '订单号',
  `user_id`          BIGINT         NOT NULL                COMMENT '下单用户ID',
  `openid`           VARCHAR(100)   DEFAULT NULL            COMMENT '微信openid',
  `shop_id`          BIGINT         DEFAULT 1               COMMENT '门店ID',
  `shop_name`        VARCHAR(200)   DEFAULT NULL            COMMENT '门店名称',
  `table_no`         VARCHAR(50)    DEFAULT NULL            COMMENT '桌号',
  `scene`            VARCHAR(50)    DEFAULT 'dine_in'       COMMENT '场景',
  `total_amount`     DECIMAL(10, 2) NOT NULL                COMMENT '订单总金额(原价)',
  `pay_amount`       DECIMAL(10, 2) NOT NULL                COMMENT '实付金额',
  `discount_amount`  DECIMAL(10, 2) NOT NULL DEFAULT 0.00   COMMENT '活动优惠金额',
  `member_discount`  DECIMAL(10, 2) NOT NULL DEFAULT 0.00   COMMENT '会员折扣金额',
  `activity_summary` VARCHAR(500)   DEFAULT NULL            COMMENT '命中的活动文案,逗号分隔',
  `status`           TINYINT        NOT NULL DEFAULT 0      COMMENT '状态(0-待支付,1-保留,2-制作中,3-待取餐,4-已完成,5-已取消)',
  `pickup_no`        VARCHAR(20)    DEFAULT NULL            COMMENT '取餐号',
  `estimated_wait_minutes` INT      DEFAULT NULL            COMMENT '预计等待分钟数',
  `pay_type`         VARCHAR(20)    DEFAULT NULL            COMMENT '支付方式',
  `pay_time`         DATETIME       DEFAULT NULL            COMMENT '支付时间',
  `accept_time`      DATETIME       DEFAULT NULL            COMMENT '自动开始时间',
  `making_time`      DATETIME       DEFAULT NULL            COMMENT '开始制作时间',
  `call_time`        DATETIME       DEFAULT NULL            COMMENT '叫号时间',
  `finish_time`      DATETIME       DEFAULT NULL            COMMENT '完成时间',
  `cancel_time`      DATETIME       DEFAULT NULL            COMMENT '取消时间',
  `urge_count`       INT            NOT NULL DEFAULT 0      COMMENT '催单次数',
  `last_urge_time`   DATETIME       DEFAULT NULL            COMMENT '最后催单时间',
  `refund_status`    TINYINT        DEFAULT 0               COMMENT '退款状态(0-未退款,1-退款中,2-已退款,3-退款失败)',
  `refund_reason`    VARCHAR(200)   DEFAULT NULL            COMMENT '退款原因',
  `refund_reject_reason` VARCHAR(200) DEFAULT NULL          COMMENT '退款拒绝原因',
  `refund_apply_time` DATETIME      DEFAULT NULL            COMMENT '退款申请时间',
  `refund_time`      DATETIME       DEFAULT NULL            COMMENT '退款完成时间',
  `refund_amount`    DECIMAL(10, 2) DEFAULT NULL            COMMENT '退款金额',
  `remark`           VARCHAR(500)   DEFAULT NULL            COMMENT '备注',
  `create_by`        VARCHAR(64)    DEFAULT ''              COMMENT '创建者',
  `create_time`      DATETIME       DEFAULT NULL            COMMENT '创建时间',
  `update_by`        VARCHAR(64)    DEFAULT ''              COMMENT '更新者',
  `update_time`      DATETIME       DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_pickup_day` (`shop_id`, `pay_time`, `pickup_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_shop_table` (`shop_id`, `table_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫码点单-订单';

-- -----------------------------------------------------------------------------
-- 7. 扫码点单订单明细
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_scan_order_item` (
  `item_id`        BIGINT         NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id`       BIGINT         NOT NULL                COMMENT '订单ID',
  `product_id`     BIGINT         NOT NULL                COMMENT '商品ID',
  `product_name`   VARCHAR(200)   NOT NULL                COMMENT '商品名称',
  `product_image`  VARCHAR(500)   DEFAULT NULL            COMMENT '商品图片(快照)',
  `spec`           VARCHAR(500)   DEFAULT NULL            COMMENT '规格字符串(如: 热/正常糖/中杯)',
  `price`          DECIMAL(10, 2) NOT NULL                COMMENT '下单时单价',
  `quantity`       INT            NOT NULL DEFAULT 1      COMMENT '数量',
  `total_price`    DECIMAL(10, 2) NOT NULL                COMMENT '小计金额',
  `create_time`    DATETIME       DEFAULT NULL            COMMENT '创建时间',
  PRIMARY KEY (`item_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫码点单-订单明细';

SET FOREIGN_KEY_CHECKS = 1;

/*
 咖啡扫码点单 购物车表
 -----------------------------------------------------------------------------
 执行前提:
   请在扫码点单主建表脚本之后执行(依赖 t_scan_product 作为商品来源)。
 设计原则:
   1. 扫码点单购物车与商城购物车物理隔离,不改动原有 t_cart 表。
   2. 扫码点单仅服务堂食/外带场景,带 shop_id 与 table_no,便于按桌台清理。
   3. product_id 指向 t_scan_product.product_id,但不设硬外键,保持与其它 coffee_* 表风格一致。
   4. 本脚本可重复执行:已有购物车表和数据不会被删除。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `t_scan_cart` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `user_id`       BIGINT        DEFAULT NULL            COMMENT '用户ID(登录后写入,匿名下单可为空)',
  `openid`        VARCHAR(100)  DEFAULT NULL            COMMENT '微信openid(未绑定userId时的用户标识)',
  `shop_id`       BIGINT        DEFAULT 1               COMMENT '门店ID(对应 t_scan_table_qrcode.shop_id)',
  `table_no`      VARCHAR(50)   DEFAULT NULL            COMMENT '桌号(对应 t_scan_table_qrcode.table_no)',
  `product_id`    BIGINT        NOT NULL                COMMENT '扫码点单商品ID(对应 t_scan_product.product_id)',
  `product_name`  VARCHAR(200)  NOT NULL                COMMENT '商品名称(加购时快照,避免商品改名后错乱)',
  `product_image` VARCHAR(500)  DEFAULT NULL            COMMENT '商品主图(加购时快照)',
  `price`         DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '单价(已包含规格加价后的最终单价)',
  `quantity`      INT           NOT NULL DEFAULT 1      COMMENT '数量',
  `spec_text`     VARCHAR(500)  DEFAULT NULL            COMMENT '规格描述文本(如: 热/正常糖/中杯)',
  `spec_json`     TEXT                                  COMMENT '规格详情JSON(结构化存储规格组与选项ID,便于提交订单时还原)',
  `selected`      TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否勾选结算(0-未勾选,1-已勾选)',
  `status`        TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '状态(0-失效,如商品下架;1-有效)',
  `create_time`   DATETIME      DEFAULT NULL            COMMENT '创建时间',
  `update_time`   DATETIME      DEFAULT NULL            COMMENT '更新时间',
  `del_flag`      TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '删除标志(0-正常,1-已删除,软删除用)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`    (`user_id`),
  KEY `idx_openid`     (`openid`),
  KEY `idx_shop_id`    (`shop_id`),
  KEY `idx_table_no`   (`table_no`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫码点单-购物车(与商城 t_cart 物理隔离)';

SET FOREIGN_KEY_CHECKS = 1;

-- 购物车表不预置数据,由小程序扫码点单流程运行时写入。
/*
 咖啡商城后台菜单

 请在 ry_20210924.sql 之后执行。
 “经营看板”由 src/main/resources/templates/index.html 固定渲染，
 所以本脚本只维护 sys_menu 中的菜单记录。
 若依原始系统菜单会保留，以兼容后台权限和路由；
 但最终只显示当前侧边栏需要的菜单。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 平台设置：当前后台侧边栏显示为“平台设置”。
UPDATE `sys_menu`
SET `menu_name` = '平台设置',
    `order_num` = 1,
    `visible` = '0',
    `icon` = 'fa fa-gear',
    `remark` = '平台设置目录'
WHERE `menu_id` = 1;

-- 显示平台设置中保留的若依基础菜单。
-- 咖啡商城菜单固定使用 2000-2099 的菜单 ID，已存在的菜单不覆盖。
UPDATE `sys_menu`
SET `visible` = '0'
WHERE `menu_id` IN (100, 101, 102, 105, 106);

-- 隐藏当前侧边栏不使用的若依原始菜单。
UPDATE `sys_menu`
SET `visible` = '1'
WHERE `menu_id` IN (
  2, 3, 4,
  103, 104, 107, 108,
  109, 110, 111, 112, 113,
  114, 115, 116,
  500, 501
);

-- 固定 ID 菜单仅在不存在时插入，保留已有菜单和角色授权。
INSERT IGNORE INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `url`, `target`, `menu_type`, `visible`, `is_refresh`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(2000, '咖啡商城', 0, 2, '#', '', 'M', '0', '1', '', 'fa fa-coffee', 'admin', NOW(), '', NULL, '咖啡商城目录'),

(2001, '商品分类', 2000, 1, 'coffee/category', '', 'C', '0', '1', 'coffee:category:view', 'tree', 'admin', NOW(), '', NULL, '商品分类菜单'),
(2002, '商品分类查询', 2001, 1, '#', '', 'F', '0', '1', 'coffee:category:list', '#', 'admin', NOW(), '', NULL, ''),
(2003, '商品分类新增', 2001, 2, '#', '', 'F', '0', '1', 'coffee:category:add', '#', 'admin', NOW(), '', NULL, ''),
(2004, '商品分类修改', 2001, 3, '#', '', 'F', '0', '1', 'coffee:category:edit', '#', 'admin', NOW(), '', NULL, ''),
(2005, '商品分类删除', 2001, 4, '#', '', 'F', '0', '1', 'coffee:category:remove', '#', 'admin', NOW(), '', NULL, ''),
(2006, '商品分类导出', 2001, 5, '#', '', 'F', '0', '1', 'coffee:category:export', '#', 'admin', NOW(), '', NULL, ''),

(2010, '商品管理', 2000, 2, 'coffee/product', '', 'C', '0', '1', 'coffee:product:view', 'goods', 'admin', NOW(), '', NULL, '商品管理菜单'),
(2011, '商品查询', 2010, 1, '#', '', 'F', '0', '1', 'coffee:product:list', '#', 'admin', NOW(), '', NULL, ''),
(2012, '商品新增', 2010, 2, '#', '', 'F', '0', '1', 'coffee:product:add', '#', 'admin', NOW(), '', NULL, ''),
(2013, '商品修改', 2010, 3, '#', '', 'F', '0', '1', 'coffee:product:edit', '#', 'admin', NOW(), '', NULL, ''),
(2014, '商品删除', 2010, 4, '#', '', 'F', '0', '1', 'coffee:product:remove', '#', 'admin', NOW(), '', NULL, ''),
(2015, '商品导出', 2010, 5, '#', '', 'F', '0', '1', 'coffee:product:export', '#', 'admin', NOW(), '', NULL, ''),

(2020, '订单管理', 2000, 3, 'coffee/order', '', 'C', '0', '1', 'coffee:order:view', 'form', 'admin', NOW(), '', NULL, '订单管理菜单'),
(2021, '订单查询', 2020, 1, '#', '', 'F', '0', '1', 'coffee:order:list', '#', 'admin', NOW(), '', NULL, ''),
(2022, '订单新增', 2020, 2, '#', '', 'F', '0', '1', 'coffee:order:add', '#', 'admin', NOW(), '', NULL, ''),
(2023, '订单修改', 2020, 3, '#', '', 'F', '0', '1', 'coffee:order:edit', '#', 'admin', NOW(), '', NULL, ''),
(2024, '订单删除', 2020, 4, '#', '', 'F', '0', '1', 'coffee:order:remove', '#', 'admin', NOW(), '', NULL, ''),
(2025, '订单导出', 2020, 5, '#', '', 'F', '0', '1', 'coffee:order:export', '#', 'admin', NOW(), '', NULL, ''),

(2030, '收货地址', 2000, 4, 'coffee/address', '', 'C', '0', '1', 'coffee:address:view', 'guide', 'admin', NOW(), '', NULL, '收货地址菜单'),
(2031, '地址查询', 2030, 1, '#', '', 'F', '0', '1', 'coffee:address:list', '#', 'admin', NOW(), '', NULL, ''),
(2032, '地址新增', 2030, 2, '#', '', 'F', '0', '1', 'coffee:address:add', '#', 'admin', NOW(), '', NULL, ''),
(2033, '地址修改', 2030, 3, '#', '', 'F', '0', '1', 'coffee:address:edit', '#', 'admin', NOW(), '', NULL, ''),
(2034, '地址删除', 2030, 4, '#', '', 'F', '0', '1', 'coffee:address:remove', '#', 'admin', NOW(), '', NULL, ''),
(2035, '地址导出', 2030, 5, '#', '', 'F', '0', '1', 'coffee:address:export', '#', 'admin', NOW(), '', NULL, ''),

(2046, '微信用户', 2000, 5, 'coffee/wxuser', '', 'C', '0', '1', 'coffee:wxuser:view', 'fa fa-user', 'admin', NOW(), '', NULL, '微信用户菜单'),
(2048, '微信用户查询', 2046, 1, '#', '', 'F', '0', '1', 'coffee:wxuser:query', '#', 'admin', NOW(), '', NULL, ''),
(2049, '微信用户新增', 2046, 2, '#', '', 'F', '0', '1', 'coffee:wxuser:add', '#', 'admin', NOW(), '', NULL, ''),
(2050, '微信用户修改', 2046, 3, '#', '', 'F', '0', '1', 'coffee:wxuser:edit', '#', 'admin', NOW(), '', NULL, ''),
(2051, '微信用户删除', 2046, 4, '#', '', 'F', '0', '1', 'coffee:wxuser:remove', '#', 'admin', NOW(), '', NULL, ''),
(2052, '微信用户导出', 2046, 5, '#', '', 'F', '0', '1', 'coffee:wxuser:export', '#', 'admin', NOW(), '', NULL, ''),

(2047, '轮播图管理', 2000, 6, 'coffee/banner', '', 'C', '0', '1', 'coffee:banner:view', 'fa fa-image', 'admin', NOW(), '', NULL, '轮播图管理菜单'),
(2053, '轮播图查询', 2047, 1, '#', '', 'F', '0', '1', 'coffee:banner:query', '#', 'admin', NOW(), '', NULL, ''),
(2054, '轮播图新增', 2047, 2, '#', '', 'F', '0', '1', 'coffee:banner:add', '#', 'admin', NOW(), '', NULL, ''),
(2055, '轮播图修改', 2047, 3, '#', '', 'F', '0', '1', 'coffee:banner:edit', '#', 'admin', NOW(), '', NULL, ''),
(2056, '轮播图删除', 2047, 4, '#', '', 'F', '0', '1', 'coffee:banner:remove', '#', 'admin', NOW(), '', NULL, ''),
(2057, '轮播图导出', 2047, 5, '#', '', 'F', '0', '1', 'coffee:banner:export', '#', 'admin', NOW(), '', NULL, ''),

(2040, '营销活动', 2000, 7, 'coffee/activity', '', 'C', '0', '1', 'coffee:activity:view', 'star', 'admin', NOW(), '', NULL, '营销活动菜单'),
(2041, '活动查询', 2040, 1, '#', '', 'F', '0', '1', 'coffee:activity:list', '#', 'admin', NOW(), '', NULL, ''),
(2042, '活动新增', 2040, 2, '#', '', 'F', '0', '1', 'coffee:activity:add', '#', 'admin', NOW(), '', NULL, ''),
(2043, '活动修改', 2040, 3, '#', '', 'F', '0', '1', 'coffee:activity:edit', '#', 'admin', NOW(), '', NULL, ''),
(2044, '活动删除', 2040, 4, '#', '', 'F', '0', '1', 'coffee:activity:remove', '#', 'admin', NOW(), '', NULL, ''),
(2045, '活动导出', 2040, 5, '#', '', 'F', '0', '1', 'coffee:activity:export', '#', 'admin', NOW(), '', NULL, ''),

(2060, '会员管理', 2000, 8, 'coffee/member', '', 'C', '0', '1', 'coffee:member:view', '#', 'admin', NOW(), '', NULL, '会员管理菜单'),
(2061, '会员查询', 2060, 1, '#', '', 'F', '0', '1', 'coffee:member:list', '#', 'admin', NOW(), '', NULL, ''),
(2062, '会员修改', 2060, 2, '#', '', 'F', '0', '1', 'coffee:member:edit', '#', 'admin', NOW(), '', NULL, ''),

(2065, '充值模板', 2000, 9, 'coffee/rechargeTemplate', '', 'C', '0', '1', 'coffee:rechargeTemplate:view', '#', 'admin', NOW(), '', NULL, '充值模板菜单'),
(2066, '模板查询', 2065, 1, '#', '', 'F', '0', '1', 'coffee:rechargeTemplate:list', '#', 'admin', NOW(), '', NULL, ''),
(2067, '模板新增', 2065, 2, '#', '', 'F', '0', '1', 'coffee:rechargeTemplate:add', '#', 'admin', NOW(), '', NULL, ''),
(2068, '模板修改', 2065, 3, '#', '', 'F', '0', '1', 'coffee:rechargeTemplate:edit', '#', 'admin', NOW(), '', NULL, ''),
(2069, '模板删除', 2065, 4, '#', '', 'F', '0', '1', 'coffee:rechargeTemplate:remove', '#', 'admin', NOW(), '', NULL, ''),

(2070, '充值记录', 2000, 10, 'coffee/rechargeRecord', '', 'C', '0', '1', 'coffee:rechargeRecord:view', '#', 'admin', NOW(), '', NULL, '充值记录菜单'),
(2071, '记录查询', 2070, 1, '#', '', 'F', '0', '1', 'coffee:rechargeRecord:list', '#', 'admin', NOW(), '', NULL, ''),

(2075, '余额流水', 2000, 11, 'coffee/walletLog', '', 'C', '0', '1', 'coffee:walletLog:view', '#', 'admin', NOW(), '', NULL, '余额流水菜单'),
(2076, '流水查询', 2075, 1, '#', '', 'F', '0', '1', 'coffee:walletLog:list', '#', 'admin', NOW(), '', NULL, ''),

(2080, '线下活动', 2000, 12, 'coffee/offlineActivity', '', 'C', '0', '1', 'coffee:offlineActivity:view', 'fa fa-calendar', 'admin', NOW(), '', NULL, '线下活动菜单'),
(2081, '活动查询', 2080, 1, '#', '', 'F', '0', '1', 'coffee:offlineActivity:list', '#', 'admin', NOW(), '', NULL, ''),
(2082, '活动新增', 2080, 2, '#', '', 'F', '0', '1', 'coffee:offlineActivity:add', '#', 'admin', NOW(), '', NULL, ''),
(2083, '活动修改', 2080, 3, '#', '', 'F', '0', '1', 'coffee:offlineActivity:edit', '#', 'admin', NOW(), '', NULL, ''),
(2084, '活动删除', 2080, 4, '#', '', 'F', '0', '1', 'coffee:offlineActivity:remove', '#', 'admin', NOW(), '', NULL, ''),

(2090, '商家装修', 2000, 13, '/coffee/decorator/workbench', '', 'C', '0', '1', 'coffee:decorator:view', 'fa fa-paint-brush', 'admin', NOW(), '', NULL, '商家小程序个性化装修入口'),

-- 支付日志菜单
-- 注意: 后台 IndexController 会过滤若依内置的“日志管理”,这里不能命名为“日志管理”。
(2200, '支付日志', 0, 3, '#', '', 'M', '0', '1', '', 'fa fa-file-text-o', 'admin', NOW(), '', NULL, '支付日志目录'),
(2201, '支付记录', 2200, 1, 'coffee/paymentLog', '', 'C', '0', '1', 'coffee:paymentLog:view', 'fa fa-credit-card', 'admin', NOW(), '', NULL, '支付记录菜单'),
(2202, '日志查询', 2201, 1, '#', '', 'F', '0', '1', 'coffee:paymentLog:list', '#', 'admin', NOW(), '', NULL, ''),

-- 扫码点单菜单
(2100, '扫码点单', 0, 4, '#', '', 'M', '0', '1', '', 'fa fa-qrcode', 'admin', NOW(), '', NULL, '扫码点单菜单'),

(2101, '点单分类', 2100, 1, 'coffee/scanCategory', '', 'C', '0', '1', 'coffee:scanCategory:view', 'fa fa-list', 'admin', NOW(), '', NULL, '扫码点单分类菜单'),
(2102, '分类查询', 2101, 1, '#', '', 'F', '0', '1', 'coffee:scanCategory:list', '#', 'admin', NOW(), '', NULL, ''),
(2103, '分类新增', 2101, 2, '#', '', 'F', '0', '1', 'coffee:scanCategory:add', '#', 'admin', NOW(), '', NULL, ''),
(2104, '分类修改', 2101, 3, '#', '', 'F', '0', '1', 'coffee:scanCategory:edit', '#', 'admin', NOW(), '', NULL, ''),
(2105, '分类删除', 2101, 4, '#', '', 'F', '0', '1', 'coffee:scanCategory:remove', '#', 'admin', NOW(), '', NULL, ''),

(2110, '点单商品', 2100, 2, 'coffee/scanProduct', '', 'C', '0', '1', 'coffee:scanProduct:view', 'fa fa-coffee', 'admin', NOW(), '', NULL, '扫码点单商品菜单'),
(2111, '商品查询', 2110, 1, '#', '', 'F', '0', '1', 'coffee:scanProduct:list', '#', 'admin', NOW(), '', NULL, ''),
(2112, '商品新增', 2110, 2, '#', '', 'F', '0', '1', 'coffee:scanProduct:add', '#', 'admin', NOW(), '', NULL, ''),
(2113, '商品修改', 2110, 3, '#', '', 'F', '0', '1', 'coffee:scanProduct:edit', '#', 'admin', NOW(), '', NULL, ''),
(2114, '商品删除', 2110, 4, '#', '', 'F', '0', '1', 'coffee:scanProduct:remove', '#', 'admin', NOW(), '', NULL, ''),

(2120, '桌台二维码', 2100, 3, 'coffee/scanTable', '', 'C', '0', '1', 'coffee:scanTable:view', 'fa fa-table', 'admin', NOW(), '', NULL, '桌台二维码管理'),
(2121, '桌台查询', 2120, 1, '#', '', 'F', '0', '1', 'coffee:scanTable:list', '#', 'admin', NOW(), '', NULL, ''),
(2122, '桌台新增', 2120, 2, '#', '', 'F', '0', '1', 'coffee:scanTable:add', '#', 'admin', NOW(), '', NULL, ''),
(2123, '桌台修改', 2120, 3, '#', '', 'F', '0', '1', 'coffee:scanTable:edit', '#', 'admin', NOW(), '', NULL, ''),
(2124, '桌台删除', 2120, 4, '#', '', 'F', '0', '1', 'coffee:scanTable:remove', '#', 'admin', NOW(), '', NULL, ''),

(2130, '点单订单', 2100, 4, 'coffee/scanOrder', '', 'C', '0', '1', 'coffee:scanOrder:view', 'fa fa-shopping-cart', 'admin', NOW(), '', NULL, '扫码点单订单菜单'),
(2131, '订单查询', 2130, 1, '#', '', 'F', '0', '1', 'coffee:scanOrder:list', '#', 'admin', NOW(), '', NULL, ''),
(2132, '订单详情', 2130, 2, '#', '', 'F', '0', '1', 'coffee:scanOrder:detail', '#', 'admin', NOW(), '', NULL, ''),
(2133, '订单操作', 2130, 3, '#', '', 'F', '0', '1', 'coffee:scanOrder:edit', '#', 'admin', NOW(), '', NULL, '');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id`
FROM `sys_menu`
WHERE (`menu_id` BETWEEN 2000 AND 2199)
   OR (`menu_id` BETWEEN 2200 AND 2299);

SET FOREIGN_KEY_CHECKS = 1;

-- 装修平台表、系统模板和背景插槽由独立迁移维护；从项目根目录执行本脚本时一并加载。
SOURCE sql/coffee_theme_decorator.sql;
SOURCE sql/coffee_theme_scope_v2.sql;
