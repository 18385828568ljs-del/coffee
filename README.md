# 咖啡商城

基于 Spring Boot、MyBatis 和 uni-app 的咖啡商城与扫码点单系统，包含后端管理端、微信小程序端、H5 端和商家装修工作台。

## 功能

- 商品、分类、库存、图片和上下架管理
- 商城购物车、收货地址、订单、余额支付和退款申请
- 桌台二维码、扫码菜单、扫码购物车、取餐号、催单和订单履约
- 会员等级、折扣、钱包余额、消费流水和充值模板
- 满减活动、指定商品优惠、线下活动和报名管理
- 商家主题模板、草稿、发布、版本恢复、门店皮肤和预览
- AI 背景、艺术字和商品图片生成，以及候选结果应用
- 用户行为采集、用户画像、临时用户意图和商品推荐排序

当前订单支付使用会员余额，微信支付和在线充值需要后续接入支付平台。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 8、Spring Boot 2.5、Spring MVC、MyBatis、Druid |
| 权限与任务 | Shiro、Quartz |
| 数据库 | MySQL 8.0+ |
| 前端 | uni-app、Vue 2、uView |
| 外部服务 | 微信小程序、腾讯云 COS、AI 图片服务 |

## 目录

```text
coffee/
├── src/main/java/com/ruoyi/project/coffee/
│   ├── api/                    小程序 REST API
│   ├── decorator/              商家装修、主题和 AI 能力
│   ├── image/                  图片生成客户端
│   ├── profile/                用户画像与推荐
│   ├── scanOrder/              扫码点单、购物车和订单
│   └── ...                     其他咖啡业务模块
├── src/main/resources/
│   ├── mybatis/coffee/         MyBatis 映射文件
│   ├── templates/coffee/       后台管理页面
│   └── schema/                 主题配置结构
├── RuoYi-AbuCoder-UniAppWx/
│   └── Ruoyi-AbuCoder-UniApp-WX/
│       ├── pages/              小程序和 H5 页面
│       ├── components/         通用组件
│       ├── theme/              主题运行时和配置
│       └── skin/               皮肤兼容层
├── sql/                        数据库脚本
├── docs/                       项目和功能文档
├── pom.xml                    Maven 配置
└── README.md
```

## 环境要求

- JDK 8
- Maven 3.6+
- MySQL 8.0+
- Node.js 14+ 和 npm
- 微信开发者工具
- HBuilderX

## 配置

本地配置文件不会提交到 Git，需要自行创建：

```text
src/main/resources/application.yml
src/main/resources/application-druid.yml
```

`application.yml` 主要配置应用、端口、日志、Shiro、MyBatis、微信、COS、AI 和装修预览地址。`application-druid.yml` 配置 MySQL 数据源和 Druid 连接池。

敏感配置使用环境变量覆盖：

| 环境变量 | 用途 |
| --- | --- |
| `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | 数据库连接 |
| `WX_APP_ID`、`WX_APP_SECRET`、`WX_TOKEN_SECRET` | 微信小程序 |
| `COS_BUCKET`、`COS_REGION`、`COS_SECRET_ID`、`COS_SECRET_KEY` | 腾讯云 COS |
| `AI_IMAGE_BASE_URL`、`AI_IMAGE_ENDPOINT`、`AI_IMAGE_API_KEY`、`AI_IMAGE_MODEL` | AI 图片服务 |
| `DECORATOR_PREVIEW_H5_URL` | 装修预览地址 |

不要把真实密码、Token、COS 密钥或 AI 密钥写入 README 或提交到 Git。

## 启动

### 初始化数据库

```sql
CREATE DATABASE ruoyi DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ruoyi;
SOURCE E:/Coffee/coffee/sql/ry_20210924.sql;
SOURCE E:/Coffee/coffee/sql/quartz.sql;
SOURCE E:/Coffee/coffee/sql/coffee_all_business.sql;
```

可选的开发测试数据：

```sql
SOURCE E:/Coffee/coffee/sql/coffee_test_data.sql;
```

生产环境执行数据库脚本前必须备份，不要重复执行会重建系统表的初始化脚本。

### 启动后端

```powershell
mvn clean package -DskipTests
mvn spring-boot:run
```

默认地址：

```text
管理端：http://localhost:8080
登录页：http://localhost:8080/login
Swagger：http://localhost:8080/swagger-ui/index.html
Druid：http://localhost:8080/druid/
```

### 启动 H5

```powershell
cd RuoYi-AbuCoder-UniAppWx/Ruoyi-AbuCoder-UniApp-WX
npm install
npm run dev:h5
```

H5 默认地址：`http://localhost:18081`。

后端不在本机时：

```powershell
$env:H5_API_PROXY = "http://后端地址:8080"
npm run dev:h5
```

