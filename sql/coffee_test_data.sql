-- =============================================================================
-- 咖啡项目完整测试数据
-- -----------------------------------------------------------------------------
-- 用途:
-- 1. 仅用于本地开发、演示和测试环境，生产环境请勿执行。
-- 2. 执行前请先执行 coffee_all_business.sql 创建业务表结构。
-- 3. 本脚本使用固定的测试 ID；重复执行会先清理本脚本的数据再重建。
-- 4. 所有图片字段均为空，由项目使用者自行补充。
--
-- 覆盖范围:
-- - 商城: t_category / t_product / t_order / t_order_item / t_cart
-- - 扫码点单: t_scan_category / t_scan_product / 规格 / 桌台 / 订单 / 购物车
-- - 用户画像: t_wxuser / t_user_behavior_event / t_user_profile
-- - 用户 9001=EMPTY, 9002=LEARNING, 9003=READY(有订单),
--   用户 9004=READY(仅靠行为证据)
-- =============================================================================

SET NAMES utf8mb4;
START TRANSACTION;

-- 清理本脚本之前生成的数据，保证可重复执行。
DELETE FROM `t_user_behavior_event` WHERE `user_id` IN (9001, 9002, 9003, 9004);
DELETE FROM `t_user_profile` WHERE `user_id` IN (9001, 9002, 9003, 9004);
DELETE FROM `t_wxuser` WHERE `id` IN (9001, 9002, 9003, 9004);
DELETE FROM `t_scan_cart` WHERE `id` IN (7001, 7002);
DELETE FROM `t_cart` WHERE `cart_id` IN (6001, 6002);

DELETE FROM `t_order_item` WHERE `order_id` IN (3001, 3002);
DELETE FROM `t_order` WHERE `order_id` IN (3001, 3002);
DELETE FROM `t_scan_order_item` WHERE `order_id` IN (4001, 4002);
DELETE FROM `t_scan_order` WHERE `order_id` IN (4001, 4002);

DELETE FROM `t_product_image` WHERE `product_id` IN (1001, 1002, 1003, 1004, 1005, 1006);
DELETE FROM `t_product` WHERE `product_id` IN (1001, 1002, 1003, 1004, 1005, 1006);
DELETE FROM `t_category` WHERE `category_id` IN (11, 12);

DELETE FROM `t_scan_product_spec_option` WHERE `product_id` IN
    (101, 102, 103, 201, 202, 203, 301, 302, 401, 402);
DELETE FROM `t_scan_product_spec` WHERE `product_id` IN
    (101, 102, 103, 201, 202, 203, 301, 302, 401, 402);
DELETE FROM `t_scan_product` WHERE `product_id` IN
    (101, 102, 103, 201, 202, 203, 301, 302, 401, 402);
DELETE FROM `t_scan_category` WHERE `category_id` IN (1, 2, 3, 4);
DELETE FROM `t_scan_table_qrcode` WHERE `table_id` IN (1, 2, 3);

-- -----------------------------------------------------------------------------
-- 1. 微信用户
-- -----------------------------------------------------------------------------
INSERT INTO `t_wxuser` (`id`, `openid`, `nickname`, `avatar`, `phone`, `gender`, `create_time`)
VALUES
    (9001, 'coffee-test-9001', '空画像用户', NULL, '13800009001', 1, NOW()),
    (9002, 'coffee-test-9002', '学习中用户', NULL, '13800009002', 2, NOW()),
    (9003, 'coffee-test-9003', '订单用户',   NULL, '13800009003', 1, NOW()),
    (9004, 'coffee-test-9004', '行为用户',   NULL, '13800009004', 0, NOW());

-- -----------------------------------------------------------------------------
-- 2. 商城分类与商品（图片留空）
-- -----------------------------------------------------------------------------
INSERT INTO `t_category` (`category_id`, `category_name`, `sort_order`, `create_by`, `create_time`)
VALUES
    (11, '咖啡豆', 1, 'test-data', NOW()),
    (12, '咖啡饮品与器具', 2, 'test-data', NOW());

