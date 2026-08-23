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
    (10001, 101, '温度', 'single', 1, 1, NOW()),
    (10002, 101, '杯型', 'single', 1, 2, NOW()),
    (10003, 201, '温度', 'single', 1, 1, NOW()),
    (10004, 201, '杯型', 'single', 1, 2, NOW()),
    (10005, 201, '糖度', 'single', 1, 3, NOW()),
    (10006, 202, '温度', 'single', 1, 1, NOW()),
    (10007, 101, '糖度', 'single', 1, 3, NOW()),
    (10008, 101, '咖啡豆', 'single', 1, 4, NOW()),
    (10009, 101, '咖啡浓度', 'single', 1, 5, NOW()),
    (10011, 201, '咖啡豆', 'single', 1, 5, NOW()),
    (10012, 201, '咖啡浓度', 'single', 1, 6, NOW()),
    (10013, 202, '杯型', 'single', 1, 2, NOW()),
    (10014, 202, '糖度', 'single', 1, 3, NOW()),
    (10015, 202, '咖啡豆', 'single', 1, 4, NOW()),
    (10016, 202, '咖啡浓度', 'single', 1, 5, NOW()),
    (10017, 301, '温度', 'single', 1, 1, NOW()),
    (10018, 301, '杯型', 'single', 1, 2, NOW()),
    (10019, 301, '糖度', 'single', 1, 3, NOW()),
    (10020, 102, '温度', 'single', 1, 1, NOW()),
    (10021, 102, '杯型', 'single', 1, 2, NOW()),
    (10022, 102, '糖度', 'single', 1, 3, NOW()),
    (10023, 102, '咖啡豆', 'single', 1, 4, NOW()),
    (10024, 102, '咖啡浓度', 'single', 1, 5, NOW()),
    (10025, 103, '温度', 'single', 1, 1, NOW()),
    (10026, 103, '杯型', 'single', 1, 2, NOW()),
    (10027, 103, '糖度', 'single', 1, 3, NOW()),
    (10028, 103, '咖啡豆', 'single', 1, 4, NOW()),
    (10029, 103, '咖啡浓度', 'single', 1, 5, NOW()),
    (10030, 203, '温度', 'single', 1, 1, NOW()),
    (10031, 203, '杯型', 'single', 1, 2, NOW()),
    (10032, 203, '糖度', 'single', 1, 3, NOW()),
    (10033, 203, '咖啡豆', 'single', 1, 4, NOW()),
    (10034, 203, '咖啡浓度', 'single', 1, 5, NOW()),
    (10035, 302, '温度', 'single', 1, 1, NOW()),
    (10036, 302, '杯型', 'single', 1, 2, NOW()),
    (10037, 302, '糖度', 'single', 1, 3, NOW());

INSERT INTO `t_scan_product_spec_option`
    (`option_id`, `spec_id`, `product_id`, `option_name`, `extra_price`, `is_default`, `sort_order`, `create_time`)
