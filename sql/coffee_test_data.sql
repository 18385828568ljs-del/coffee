-- =============================================================================
-- 咖啡小程序测试数据
-- -----------------------------------------------------------------------------
-- 用途:
-- 1. 本文件仅用于本地开发、演示和测试环境。
-- 2. 生产环境请使用后台管理录入真实商品、规格和桌台二维码。
-- 3. 执行前请先执行 coffee_all_business.sql 创建业务表结构。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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

-- 桌台二维码
INSERT INTO `t_scan_table_qrcode`(`table_id`,`shop_id`,`shop_name`,`table_no`,`scene`,`status`,`create_by`,`create_time`) VALUES
(1, 1, '咖啡旗舰店', 'A01', 'dine_in', 1, 'admin', NOW()),
(2, 1, '咖啡旗舰店', 'A02', 'dine_in', 1, 'admin', NOW()),
(3, 1, '咖啡旗舰店', 'B01', 'dine_in', 1, 'admin', NOW());

SET FOREIGN_KEY_CHECKS = 1;