INSERT INTO `t_product`
    (`product_id`, `category_id`, `product_name`, `origin`, `processing_method`,
     `roast_level`, `flavor_notes`, `description`, `price`, `stock`, `image_url`,
     `status`, `create_by`, `create_time`)
VALUES
    (1001, 11, '耶加雪菲咖啡豆', '埃塞俄比亚', '水洗', '浅烘焙', '茉莉、柑橘、红茶', '明亮清爽的花香型咖啡豆', 68.00, 100, NULL, 1, 'test-data', NOW()),
    (1002, 11, '哥伦比亚咖啡豆', '哥伦比亚', '双重发酵水洗', '中烘焙', '坚果、焦糖、可可', '均衡醇厚，适合日常饮用', 72.00, 100, NULL, 1, 'test-data', NOW()),
    (1003, 11, '手冲咖啡礼盒', '多产地拼配', '水洗', '中浅烘焙', '花香、果酸、焦糖', '适合送礼的手冲咖啡组合', 128.00, 50, NULL, 1, 'test-data', NOW()),
    (1004, 12, '挂耳咖啡', '云南', '水洗', '中烘焙', '坚果、黑巧克力', '便携式单杯咖啡', 39.00, 200, NULL, 1, 'test-data', NOW()),
    (1005, 12, '冷萃咖啡液', '云南', '日晒', '深烘焙', '可可、焦糖', '冷藏即饮的浓缩咖啡液', 49.00, 120, NULL, 1, 'test-data', NOW()),
    (1006, 12, '咖啡滤纸', NULL, NULL, NULL, NULL, '锥形滤杯通用滤纸', 19.00, 300, NULL, 1, 'test-data', NOW());

-- -----------------------------------------------------------------------------
-- 3. 商城订单、订单明细和购物车
-- -----------------------------------------------------------------------------
INSERT INTO `t_order`
    (`order_id`, `order_no`, `user_id`, `total_amount`, `pay_amount`, `discount_amount`,
     `freight_amount`, `receiver_name`, `receiver_phone`, `receiver_address`, `status`,
     `pay_type`, `create_time`, `update_time`, `pay_time`, `ship_time`, `finish_time`,
     `refund_status`, `remark`)
VALUES
    (3001, 'COFFEE-MALL-3001', 9003, 140.00, 130.00, 10.00, 0.00,
     '订单用户', '13800009003', '上海市咖啡路 1 号', 3, 'wechat',
     NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 3 DAY,
     NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY, 0, '有效订单，参与画像计算'),
    (3002, 'COFFEE-MALL-3002', 9002, 72.00, 0.00, 0.00, 0.00,
     '学习中用户', '13800009002', '上海市咖啡路 2 号', 4, 'wechat',
     NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NULL, NULL, NULL, 0, '已取消，不参与画像计算');

INSERT INTO `t_order_item`
    (`item_id`, `order_id`, `product_id`, `product_name`, `product_image`, `price`, `spec`, `quantity`, `total_price`)
VALUES
    (3101, 3001, 1001, '耶加雪菲咖啡豆', NULL, 68.00, '250g', 1, 68.00),
    (3102, 3001, 1002, '哥伦比亚咖啡豆', NULL, 72.00, '250g', 1, 72.00),
    (3103, 3002, 1002, '哥伦比亚咖啡豆', NULL, 72.00, '250g', 1, 72.00);

INSERT INTO `t_cart`
    (`cart_id`, `user_id`, `product_id`, `spec`, `quantity`, `create_time`, `update_time`)
VALUES
    (6001, 9003, 1003, '标准礼盒', 1, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR),
    (6002, 9004, 1005, NULL, 1, NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 1 HOUR);

-- -----------------------------------------------------------------------------
-- 4. 扫码点单分类、商品、规格和桌台（图片留空）
-- -----------------------------------------------------------------------------
INSERT INTO `t_scan_category`
    (`category_id`, `category_name`, `sort_order`, `status`, `create_by`, `create_time`)