VALUES
    (20001, 10001, 101, '热', 0.00, 1, 1, NOW()),
    (20002, 10001, 101, '冰', 0.00, 0, 2, NOW()),
    (20003, 10002, 101, '中杯', 0.00, 1, 1, NOW()),
    (20004, 10002, 101, '大杯', 3.00, 0, 2, NOW()),
    (20005, 10003, 201, '热', 0.00, 1, 1, NOW()),
    (20006, 10003, 201, '冰', 0.00, 0, 2, NOW()),
    (20007, 10004, 201, '中杯', 0.00, 1, 1, NOW()),
    (20008, 10004, 201, '大杯', 4.00, 0, 2, NOW()),
    (20009, 10005, 201, '无糖', 0.00, 0, 1, NOW()),
    (20010, 10005, 201, '少糖', 0.00, 0, 2, NOW()),
    (20049, 10005, 201, '正常糖', 0.00, 1, 3, NOW()),
    (20011, 10006, 202, '热', 0.00, 1, 1, NOW()),
    (20012, 10006, 202, '冰', 0.00, 0, 2, NOW()),
    (20013, 10007, 101, '无糖', 0.00, 0, 1, NOW()),
    (20014, 10007, 101, '少糖', 0.00, 0, 2, NOW()),
    (20015, 10007, 101, '正常糖', 0.00, 1, 3, NOW()),
    (20016, 10008, 101, '门店拼配豆', 0.00, 1, 1, NOW()),
    (20017, 10008, 101, '埃塞俄比亚', 0.00, 0, 2, NOW()),
    (20018, 10008, 101, '哥伦比亚', 0.00, 0, 3, NOW()),
    (20019, 10009, 101, '标准', 0.00, 1, 1, NOW()),
    (20020, 10009, 101, '加一份浓缩', 5.00, 0, 2, NOW()),
    (20021, 10009, 101, '加两份浓缩', 10.00, 0, 3, NOW()),
    (20022, 10011, 201, '门店拼配豆', 0.00, 1, 1, NOW()),
    (20023, 10011, 201, '埃塞俄比亚', 0.00, 0, 2, NOW()),
    (20024, 10011, 201, '哥伦比亚', 0.00, 0, 3, NOW()),
    (20025, 10012, 201, '标准', 0.00, 1, 1, NOW()),
    (20026, 10012, 201, '加一份浓缩', 5.00, 0, 2, NOW()),
    (20027, 10012, 201, '加两份浓缩', 10.00, 0, 3, NOW()),
    (20028, 10013, 202, '中杯', 0.00, 1, 1, NOW()),
    (20029, 10013, 202, '大杯', 3.00, 0, 2, NOW()),
    (20030, 10014, 202, '无糖', 0.00, 0, 1, NOW()),
    (20031, 10014, 202, '少糖', 0.00, 0, 2, NOW()),
    (20032, 10014, 202, '正常糖', 0.00, 1, 3, NOW()),
    (20033, 10015, 202, '门店拼配豆', 0.00, 1, 1, NOW()),
    (20034, 10015, 202, '埃塞俄比亚', 0.00, 0, 2, NOW()),
    (20035, 10015, 202, '哥伦比亚', 0.00, 0, 3, NOW()),
    (20036, 10016, 202, '标准', 0.00, 1, 1, NOW()),
    (20037, 10016, 202, '加一份浓缩', 5.00, 0, 2, NOW()),
    (20038, 10016, 202, '加两份浓缩', 10.00, 0, 3, NOW()),
    (20039, 10017, 301, '热', 0.00, 1, 1, NOW()),
    (20040, 10017, 301, '冰', 0.00, 0, 2, NOW()),
    (20041, 10018, 301, '中杯', 0.00, 1, 1, NOW()),
    (20042, 10018, 301, '大杯', 3.00, 0, 2, NOW()),
    (20043, 10019, 301, '无糖', 0.00, 0, 1, NOW()),
    (20044, 10019, 301, '少糖', 0.00, 0, 2, NOW()),
    (20045, 10019, 301, '正常糖', 0.00, 1, 3, NOW()),
    (20050, 10020, 102, '热', 0.00, 1, 1, NOW()),
    (20051, 10020, 102, '冰', 0.00, 0, 2, NOW()),
    (20052, 10021, 102, '中杯', 0.00, 1, 1, NOW()),
    (20053, 10021, 102, '大杯', 3.00, 0, 2, NOW()),
    (20054, 10022, 102, '无糖', 0.00, 0, 1, NOW()),
    (20055, 10022, 102, '少糖', 0.00, 0, 2, NOW()),
    (20056, 10022, 102, '正常糖', 0.00, 1, 3, NOW()),
    (20057, 10023, 102, '门店拼配豆', 0.00, 1, 1, NOW()),
    (20058, 10023, 102, '埃塞俄比亚', 0.00, 0, 2, NOW()),
    (20059, 10023, 102, '哥伦比亚', 0.00, 0, 3, NOW()),
    (20060, 10024, 102, '标准', 0.00, 1, 1, NOW()),
    (20061, 10024, 102, '加一份浓缩', 5.00, 0, 2, NOW()),
    (20062, 10024, 102, '加两份浓缩', 10.00, 0, 3, NOW()),
    (20063, 10025, 103, '热', 0.00, 1, 1, NOW()),
    (20064, 10025, 103, '冰', 0.00, 0, 2, NOW()),
    (20065, 10026, 103, '中杯', 0.00, 1, 1, NOW()),
    (20066, 10026, 103, '大杯', 3.00, 0, 2, NOW()),
    (20067, 10027, 103, '无糖', 0.00, 0, 1, NOW()),
    (20068, 10027, 103, '少糖', 0.00, 0, 2, NOW()),
    (20069, 10027, 103, '正常糖', 0.00, 1, 3, NOW()),
    (20070, 10028, 103, '门店拼配豆', 0.00, 1, 1, NOW()),
    (20071, 10028, 103, '埃塞俄比亚', 0.00, 0, 2, NOW()),
    (20072, 10028, 103, '哥伦比亚', 0.00, 0, 3, NOW()),
    (20073, 10029, 103, '标准', 0.00, 1, 1, NOW()),
    (20074, 10029, 103, '加一份浓缩', 5.00, 0, 2, NOW()),
    (20075, 10029, 103, '加两份浓缩', 10.00, 0, 3, NOW()),
    (20076, 10030, 203, '热', 0.00, 1, 1, NOW()),
    (20077, 10030, 203, '冰', 0.00, 0, 2, NOW()),
    (20078, 10031, 203, '中杯', 0.00, 1, 1, NOW()),
    (20079, 10031, 203, '大杯', 3.00, 0, 2, NOW()),
    (20080, 10032, 203, '无糖', 0.00, 0, 1, NOW()),
    (20081, 10032, 203, '少糖', 0.00, 0, 2, NOW()),
    (20082, 10032, 203, '正常糖', 0.00, 1, 3, NOW()),
    (20083, 10033, 203, '门店拼配豆', 0.00, 1, 1, NOW()),
    (20084, 10033, 203, '埃塞俄比亚', 0.00, 0, 2, NOW()),
    (20085, 10033, 203, '哥伦比亚', 0.00, 0, 3, NOW()),
    (20086, 10034, 203, '标准', 0.00, 1, 1, NOW()),
    (20087, 10034, 203, '加一份浓缩', 5.00, 0, 2, NOW()),
    (20088, 10034, 203, '加两份浓缩', 10.00, 0, 3, NOW()),
    (20089, 10035, 302, '热', 0.00, 1, 1, NOW()),
    (20090, 10035, 302, '冰', 0.00, 0, 2, NOW()),
    (20091, 10036, 302, '中杯', 0.00, 1, 1, NOW()),
    (20092, 10036, 302, '大杯', 3.00, 0, 2, NOW()),
    (20093, 10037, 302, '无糖', 0.00, 0, 1, NOW()),
    (20094, 10037, 302, '少糖', 0.00, 0, 2, NOW()),
    (20095, 10037, 302, '正常糖', 0.00, 1, 3, NOW());

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

