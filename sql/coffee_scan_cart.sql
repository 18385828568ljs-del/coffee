/*
 咖啡扫码点单 购物车表
 -----------------------------------------------------------------------------
 执行前提:
   请在扫码点单主建表脚本之后执行(依赖 t_scan_product 作为商品来源)。
 设计原则:
   1. 扫码点单购物车与商城购物车物理隔离,不改动原有 t_cart 表。
   2. 扫码点单仅服务堂食/外带场景,带 shop_id 与 table_no,便于按桌台清理。
   3. product_id 指向 t_scan_product.product_id,但不设硬外键,保持与其它 coffee_* 表风格一致。
   4. 本脚本可重复执行:会先 DROP 再 CREATE,生产环境重跑将清空购物车数据,请谨慎。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `t_scan_cart`;

CREATE TABLE `t_scan_cart` (
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

-- 购物车表不初始化测试数据,由小程序扫码点单流程运行时写入。