VALUES
    (1, '经典咖啡', 1, 1, 'test-data', NOW()),
    (2, '奶咖',     2, 1, 'test-data', NOW()),
    (3, '无咖啡因饮品', 3, 1, 'test-data', NOW()),
    (4, '小食甜品', 4, 1, 'test-data', NOW());

INSERT INTO `t_scan_product`
    (`product_id`, `category_id`, `product_type`, `product_name`, `sub_title`, `description`, `image_url`, `price`,
     `month_sales`, `tag`, `status`, `sort_order`, `create_by`, `create_time`)
VALUES
    (101, 1, 'COFFEE', '美式咖啡', '精选阿拉比卡豆，风味干净', '双份浓缩加水，清爽且保留咖啡香气', NULL, 12.00, 380, NULL, 1, 1, 'test-data', NOW()),
    (102, 1, 'COFFEE', '手冲咖啡', '当日精选单品豆', '按当日豆单制作，突出产区风味', NULL, 22.00, 220, '新品', 1, 2, 'test-data', NOW()),
    (103, 1, 'COFFEE', '冷萃咖啡', '低温慢萃，口感顺滑', '长时间低温萃取，苦感柔和', NULL, 20.00, 260, '招牌', 1, 3, 'test-data', NOW()),
    (201, 2, 'COFFEE', '拿铁咖啡', '丝滑奶泡与浓郁咖啡融合', '浓缩咖啡搭配蒸汽牛奶', NULL, 18.00, 610, '热销', 1, 1, 'test-data', NOW()),
    (202, 2, 'COFFEE', '燕麦拿铁', '植物奶低脂选择', '浓缩咖啡搭配燕麦奶', NULL, 22.00, 300, '新品', 1, 2, 'test-data', NOW()),
    (203, 2, 'COFFEE', '摩卡咖啡', '咖啡、巧克力与奶泡', '浓缩咖啡加入巧克力和牛奶', NULL, 24.00, 190, NULL, 1, 3, 'test-data', NOW()),
    (301, 3, 'DRINK', '热可可', '浓郁可可风味', '可可与热牛奶调制的无咖啡饮品', NULL, 16.00, 170, NULL, 1, 1, 'test-data', NOW()),
    (302, 3, 'DRINK', '柠檬气泡水', '清爽无咖啡因饮品', '柠檬果汁搭配气泡水', NULL, 15.00, 140, NULL, 1, 2, 'test-data', NOW()),
    (401, 4, 'FOOD', '肉桂卷', '现烤肉桂风味点心', '适合搭配咖啡的现烤甜点', NULL, 14.00, 150, NULL, 1, 1, 'test-data', NOW()),
    (402, 4, 'FOOD', '提拉米苏', '意式经典甜品', '马斯卡彭与咖啡风味甜品', NULL, 25.00, 95, NULL, 1, 2, 'test-data', NOW());

INSERT INTO `t_scan_product_spec`
    (`spec_id`, `product_id`, `spec_name`, `spec_type`, `required`, `sort_order`, `create_time`)
