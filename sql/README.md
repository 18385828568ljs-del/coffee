# 咖啡商城数据库SQL文件说明

## 📁 文件清单

### 一、框架SQL（必须先执行）

| 文件名 | 大小 | 用途 | 说明 |
|--------|------|------|------|
| `ry_20210924.sql` | 36K | 若依框架核心表 | 包含11个系统表（用户、角色、菜单、权限等），**不要修改** |
| `quartz.sql` | 12K | Quartz定时任务框架 | 包含11个任务调度表，**不要修改** |

### 二、业务SQL（推荐使用合并版）

#### ⭐ 推荐：使用合并版本

| 文件名 | 大小 | 用途 | 说明 |
|--------|------|------|------|
| **`coffee_all_business.sql`** | 58K | **所有业务表（合并版）** | 包含所有咖啡商城业务表，一次执行完成 |

#### 或者：使用分散版本（旧文件）

| 文件名 | 大小 | 用途 | 包含表数 |
|--------|------|------|---------|
| `coffee_business.sql` | 18K | 商城核心业务 | 17个表 |
| `coffee_scan_order.sql` | 15K | 扫码点单业务 | 7个表 |
| `coffee_scan_cart.sql` | 3K | 扫码购物车 | 1个表 |
| `coffee_menu.sql` | 9K | 后台菜单配置 | 菜单数据 |
| `coffee_scan_order_menu.sql` | 4K | 扫码菜单配置 | 菜单数据 |
| `coffee_scan_product_add_video_url.sql` | 540B | 升级脚本 | 添加video_url字段 |

---

## 🚀 初始化数据库步骤

### 方式一：推荐（使用合并版）

```bash
# 1. 创建数据库
mysql -u root -p
CREATE DATABASE ruoyi DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ruoyi;

# 2. 执行若依框架SQL
mysql -u root -p ruoyi < sql/ry_20210924.sql

# 3. 执行定时任务框架SQL
mysql -u root -p ruoyi < sql/quartz.sql

# 4. 执行业务表SQL（合并版，一次搞定）
mysql -u root -p ruoyi < sql/coffee_all_business.sql
```

### 方式二：使用分散文件（不推荐）

```bash
# 前3步同上...

# 4. 依次执行业务表SQL
mysql -u root -p ruoyi < sql/coffee_business.sql
mysql -u root -p ruoyi < sql/coffee_scan_order.sql
mysql -u root -p ruoyi < sql/coffee_scan_cart.sql
mysql -u root -p ruoyi < sql/coffee_menu.sql
```

---

## 📊 表结构说明

### 第一部分：商城核心业务（17个表）

#### 商品相关（4个表）
- `t_category` - 商品分类
- `t_product` - 商品信息
- `t_cart` - 购物车
- `t_banner` - 轮播图

#### 订单相关（3个表）
- `t_order` - 订单主表
- `t_order_item` - 订单明细
- `t_address` - 收货地址

#### 用户相关（1个表）
- `t_wxuser` - 微信用户

#### 会员钱包（5个表）
- `t_member` - 会员信息
- `t_wallet` - 钱包余额
- `t_wallet_log` - 余额流水
- `t_recharge_record` - 充值记录
- `t_recharge_template` - 充值模板

#### 营销活动（2个表）
- `t_marketing_activity` - 营销活动
- `t_marketing_activity_scope` - 活动范围

#### 线下活动（2个表）
- `t_offline_activity` - 线下活动
- `t_offline_activity_signup` - 活动预约

### 第二部分：扫码点单业务（7个表）

- `t_scan_category` - 点单分类
- `t_scan_product` - 点单商品
- `t_scan_product_spec` - 商品规格组
- `t_scan_product_spec_option` - 规格选项
- `t_scan_table_qrcode` - 桌台二维码
- `t_scan_order` - 扫码订单
- `t_scan_order_item` - 扫码订单明细

### 第三部分：扫码购物车（1个表）

- `t_scan_cart` - 扫码购物车

### 第四部分：系统配置

- `sys_menu` - 后台菜单权限配置
- `sys_job` - 定时任务配置

---

## ⚠️ 注意事项

1. **执行顺序很重要**
   - 必须先执行 `ry_20210924.sql` 和 `quartz.sql`
   - 然后再执行业务表SQL

2. **数据库字符集**
   - 统一使用 `utf8mb4` 字符集
   - 确保支持中文和emoji表情

3. **生产环境警告**
   - 所有SQL脚本都会先删除表再创建（DROP TABLE IF EXISTS）
   - 执行前请务必备份数据库
   - 建议先在测试环境验证

4. **版本更新**
   - `coffee_all_business.sql` 是最新的合并版本
   - 旧的分散文件保留作为参考，但建议使用合并版

---

## 📝 版本历史

- **v4.0** (2026-06-13) - 合并所有业务SQL到一个文件
- **v3.0** - 完善扫码点单履约流程（取餐号、催单等）
- **v2.0** - 添加线下活动功能
- **v1.0** - 初始版本

---

## 🔗 相关文档

- 项目主文档: `../README.md`
- 数据库设计文档: 待补充
- API接口文档: 待补充

---

**最后更新时间：** 2026-06-13
