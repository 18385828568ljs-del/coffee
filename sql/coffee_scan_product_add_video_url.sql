-- =============================================================================
-- 扫码点单-商品 增加"商品讲解视频"字段
-- 已有库执行此脚本，将 video_url 字段补到 t_scan_product 表
-- 全新部署不需要执行此脚本，coffee_scan_order.sql 中的 CREATE TABLE 已包含该字段
-- =============================================================================

ALTER TABLE `t_scan_product`
  ADD COLUMN `video_url` VARCHAR(500) DEFAULT NULL COMMENT '商品讲解视频地址' AFTER `image_url`;