VALUES
    (1, 101, '温度', 'single', 1, 1, NOW()),
    (2, 101, '杯型', 'single', 1, 2, NOW()),
    (3, 201, '温度', 'single', 1, 1, NOW()),
    (4, 201, '杯型', 'single', 1, 2, NOW()),
    (5, 201, '糖度', 'single', 1, 3, NOW()),
    (6, 202, '温度', 'single', 1, 1, NOW()),
    (7, 101, '糖度', 'single', 1, 3, NOW()),
    (8, 101, '咖啡豆', 'single', 1, 4, NOW()),
    (9, 101, '咖啡浓度', 'single', 1, 5, NOW()),
    (11, 201, '咖啡豆', 'single', 1, 5, NOW()),
    (12, 201, '咖啡浓度', 'single', 1, 6, NOW()),
    (13, 202, '杯型', 'single', 1, 2, NOW()),
    (14, 202, '糖度', 'single', 1, 3, NOW()),
    (15, 202, '咖啡豆', 'single', 1, 4, NOW()),
    (16, 202, '咖啡浓度', 'single', 1, 5, NOW()),
    (17, 301, '温度', 'single', 1, 1, NOW()),
    (18, 301, '杯型', 'single', 1, 2, NOW()),
    (19, 301, '糖度', 'single', 1, 3, NOW()),
    (20, 102, '温度', 'single', 1, 1, NOW()),
    (21, 102, '杯型', 'single', 1, 2, NOW()),
    (22, 102, '糖度', 'single', 1, 3, NOW()),
    (23, 102, '咖啡豆', 'single', 1, 4, NOW()),
    (24, 102, '咖啡浓度', 'single', 1, 5, NOW()),
    (25, 103, '温度', 'single', 1, 1, NOW()),
    (26, 103, '杯型', 'single', 1, 2, NOW()),
    (27, 103, '糖度', 'single', 1, 3, NOW()),
    (28, 103, '咖啡豆', 'single', 1, 4, NOW()),
    (29, 103, '咖啡浓度', 'single', 1, 5, NOW()),
    (30, 203, '温度', 'single', 1, 1, NOW()),
    (31, 203, '杯型', 'single', 1, 2, NOW()),
    (32, 203, '糖度', 'single', 1, 3, NOW()),
    (33, 203, '咖啡豆', 'single', 1, 4, NOW()),
    (34, 203, '咖啡浓度', 'single', 1, 5, NOW()),
    (35, 302, '温度', 'single', 1, 1, NOW()),
    (36, 302, '杯型', 'single', 1, 2, NOW()),
    (37, 302, '糖度', 'single', 1, 3, NOW());

INSERT INTO `t_scan_product_spec_option`
    (`option_id`, `spec_id`, `product_id`, `option_name`, `extra_price`, `is_default`, `sort_order`, `create_time`)
