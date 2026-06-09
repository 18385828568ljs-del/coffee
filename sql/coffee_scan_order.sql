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

DROP TABLE IF EXISTS `t_scan_order_item`;
DROP TABLE IF EXISTS `t_scan_order`;
DROP TABLE IF EXISTS `t_scan_table_qrcode`;
DROP TABLE IF EXISTS `t_scan_product_spec_option`;
DROP TABLE IF EXISTS `t_scan_product_spec`;
DROP TABLE IF EXISTS `t_scan_product`;
DROP TABLE IF EXISTS `t_scan_category`;

-- -----------------------------------------------------------------------------
-- 1. 点单分类
-- -----------------------------------------------------------------------------
CREATE TABLE `t_scan_category` (
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
CREATE TABLE `t_scan_product` (
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
CREATE TABLE `t_scan_product_spec` (
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
CREATE TABLE `t_scan_product_spec_option` (
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
CREATE TABLE `t_scan_table_qrcode` (
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
CREATE TABLE `t_scan_order` (
  `order_id`         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`         VARCHAR(64)    NOT NULL                COMMENT '订单号',
  `user_id`          BIGINT         NOT NULL                COMMENT '下单用户ID',
  `shop_id`          BIGINT         DEFAULT 1               COMMENT '门店ID',
  `shop_name`        VARCHAR(200)   DEFAULT NULL            COMMENT '门店名称',
  `table_no`         VARCHAR(50)    DEFAULT NULL            COMMENT '桌号',
  `scene`            VARCHAR(50)    DEFAULT 'dine_in'       COMMENT '场景',
  `total_amount`     DECIMAL(10, 2) NOT NULL                COMMENT '订单总金额(原价)',
  `pay_amount`       DECIMAL(10, 2) NOT NULL                COMMENT '实付金额',
  `discount_amount`  DECIMAL(10, 2) NOT NULL DEFAULT 0.00   COMMENT '活动优惠金额',
  `member_discount`  DECIMAL(10, 2) NOT NULL DEFAULT 0.00   COMMENT '会员折扣金额',
  `activity_summary` VARCHAR(500)   DEFAULT NULL            COMMENT '命中的活动文案,逗号分隔',
  `status`           TINYINT        NOT NULL DEFAULT 0      COMMENT '状态(0-待支付,1-已支付/制作中,2-已完成,3-已取消)',
  `pay_type`         VARCHAR(20)    DEFAULT NULL            COMMENT '支付方式',
  `pay_time`         DATETIME       DEFAULT NULL            COMMENT '支付时间',
  `finish_time`      DATETIME       DEFAULT NULL            COMMENT '完成时间',
  `cancel_time`      DATETIME       DEFAULT NULL            COMMENT '取消时间',
  `remark`           VARCHAR(500)   DEFAULT NULL            COMMENT '备注',
  `create_by`        VARCHAR(64)    DEFAULT ''              COMMENT '创建者',
  `create_time`      DATETIME       DEFAULT NULL            COMMENT '创建时间',
  `update_by`        VARCHAR(64)    DEFAULT ''              COMMENT '更新者',
  `update_time`      DATETIME       DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_shop_table` (`shop_id`, `table_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫码点单-订单';

-- -----------------------------------------------------------------------------
-- 7. 扫码点单订单明细
-- -----------------------------------------------------------------------------
CREATE TABLE `t_scan_order_item` (
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

-- =============================================================================
-- 测试数据
-- =============================================================================

-- 分类
INSERT INTO `t_scan_category`(`category_id`,`category_name`,`sort_order`,`status`,`create_by`,`create_time`) VALUES
(1, '招牌奶茶', 1, 1, 'admin', NOW()),
(2, '精品咖啡', 2, 1, 'admin', NOW()),
(3, '水果茶',   3, 1, 'admin', NOW()),
(4, '小食甜品', 4, 1, 'admin', NOW());

-- 商品
INSERT INTO `t_scan_product`(`product_id`,`category_id`,`product_name`,`sub_title`,`image_url`,`price`,`month_sales`,`tag`,`status`,`sort_order`,`create_by`,`create_time`) VALUES
(101, 1, '招牌奶茶',      '经典红茶 + 鲜奶',         '', 15.00, 520, '招牌', 1, 1, 'admin', NOW()),
(102, 1, '珍珠奶茶',      'Q弹珍珠 + 黑糖',          '', 16.00, 430, '热销', 1, 2, 'admin', NOW()),
(103, 1, '厚乳波波',      '厚乳 + 手工波波',         '', 18.00, 260, '新品', 1, 3, 'admin', NOW()),
(201, 2, '美式咖啡',      '精选阿拉比卡豆',          '', 12.00, 380, NULL,   1, 1, 'admin', NOW()),
(202, 2, '拿铁咖啡',      '丝滑拿铁经典款',          '', 18.00, 610, '热销', 1, 2, 'admin', NOW()),
(203, 2, '燕麦拿铁',      '植物奶低脂选择',          '', 22.00, 180, '新品', 1, 3, 'admin', NOW()),
(301, 3, '满杯百香果',    '百香果果茶',              '', 18.00, 290, NULL,   1, 1, 'admin', NOW()),
(302, 3, '多肉葡萄',      '整颗葡萄鲜果',            '', 22.00, 340, '招牌', 1, 2, 'admin', NOW()),
(401, 4, '肉松小贝',      '海苔肉松面包',            '', 8.00,  150, NULL,   1, 1, 'admin', NOW()),
(402, 4, '提拉米苏',      '意式经典甜品',            '', 25.00, 95,  NULL,   1, 2, 'admin', NOW());

-- 规格组(以招牌奶茶/拿铁为例)
INSERT INTO `t_scan_product_spec`(`spec_id`,`product_id`,`spec_name`,`spec_type`,`required`,`sort_order`,`create_time`) VALUES
(1, 101, '温度', 'single', 1, 1, NOW()),
(2, 101, '糖度', 'single', 1, 2, NOW()),
(3, 101, '杯型', 'single', 1, 3, NOW()),
(4, 202, '温度', 'single', 1, 1, NOW()),
(5, 202, '杯型', 'single', 1, 2, NOW()),
(6, 202, '加料', 'multiple', 0, 3, NOW());

-- 规格选项
INSERT INTO `t_scan_product_spec_option`(`spec_id`,`product_id`,`option_name`,`extra_price`,`is_default`,`sort_order`,`create_time`) VALUES
(1, 101, '热',     0.00, 1, 1, NOW()),
(1, 101, '温',     0.00, 0, 2, NOW()),
(1, 101, '冰',     0.00, 0, 3, NOW()),
(2, 101, '正常糖', 0.00, 1, 1, NOW()),
(2, 101, '少糖',   0.00, 0, 2, NOW()),
(2, 101, '半糖',   0.00, 0, 3, NOW()),
(2, 101, '无糖',   0.00, 0, 4, NOW()),
(3, 101, '中杯',   0.00, 1, 1, NOW()),
(3, 101, '大杯',   3.00, 0, 2, NOW()),
(4, 202, '热',     0.00, 1, 1, NOW()),
(4, 202, '冰',     0.00, 0, 2, NOW()),
(5, 202, '中杯',   0.00, 1, 1, NOW()),
(5, 202, '大杯',   4.00, 0, 2, NOW()),
(6, 202, '加浓缩', 5.00, 0, 1, NOW()),
(6, 202, '加奶盖', 4.00, 0, 2, NOW()),
(6, 202, '加燕麦奶', 3.00, 0, 3, NOW());

-- 桌台二维码(测试)
INSERT INTO `t_scan_table_qrcode`(`table_id`,`shop_id`,`shop_name`,`table_no`,`scene`,`status`,`create_by`,`create_time`) VALUES
(1, 1, '咖啡旗舰店', 'A01', 'dine_in', 1, 'admin', NOW()),
(2, 1, '咖啡旗舰店', 'A02', 'dine_in', 1, 'admin', NOW()),
(3, 1, '咖啡旗舰店', 'B01', 'dine_in', 1, 'admin', NOW());