UPDATE `t_scan_order_item`
SET `spec_json` = CASE `item_id`
    WHEN 4101 THEN '[{"specName":"温度","optionNames":["热"]},{"specName":"杯型","optionNames":["中杯"]}]'
    WHEN 4102 THEN '[{"specName":"温度","optionNames":["冰"]},{"specName":"杯型","optionNames":["中杯"]}]'
END
WHERE `item_id` IN (4101, 4102);

INSERT INTO `t_scan_cart`
    (`id`, `user_id`, `openid`, `table_no`, `product_id`, `product_name`, `product_image`,
     `price`, `quantity`, `spec_text`, `spec_json`, `selected`, `status`, `create_time`, `update_time`, `del_flag`)
VALUES
    (7001, 9003, 'coffee-test-9003', 'A01', 201, '拿铁咖啡', NULL,
     18.00, 1, '热/中杯', '{"specId":10003,"optionId":20005}', 1, 1,
     NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR, 0),
     (7002, 9004, 'coffee-test-9004', 'A02', 202, '燕麦拿铁', NULL,
      22.00, 1, '冰', '{"specId":10006,"optionId":20012}', 1, 1,
      NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR, 0);

UPDATE `t_scan_cart`
SET `spec_json` = CASE `id`
    WHEN 7001 THEN '[{"specName":"温度","optionNames":["热"]},{"specName":"杯型","optionNames":["中杯"]}]'
    WHEN 7002 THEN '[{"specName":"温度","optionNames":["冰"]},{"specName":"杯型","optionNames":["中杯"]},{"specName":"糖度","optionNames":["少糖"]}]'