VALUES
    (1, 1, 101, '热', 0.00, 1, 1, NOW()),
    (2, 1, 101, '冰', 0.00, 0, 2, NOW()),
    (3, 2, 101, '中杯', 0.00, 1, 1, NOW()),
    (4, 2, 101, '大杯', 3.00, 0, 2, NOW()),
    (5, 3, 201, '热', 0.00, 1, 1, NOW()),
    (6, 3, 201, '冰', 0.00, 0, 2, NOW()),
    (7, 4, 201, '中杯', 0.00, 1, 1, NOW()),
    (8, 4, 201, '大杯', 4.00, 0, 2, NOW()),
    (9, 5, 201, '无糖', 0.00, 0, 1, NOW()),
    (10, 5, 201, '少糖', 0.00, 0, 2, NOW()),
    (49, 5, 201, '正常糖', 0.00, 1, 3, NOW()),
    (11, 6, 202, '热', 0.00, 1, 1, NOW()),
    (12, 6, 202, '冰', 0.00, 0, 2, NOW()),
    (13, 7, 101, '无糖', 0.00, 0, 1, NOW()),
    (14, 7, 101, '少糖', 0.00, 0, 2, NOW()),
    (15, 7, 101, '正常糖', 0.00, 1, 3, NOW()),
    (16, 8, 101, '门店拼配豆', 0.00, 1, 1, NOW()),
    (17, 8, 101, '埃塞俄比亚', 0.00, 0, 2, NOW()),
    (18, 8, 101, '哥伦比亚', 0.00, 0, 3, NOW()),
    (19, 9, 101, '标准', 0.00, 1, 1, NOW()),
    (20, 9, 101, '加一份浓缩', 5.00, 0, 2, NOW()),
    (21, 9, 101, '加两份浓缩', 10.00, 0, 3, NOW()),
    (22, 11, 201, '门店拼配豆', 0.00, 1, 1, NOW()),
    (23, 11, 201, '埃塞俄比亚', 0.00, 0, 2, NOW()),
    (24, 11, 201, '哥伦比亚', 0.00, 0, 3, NOW()),
    (25, 12, 201, '标准', 0.00, 1, 1, NOW()),
    (26, 12, 201, '加一份浓缩', 5.00, 0, 2, NOW()),
    (27, 12, 201, '加两份浓缩', 10.00, 0, 3, NOW()),
    (28, 13, 202, '中杯', 0.00, 1, 1, NOW()),
    (29, 13, 202, '大杯', 3.00, 0, 2, NOW()),
    (30, 14, 202, '无糖', 0.00, 0, 1, NOW()),
    (31, 14, 202, '少糖', 0.00, 0, 2, NOW()),
    (32, 14, 202, '正常糖', 0.00, 1, 3, NOW()),
    (33, 15, 202, '门店拼配豆', 0.00, 1, 1, NOW()),
    (34, 15, 202, '埃塞俄比亚', 0.00, 0, 2, NOW()),
    (35, 15, 202, '哥伦比亚', 0.00, 0, 3, NOW()),
    (36, 16, 202, '标准', 0.00, 1, 1, NOW()),
    (37, 16, 202, '加一份浓缩', 5.00, 0, 2, NOW()),
    (38, 16, 202, '加两份浓缩', 10.00, 0, 3, NOW()),
    (39, 17, 301, '热', 0.00, 1, 1, NOW()),
    (40, 17, 301, '冰', 0.00, 0, 2, NOW()),
    (41, 18, 301, '中杯', 0.00, 1, 1, NOW()),
    (42, 18, 301, '大杯', 3.00, 0, 2, NOW()),
    (43, 19, 301, '无糖', 0.00, 0, 1, NOW()),
    (44, 19, 301, '少糖', 0.00, 0, 2, NOW()),
    (45, 19, 301, '正常糖', 0.00, 1, 3, NOW()),
    (50, 20, 102, '热', 0.00, 1, 1, NOW()),
    (51, 20, 102, '冰', 0.00, 0, 2, NOW()),
    (52, 21, 102, '中杯', 0.00, 1, 1, NOW()),
    (53, 21, 102, '大杯', 3.00, 0, 2, NOW()),
    (54, 22, 102, '无糖', 0.00, 0, 1, NOW()),
    (55, 22, 102, '少糖', 0.00, 0, 2, NOW()),
    (56, 22, 102, '正常糖', 0.00, 1, 3, NOW()),
    (57, 23, 102, '门店拼配豆', 0.00, 1, 1, NOW()),
    (58, 23, 102, '埃塞俄比亚', 0.00, 0, 2, NOW()),
    (59, 23, 102, '哥伦比亚', 0.00, 0, 3, NOW()),
    (60, 24, 102, '标准', 0.00, 1, 1, NOW()),
    (61, 24, 102, '加一份浓缩', 5.00, 0, 2, NOW()),
    (62, 24, 102, '加两份浓缩', 10.00, 0, 3, NOW()),
    (63, 25, 103, '热', 0.00, 1, 1, NOW()),
    (64, 25, 103, '冰', 0.00, 0, 2, NOW()),
    (65, 26, 103, '中杯', 0.00, 1, 1, NOW()),
    (66, 26, 103, '大杯', 3.00, 0, 2, NOW()),
    (67, 27, 103, '无糖', 0.00, 0, 1, NOW()),
    (68, 27, 103, '少糖', 0.00, 0, 2, NOW()),
    (69, 27, 103, '正常糖', 0.00, 1, 3, NOW()),
    (70, 28, 103, '门店拼配豆', 0.00, 1, 1, NOW()),
    (71, 28, 103, '埃塞俄比亚', 0.00, 0, 2, NOW()),
    (72, 28, 103, '哥伦比亚', 0.00, 0, 3, NOW()),
    (73, 29, 103, '标准', 0.00, 1, 1, NOW()),
    (74, 29, 103, '加一份浓缩', 5.00, 0, 2, NOW()),
    (75, 29, 103, '加两份浓缩', 10.00, 0, 3, NOW()),
    (76, 30, 203, '热', 0.00, 1, 1, NOW()),
    (77, 30, 203, '冰', 0.00, 0, 2, NOW()),
    (78, 31, 203, '中杯', 0.00, 1, 1, NOW()),
    (79, 31, 203, '大杯', 3.00, 0, 2, NOW()),
    (80, 32, 203, '无糖', 0.00, 0, 1, NOW()),
    (81, 32, 203, '少糖', 0.00, 0, 2, NOW()),
    (82, 32, 203, '正常糖', 0.00, 1, 3, NOW()),
    (83, 33, 203, '门店拼配豆', 0.00, 1, 1, NOW()),
    (84, 33, 203, '埃塞俄比亚', 0.00, 0, 2, NOW()),
    (85, 33, 203, '哥伦比亚', 0.00, 0, 3, NOW()),
    (86, 34, 203, '标准', 0.00, 1, 1, NOW()),
    (87, 34, 203, '加一份浓缩', 5.00, 0, 2, NOW()),
    (88, 34, 203, '加两份浓缩', 10.00, 0, 3, NOW()),
    (89, 35, 302, '热', 0.00, 1, 1, NOW()),
    (90, 35, 302, '冰', 0.00, 0, 2, NOW()),
    (91, 36, 302, '中杯', 0.00, 1, 1, NOW()),
    (92, 36, 302, '大杯', 3.00, 0, 2, NOW()),
    (93, 37, 302, '无糖', 0.00, 0, 1, NOW()),
    (94, 37, 302, '少糖', 0.00, 0, 2, NOW()),
    (95, 37, 302, '正常糖', 0.00, 1, 3, NOW());

