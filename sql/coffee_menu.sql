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

-- 隐藏当前侧边栏不显示的若依原始菜单。
-- 咖啡商城菜单固定使用 2000-2099 的菜单 ID，并在下方先删后建，
-- 避免和若依内置菜单重复或冲突。
UPDATE `sys_menu`
SET `visible` = '0'
WHERE `menu_id` IN (100, 101, 102, 105, 106);

UPDATE `sys_menu`
SET `visible` = '1'
WHERE `menu_id` IN (
  2, 3, 4,
  103, 104, 107, 108,
  109, 110, 111, 112, 113,
  114, 115, 116,
  500, 501
);

-- 重建咖啡商城菜单，保证本脚本可以重复执行。
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 2000 AND 2099;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 2000 AND 2099;

INSERT INTO `sys_menu`
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
(2076, '流水查询', 2075, 1, '#', '', 'F', '0', '1', 'coffee:walletLog:list', '#', 'admin', NOW(), '', NULL, '');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id`
FROM `sys_menu`
WHERE `menu_id` BETWEEN 2000 AND 2099;

SET FOREIGN_KEY_CHECKS = 1;