END
WHERE `id` IN (7001, 7002);

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
UPDATE `t_user_behavior_event`
SET `spec_json` = '[{"specName":"温度","optionNames":["冰"]},{"specName":"杯型","optionNames":["中杯"]},{"specName":"糖度","optionNames":["少糖"]}]'
WHERE `event_id` = 5008;

INSERT INTO `t_user_profile`
    (`user_id`, `profile_status`, `profile_data`, `calculate_time`, `create_time`, `update_time`)
VALUES
    (9001, 'EMPTY',
        JSON_OBJECT(
            'MALL', JSON_OBJECT('categories', JSON_ARRAY(), 'products', JSON_ARRAY()),
            'SCAN', JSON_OBJECT('categories', JSON_ARRAY(), 'products', JSON_ARRAY())),
        NOW(), NOW(), NOW()),
    (9002, 'LEARNING',
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
    (9003, 'READY',
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
    (9004, 'READY',
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

UPDATE `t_user_profile`
SET `profile_data` = JSON_OBJECT(
    'MALL', JSON_OBJECT('tags', JSON_ARRAY()),
    'SCAN', JSON_OBJECT('tags', JSON_ARRAY()))
WHERE `user_id` = 9001;

UPDATE `t_user_profile`
SET `profile_data` = JSON_OBJECT(
    'MALL', JSON_OBJECT('tags', JSON_ARRAY(
        JSON_OBJECT('key','category:11','dimension','category','name','咖啡豆','score',1.00),
        JSON_OBJECT('key','price:60.00-69.99','dimension','price','name','60.00-69.99',
            'score',1.00,'min',60.00,'max',69.99))),
    'SCAN', JSON_OBJECT('tags', JSON_ARRAY()))
WHERE `user_id` = 9002;

UPDATE `t_user_profile`
SET `profile_data` = JSON_OBJECT(
    'MALL', JSON_OBJECT('tags', JSON_ARRAY(
        JSON_OBJECT('key','category:11','dimension','category','name','咖啡豆','score',14.00),
        JSON_OBJECT('key','product:1002','dimension','product','name','哥伦比亚咖啡豆','score',6.00),
        JSON_OBJECT('key','price:80.00-89.99','dimension','price','name','80.00-89.99',
            'score',8.00,'min',80.00,'max',89.99))),
    'SCAN', JSON_OBJECT('tags', JSON_ARRAY(
        JSON_OBJECT('key','product:201','dimension','product','name','拿铁咖啡','score',14.00),
        JSON_OBJECT('key','category:2','dimension','category','name','奶咖','score',14.00),
        JSON_OBJECT('key','temperature:热','dimension','temperature','name','热','score',8.00),
        JSON_OBJECT('key','cup:中杯','dimension','cup','name','中杯','score',8.00),
        JSON_OBJECT('key','price:10.00-19.99','dimension','price','name','10.00-19.99',
            'score',8.00,'min',10.00,'max',19.99))))
WHERE `user_id` = 9003;

UPDATE `t_user_profile`
SET `profile_data` = JSON_OBJECT(
    'MALL', JSON_OBJECT('tags', JSON_ARRAY()),
    'SCAN', JSON_OBJECT('tags', JSON_ARRAY(
        JSON_OBJECT('key','product:202','dimension','product','name','燕麦拿铁','score',12.00),
        JSON_OBJECT('key','temperature:冰','dimension','temperature','name','冰','score',12.00),
        JSON_OBJECT('key','sugar:少糖','dimension','sugar','name','少糖','score',12.00),
        JSON_OBJECT('key','cup:中杯','dimension','cup','name','中杯','score',12.00),
        JSON_OBJECT('key','price:20.00-29.99','dimension','price','name','20.00-29.99',
            'score',12.00,'min',20.00,'max',29.99))))
WHERE `user_id` = 9004;

COMMIT;