INSERT INTO `t_scan_table_qrcode`
    (`table_id`, `table_no`, `scene`, `status`, `create_by`, `create_time`)
VALUES
    (1, 'A01', 'dine_in', 1, 'test-data', NOW()),
    (2, 'A02', 'dine_in', 1, 'test-data', NOW()),
    (3, 'B01', 'dine_in', 1, 'test-data', NOW());

-- -----------------------------------------------------------------------------
-- 5. 扫码点单订单、订单明细和购物车
-- -----------------------------------------------------------------------------
INSERT INTO `t_scan_order`
    (`order_id`, `order_no`, `user_id`, `openid`, `table_no`, `scene`,
     `total_amount`, `pay_amount`, `discount_amount`, `member_discount`, `status`, `pickup_no`,
     `pay_type`, `pay_time`, `finish_time`, `refund_status`, `create_by`, `create_time`, `update_time`)
VALUES
    (4001, 'COFFEE-SCAN-4001', 9003, 'coffee-test-9003', 'A01', 'dine_in',
     36.00, 36.00, 0.00, 0.00, 4, 'A0101', 'wechat',
     NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, 0, 'test-data', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
    (4002, 'COFFEE-SCAN-4002', 9004, 'coffee-test-9004', 'A02', 'dine_in',
     18.00, 0.00, 0.00, 0.00, 5, NULL, 'wechat',
     NULL, NULL, 0, 'test-data', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY);

INSERT INTO `t_scan_order_item`
    (`item_id`, `order_id`, `product_id`, `product_name`, `product_image`, `spec`, `price`, `quantity`, `total_price`, `create_time`)
VALUES
    (4101, 4001, 201, '拿铁咖啡', NULL, '热/中杯', 18.00, 2, 36.00, NOW() - INTERVAL 2 DAY),
    (4102, 4002, 201, '拿铁咖啡', NULL, '冰/中杯', 18.00, 1, 18.00, NOW() - INTERVAL 1 DAY);

INSERT INTO `t_scan_cart`
    (`id`, `user_id`, `openid`, `table_no`, `product_id`, `product_name`, `product_image`,
     `price`, `quantity`, `spec_text`, `spec_json`, `selected`, `status`, `create_time`, `update_time`, `del_flag`)
VALUES
    (7001, 9003, 'coffee-test-9003', 'A01', 201, '拿铁咖啡', NULL,
     18.00, 1, '热/中杯', '{"specId":3,"optionId":5}', 1, 1,
     NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR, 0),
    (7002, 9004, 'coffee-test-9004', 'A02', 202, '燕麦拿铁', NULL,
     22.00, 1, '冰', '{"specId":6,"optionId":12}', 1, 1,
     NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR, 0);

-- -----------------------------------------------------------------------------
-- 6. 用户行为证据
-- -----------------------------------------------------------------------------
INSERT INTO `t_user_behavior_event`
    (`event_id`, `user_id`, `event_type`, `scene`, `product_id`, `category_id`, `source_id`,
     `source`, `dedup_key`, `event_time`)
VALUES
    (5001, 9002, 'PRODUCT_VIEW', 'MALL', 1001, 11, NULL, 'CATEGORY',
        'test:PRODUCT_VIEW:MALL:9002:1001:20260807', NOW() - INTERVAL 2 HOUR),
    (5002, 9003, 'PRODUCT_VIEW', 'MALL', 1002, 11, NULL, 'CATEGORY',
        'test:PRODUCT_VIEW:MALL:9003:1002:20260807', NOW() - INTERVAL 4 HOUR),
    (5003, 9003, 'PRODUCT_VIEW', 'SCAN', 201, 2, NULL, 'PERSONALIZED_LIST',
        'test:PRODUCT_VIEW:SCAN:9003:201:20260807', NOW() - INTERVAL 5 HOUR),
    (5004, 9003, 'CART_ADD', 'MALL', 1003, 11, 6001, 'DEFAULT_LIST',
        'test:CART_ADD:MALL:6001', NOW() - INTERVAL 3 HOUR),
    (5005, 9004, 'PRODUCT_VIEW', 'SCAN', 201, 2, NULL, 'DEFAULT_LIST',
        'test:PRODUCT_VIEW:SCAN:9004:201:20260807', NOW() - INTERVAL 6 HOUR),
    (5006, 9004, 'PRODUCT_VIEW', 'SCAN', 202, 2, NULL, 'CATEGORY',
        'test:PRODUCT_VIEW:SCAN:9004:202:20260807', NOW() - INTERVAL 5 HOUR),
    (5007, 9004, 'PRODUCT_VIEW', 'MALL', 1001, 11, NULL, 'DEFAULT_LIST',
        'test:PRODUCT_VIEW:MALL:9004:1001:20260807', NOW() - INTERVAL 4 HOUR),
    (5008, 9004, 'CART_ADD', 'SCAN', 202, 2, 7002, 'DEFAULT_LIST',
        'test:CART_ADD:SCAN:7002', NOW() - INTERVAL 2 HOUR),
    (5010, 9004, 'CART_REMOVE', 'SCAN', 202, 2, 7002, 'DEFAULT_LIST',
        'test:CART_REMOVE:SCAN:7002', NOW() - INTERVAL 30 MINUTE);

-- -----------------------------------------------------------------------------
-- 7. 画像快照
-- -----------------------------------------------------------------------------
INSERT INTO `t_user_profile`
    (`user_id`, `order_count`, `total_amount`, `avg_order_amount`, `preferred_price_min`,
     `preferred_price_max`, `last_order_time`, `last_active_time`, `evidence_count`,
     `profile_status`, `profile_data`, `calculate_time`, `create_time`, `update_time`)
VALUES
    (9001, 0, 0.00, 0.00, NULL, NULL, NULL, NULL, 0, 'EMPTY',
        JSON_OBJECT(
            'MALL', JSON_OBJECT('categories', JSON_ARRAY(), 'products', JSON_ARRAY()),
            'SCAN', JSON_OBJECT('categories', JSON_ARRAY(), 'products', JSON_ARRAY())),
        NOW(), NOW(), NOW()),
    (9002, 0, 0.00, 0.00, 60.00, 69.99, NULL, NOW() - INTERVAL 2 HOUR, 1, 'LEARNING',
        JSON_OBJECT(
            'MALL', JSON_OBJECT(
                'categories', JSON_ARRAY(JSON_OBJECT(
                    'id', 11, 'name', '咖啡豆', 'score', 1.00, 'evidenceCount', 1,
                    'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 2 HOUR, '%Y-%m-%d %H:%i:%s'))),
                'products', JSON_ARRAY(JSON_OBJECT(
                    'id', 1001, 'name', '耶加雪菲咖啡豆', 'score', 1.00, 'evidenceCount', 1,
                    'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 2 HOUR, '%Y-%m-%d %H:%i:%s')))),
            'SCAN', JSON_OBJECT('categories', JSON_ARRAY(), 'products', JSON_ARRAY())),
        NOW(), NOW(), NOW()),
    (9003, 2, 166.00, 83.00, 10.00, 19.99, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 3 HOUR, 6, 'READY',
        JSON_OBJECT(
            'MALL', JSON_OBJECT(
                'categories', JSON_ARRAY(JSON_OBJECT(
                    'id', 11, 'name', '咖啡豆', 'score', 14.00, 'evidenceCount', 4,
                    'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 3 HOUR, '%Y-%m-%d %H:%i:%s'))),
                'products', JSON_ARRAY(
                    JSON_OBJECT('id', 1002, 'name', '哥伦比亚咖啡豆', 'score', 6.00, 'evidenceCount', 2,
                        'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 4 HOUR, '%Y-%m-%d %H:%i:%s')),
                    JSON_OBJECT('id', 1001, 'name', '耶加雪菲咖啡豆', 'score', 5.00, 'evidenceCount', 1,
                        'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 3 DAY, '%Y-%m-%d %H:%i:%s')),
                    JSON_OBJECT('id', 1003, 'name', '手冲咖啡礼盒', 'score', 3.00, 'evidenceCount', 1,
                        'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 3 HOUR, '%Y-%m-%d %H:%i:%s')))),
            'SCAN', JSON_OBJECT(
                'categories', JSON_ARRAY(JSON_OBJECT(
                    'id', 2, 'name', '奶咖', 'score', 6.00, 'evidenceCount', 2,
                    'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 5 HOUR, '%Y-%m-%d %H:%i:%s'))),
                'products', JSON_ARRAY(JSON_OBJECT(
                    'id', 201, 'name', '拿铁咖啡', 'score', 6.00, 'evidenceCount', 2,
                    'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 5 HOUR, '%Y-%m-%d %H:%i:%s'))))),
        NOW(), NOW(), NOW()),
    (9004, 0, 0.00, 0.00, 20.00, 29.99, NULL, NOW() - INTERVAL 30 MINUTE, 4, 'READY',
        JSON_OBJECT(
            'MALL', JSON_OBJECT(
                'categories', JSON_ARRAY(JSON_OBJECT(
                    'id', 11, 'name', '咖啡豆', 'score', 1.00, 'evidenceCount', 1,
                    'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 4 HOUR, '%Y-%m-%d %H:%i:%s'))),
                'products', JSON_ARRAY(JSON_OBJECT(
                    'id', 1001, 'name', '耶加雪菲咖啡豆', 'score', 1.00, 'evidenceCount', 1,
                    'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 4 HOUR, '%Y-%m-%d %H:%i:%s')))),
            'SCAN', JSON_OBJECT(
                'categories', JSON_ARRAY(JSON_OBJECT(
                    'id', 2, 'name', '奶咖', 'score', 5.00, 'evidenceCount', 3,
                    'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 2 HOUR, '%Y-%m-%d %H:%i:%s'))),
                'products', JSON_ARRAY(
                    JSON_OBJECT('id', 202, 'name', '燕麦拿铁', 'score', 4.00, 'evidenceCount', 2,
                        'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 2 HOUR, '%Y-%m-%d %H:%i:%s')),
                    JSON_OBJECT('id', 201, 'name', '拿铁咖啡', 'score', 1.00, 'evidenceCount', 1,
                        'lastEvidenceTime', DATE_FORMAT(NOW() - INTERVAL 6 HOUR, '%Y-%m-%d %H:%i:%s'))))),
        NOW(), NOW(), NOW());

COMMIT;
