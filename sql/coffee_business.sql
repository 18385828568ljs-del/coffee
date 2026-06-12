/*
 咖啡商城业务表结构
 请在若依基础脚本 ry_20210924.sql 和 quartz.sql 之后执行。
 本脚本只包含咖啡商城业务表结构。
 本脚本不包含商品、订单、轮播图、会员、钱包、充值模板等业务初始化数据。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `t_wallet_log`;
DROP TABLE IF EXISTS `t_recharge_record`;
DROP TABLE IF EXISTS `t_recharge_template`;
DROP TABLE IF EXISTS `t_wallet`;
DROP TABLE IF EXISTS `t_member`;
DROP TABLE IF EXISTS `t_marketing_activity_scope`;
DROP TABLE IF EXISTS `t_marketing_activity`;
DROP TABLE IF EXISTS `t_order_item`;
DROP TABLE IF EXISTS `t_order`;
DROP TABLE IF EXISTS `t_address`;
DROP TABLE IF EXISTS `t_cart`;
DROP TABLE IF EXISTS `t_product`;
DROP TABLE IF EXISTS `t_category`;
DROP TABLE IF EXISTS `t_banner`;
DROP TABLE IF EXISTS `t_wxuser`;

CREATE TABLE `t_category` (
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

CREATE TABLE `t_product` (
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

CREATE TABLE `t_cart` (
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

CREATE TABLE `t_address` (
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

CREATE TABLE `t_order` (
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
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间',
  `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
  `express_no` VARCHAR(64) DEFAULT NULL COMMENT '物流单号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

CREATE TABLE `t_order_item` (
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

CREATE TABLE `t_banner` (
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

CREATE TABLE `t_wxuser` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `nickname` VARCHAR(100) DEFAULT NULL COMMENT '微信名称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `openid` VARCHAR(100) DEFAULT NULL COMMENT '微信唯一标识符',
  `gender` INT DEFAULT NULL COMMENT '性别',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信用户';

CREATE TABLE `t_marketing_activity` (
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

CREATE TABLE `t_marketing_activity_scope` (
  `scope_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '范围明细ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `scope_type` TINYINT(1) NOT NULL COMMENT '范围类型(1-分类,2-商品)',
  `scope_target_id` BIGINT NOT NULL COMMENT '范围对象ID',
  PRIMARY KEY (`scope_id`),
  UNIQUE KEY `uk_activity_scope` (`activity_id`, `scope_type`, `scope_target_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_scope_target` (`scope_type`, `scope_target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动适用范围';

CREATE TABLE `t_member` (
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

CREATE TABLE `t_wallet` (
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

CREATE TABLE `t_recharge_record` (
  `record_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `recharge_no` VARCHAR(64) NOT NULL COMMENT '充值单号',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实际支付金额',
  `gift_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '赠送金额',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '到账总金额',
  `template_id` BIGINT DEFAULT NULL COMMENT '充值模板ID',
  `pay_type` VARCHAR(16) DEFAULT 'wechat' COMMENT '支付方式',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0=待支付,1=成功,2=失败)',
  `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '微信支付交易号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_recharge_no` (`recharge_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值记录';

CREATE TABLE `t_wallet_log` (
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

CREATE TABLE `t_recharge_template` (
  `template_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '充值金额',
  `gift_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '赠送金额',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '到账总金额',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0=禁用,1=启用)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值模板';

CREATE TABLE `t_offline_activity` (
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

CREATE TABLE `t_offline_activity_signup` (
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
INSERT INTO `sys_job` VALUES (100, '商城订单超时取消',  'DEFAULT', 'orderTimeoutTask.cancelTimeoutOrders',     '0 0/5 * * * ?', '3', '1', '0', 'admin', sysdate(), '', NULL, '商城订单 30 分钟未支付自动取消并回滚库存');
INSERT INTO `sys_job` VALUES (101, '扫码点单订单超时取消','DEFAULT', 'scanOrderTimeoutTask.cancelTimeoutOrders', '0 0/5 * * * ?', '3', '1', '0', 'admin', sysdate(), '', NULL, '扫码点单 30 分钟未支付自动取消');