### 运行微信小程序

1. 使用 HBuilderX 打开 `RuoYi-AbuCoder-UniAppWx/Ruoyi-AbuCoder-UniApp-WX`。
2. 在 `manifest.json` 中配置微信小程序 AppID。
3. 运行到微信开发者工具。
4. 在 `utils/apiconfig.js` 中配置后端地址。手机调试时使用电脑局域网 IP，不要使用 `localhost`。

## API 路由

接口基地址：`http://localhost:8080`。

### 微信与公共接口

| 方法 | 路由 | 说明 |
| --- | --- | --- |
| POST | `/wxapi/wxlogin` | 微信登录 |
| GET | `/wxapi/me` | 当前用户 |
| POST | `/wxapi/saveUserInfo` | 保存用户资料 |
| POST | `/wxapi/uploadAvatar` | 上传头像 |
| GET | `/wxapi/loadBanner` | 首页轮播图 |
| POST | `/common/upload` | 文件上传 |

### 商城与会员

| 模块 | 路由 |
| --- | --- |
| 商品 | `GET /api/product/categories`、`GET /api/product/list`、`GET /api/product/{productId}` |
| 购物车 | `GET /api/cart/list`、`POST /api/cart/add`、`PUT /api/cart/update`、`DELETE /api/cart/{cartId}` |
| 订单 | `GET /api/order/list`、`POST /api/order/create`、`PUT /api/order/pay/{orderId}`、`PUT /api/order/cancel/{orderId}` |
| 地址 | `GET /api/address/list`、`POST /api/address/add`、`PUT /api/address/update`、`PUT /api/address/setDefault/{addressId}` |
| 会员 | `GET /api/member/info`、`GET /api/member/level-config` |
| 钱包 | `GET /api/wallet/info`、`GET /api/wallet/log`、`GET /api/wallet/recharge/templates`、`GET /api/wallet/recharge/records` |
| 活动 | `GET /api/activity/list`、`POST /api/activity/preview`、`GET /api/offlineActivity/list`、`POST /api/offlineActivity/signup` |

### 扫码点单

| 模块 | 路由 |
| --- | --- |
| 菜单 | `GET /api/scanMenu/categories`、`GET /api/scanMenu/products`、`GET /api/scanMenu/products/{productId}` |
| 桌台解析 | `GET /api/scanMenu/table/parse` |
| 购物车 | `GET /api/scanCart/list`、`POST /api/scanCart/add`、`PUT /api/scanCart/update`、`DELETE /api/scanCart/{id}`、`DELETE /api/scanCart/clear` |
| 订单 | `GET /api/scanOrder/list`、`POST /api/scanOrder/create`、`GET /api/scanOrder/{orderId}`、`PUT /api/scanOrder/pay/{orderId}` |
| 履约 | `POST /api/scanOrder/urge/{orderId}`、`PUT /api/scanOrder/cancel/{orderId}` |
| 订阅消息 | `GET /api/scanOrder/subscribe-config` |
| 桌台二维码 | `POST /api/scanTableQrcode/generate`、`POST /api/scanTableQrcode/batchGenerate`、`GET /api/scanTableQrcode/download/{id}` |

### 商家装修与 AI

| 模块 | 路由 |
| --- | --- |
| 工作台 | `GET /coffee/decorator/workbench` |
| 门店和模板 | `GET /coffee/decorator/stores`、`GET /coffee/decorator/templates` |
| 主题草稿 | `GET/POST /coffee/decorator/themes`、`GET/PUT /coffee/decorator/themes/{themeId}/draft` |
| 发布版本 | `POST /coffee/decorator/themes/{themeId}/validate`、`POST /coffee/decorator/themes/{themeId}/publish`、`GET /coffee/decorator/themes/{themeId}/versions` |
| 素材与字体 | `GET/POST/DELETE /coffee/decorator/assets`、`GET /coffee/decorator/fonts` |
| AI 任务 | `POST /coffee/decorator/ai/background/tasks`、`POST /coffee/decorator/ai/art-text/tasks`、`GET /coffee/decorator/ai/tasks/{taskId}` |
| AI 结果 | `GET /coffee/decorator/ai/tasks/{taskId}/results`、`POST /coffee/decorator/ai/results/{resultId}/apply` |
| 小程序主题 | `GET /api/wx/stores/{storeCode}/theme`、`GET /api/wx/stores/{storeCode}/skin`、`GET /api/mini/skin` |

### src/main/resources/mybatis/coffee/ScanCartMapper.xml
- 保留商品图片为空时从扫码商品表回退查询图片的逻辑
- 使用 sc 表别名，修正查询条件引用
- 删除所有 shop_id 查询、插入、更新和删除条件
- 与当前 t_scan_cart 表结构保持一致

