/*
 咖啡扫码点单 后台菜单
 -----------------------------------------------------------------------------
 请在 coffee_menu.sql 之后执行。
 扫码点单菜单固定使用 2100-2199 的 menu_id 区间,避免与原有咖啡商城菜单(2000-2099)冲突。
 本脚本可重复执行:每次执行前先清空 2100-2199 区间再插入。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 2100 AND 2199;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 2100 AND 2199;

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `url`, `target`, `menu_type`, `visible`, `is_refresh`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
-- 顶层目录: 扫码点单
(2100, '扫码点单', 0, 3, '#', '', 'M', '0', '1', '', 'fa fa-qrcode', 'admin', NOW(), '', NULL, '扫码点单目录'),

-- 点单分类管理
(2101, '点单分类', 2100, 1, 'coffee/scanCategory', '', 'C', '0', '1', 'coffee:scanCategory:view',   'tree',  'admin', NOW(), '', NULL, '点单分类管理'),
(2102, '分类查询', 2101, 1, '#',                    '', 'F', '0', '1', 'coffee:scanCategory:list',   '#',     'admin', NOW(), '', NULL, ''),
(2103, '分类新增', 2101, 2, '#',                    '', 'F', '0', '1', 'coffee:scanCategory:add',    '#',     'admin', NOW(), '', NULL, ''),
(2104, '分类修改', 2101, 3, '#',                    '', 'F', '0', '1', 'coffee:scanCategory:edit',   '#',     'admin', NOW(), '', NULL, ''),
(2105, '分类删除', 2101, 4, '#',                    '', 'F', '0', '1', 'coffee:scanCategory:remove', '#',     'admin', NOW(), '', NULL, ''),

-- 点单商品管理
(2110, '点单商品', 2100, 2, 'coffee/scanProduct',   '', 'C', '0', '1', 'coffee:scanProduct:view',    'goods', 'admin', NOW(), '', NULL, '点单商品管理'),
(2111, '商品查询', 2110, 1, '#',                    '', 'F', '0', '1', 'coffee:scanProduct:list',    '#',     'admin', NOW(), '', NULL, ''),
(2112, '商品新增', 2110, 2, '#',                    '', 'F', '0', '1', 'coffee:scanProduct:add',     '#',     'admin', NOW(), '', NULL, ''),
(2113, '商品修改', 2110, 3, '#',                    '', 'F', '0', '1', 'coffee:scanProduct:edit',    '#',     'admin', NOW(), '', NULL, ''),
(2114, '商品删除', 2110, 4, '#',                    '', 'F', '0', '1', 'coffee:scanProduct:remove',  '#',     'admin', NOW(), '', NULL, ''),

-- 桌台二维码
(2120, '桌台二维码', 2100, 3, 'coffee/scanTable',   '', 'C', '0', '1', 'coffee:scanTable:view',      'fa fa-qrcode', 'admin', NOW(), '', NULL, '桌台二维码管理'),
(2121, '桌台查询',   2120, 1, '#',                  '', 'F', '0', '1', 'coffee:scanTable:list',      '#', 'admin', NOW(), '', NULL, ''),
(2122, '桌台新增',   2120, 2, '#',                  '', 'F', '0', '1', 'coffee:scanTable:add',       '#', 'admin', NOW(), '', NULL, ''),
(2123, '桌台修改',   2120, 3, '#',                  '', 'F', '0', '1', 'coffee:scanTable:edit',      '#', 'admin', NOW(), '', NULL, ''),
(2124, '桌台删除',   2120, 4, '#',                  '', 'F', '0', '1', 'coffee:scanTable:remove',    '#', 'admin', NOW(), '', NULL, ''),

-- 点单订单
(2130, '点单订单', 2100, 4, 'coffee/scanOrder',     '', 'C', '0', '1', 'coffee:scanOrder:view',      'form', 'admin', NOW(), '', NULL, '点单订单管理'),
(2131, '订单查询', 2130, 1, '#',                    '', 'F', '0', '1', 'coffee:scanOrder:list',      '#', 'admin', NOW(), '', NULL, ''),
(2132, '订单修改', 2130, 2, '#',                    '', 'F', '0', '1', 'coffee:scanOrder:edit',      '#', 'admin', NOW(), '', NULL, ''),
(2133, '订单删除', 2130, 3, '#',                    '', 'F', '0', '1', 'coffee:scanOrder:remove',    '#', 'admin', NOW(), '', NULL, '');

-- 将扫码点单菜单授权给超级管理员角色
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id`
FROM `sys_menu`
WHERE `menu_id` BETWEEN 2100 AND 2199;

SET FOREIGN_KEY_CHECKS = 1;
