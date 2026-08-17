# 咖啡商城

咖啡商城是一个基于 Spring Boot、MyBatis 和 uni-app 的咖啡商城与扫码点单系统，包含后端管理端、微信小程序端和商家装修工作台。

## 1. 技术栈

- 后端：Java 8、Spring Boot 2.5、Spring MVC、MyBatis、Druid、Shiro、Quartz
- 数据库：MySQL 8.0+
- 管理端：Spring MVC + Thymeleaf
- 小程序端：uni-app、Vue 2、uView
- 外部服务：微信小程序登录/订阅消息、腾讯云 COS、可选的 AI 图片服务

## 2. 目录结构

```text
src/main/java/com/ruoyi/                 后端 Java 源码
src/main/resources/mybatis/              MyBatis XML 映射
src/main/resources/templates/            管理端 Thymeleaf 页面
src/main/resources/static/               管理端静态资源
sql/                                     数据库初始化和迁移脚本
RuoYi-AbuCoder-UniAppWx/
Ruoyi-AbuCoder-UniApp-WX/              uni-app 小程序/H5 工程
docs/                                    主题装修功能说明
pom.xml                                  Maven 配置
```

## 3. 环境要求

- JDK 8
- Maven 3.6+
- MySQL 8.0+
- Node.js 14+，npm
- 微信开发者工具（运行微信小程序时需要）
- HBuilderX（使用可视化方式运行 uni-app 时需要）

项目没有把本地配置文件提交到 Git。以下文件被 `.gitignore` 忽略，首次运行时必须在本地创建：

```text
src/main/resources/application.yml
src/main/resources/application-druid.yml
```

## 4. 数据库初始化

建议在全新的本地数据库中执行初始化脚本。`ry_20210924.sql` 和 `quartz.sql` 会删除并重建对应的系统表，不能直接用于包含重要数据的数据库。

先创建数据库：

```sql
CREATE DATABASE ruoyi
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

在仓库根目录启动 MySQL 客户端，按以下顺序执行：

```sql
USE ruoyi;
SOURCE E:/Coffee/coffee/sql/ry_20210924.sql;
SOURCE E:/Coffee/coffee/sql/quartz.sql;
SOURCE E:/Coffee/coffee/sql/coffee_all_business.sql;
```

`coffee_all_business.sql` 会继续加载以下装修平台脚本：

```text
sql/coffee_theme_decorator.sql
sql/coffee_theme_scope_v2.sql
```

如果需要完整的 SkinConfig V1 模板和背景插槽，再执行：

```sql
SOURCE E:/Coffee/coffee/sql/coffee_skin_v1.sql;
```

本地演示数据是可选的，只能在开发或测试数据库执行：

```sql
SOURCE E:/Coffee/coffee/sql/coffee_test_data.sql;
```

生产环境更新数据库时，应先备份，再编写并验证增量迁移脚本。不要直接重新执行会删除系统表的初始化脚本。

## 5. 后端配置

### 5.1 `application.yml`

创建 `src/main/resources/application.yml`，至少包含以下内容。可以根据部署环境补充日志、上传目录、微信和 COS 配置。

```yaml
ruoyi:
  name: 咖啡小程序后台
  version: 1.0.0
  profile: ${RUOYI_PROFILE:D:/RuoYi/uploadPath}
  addressEnabled: false

server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /

spring:
  profiles:
    active: druid
  thymeleaf:
    cache: false
  jackson:
    time-zone: GMT+8
    date-format: yyyy-MM-dd HH:mm:ss
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB

mybatis:
  typeAliasesPackage: com.ruoyi.project.**.domain
  mapperLocations: classpath:mybatis/**/*Mapper.xml
  configLocation: classpath:mybatis/mybatis-config.xml

pagehelper:
  helperDialect: mysql
  supportMethodsArguments: true
  params: count=countSql

user:
  password:
    maxRetryCount: 5

shiro:
  user:
    loginUrl: /login
    unauthorizedUrl: /unauth
    indexUrl: /index
    captchaEnabled: true
    captchaType: math
  cookie:
    domain:
    path: /
    httpOnly: true
    maxAge: 30
    cipherKey:
  session:
    expireTime: 30
    dbSyncPeriod: 1
    validationInterval: 10
    maxSession: -1
    kickoutAfter: false
  rememberMe:
    enabled: true

xss:
  enabled: true
  excludes:
  urlPatterns: /system/*,/monitor/*,/tool/*

swagger:
  enabled: true

wx:
  miniapp:
    app-id: ${WX_APP_ID:}
    app-secret: ${WX_APP_SECRET:}
    token-secret: ${WX_TOKEN_SECRET:}
    subscribe:
      pickup-template-id: ${WX_PICKUP_TEMPLATE_ID:}

cos:
  bucket: ${COS_BUCKET:}
  region: ${COS_REGION:}
  secret-id: ${COS_SECRET_ID:}
  secret-key: ${COS_SECRET_KEY:}
  base-url: ${COS_BASE_URL:}
  key-prefix: ${COS_KEY_PREFIX:coffee}

decorator:
  preview:
    h5-url: ${DECORATOR_PREVIEW_H5_URL:}

ai:
  image:
    provider: ${AI_IMAGE_PROVIDER:images-edits}
    base-url: ${AI_IMAGE_BASE_URL:}
    endpoint: ${AI_IMAGE_ENDPOINT:/v1/images/edits}
    api-key: ${AI_IMAGE_API_KEY:}
    model: ${AI_IMAGE_MODEL:gpt-image-2}
    timeout-seconds: ${AI_IMAGE_TIMEOUT_SECONDS:300}
    max-source-image-bytes: ${AI_IMAGE_MAX_SOURCE_BYTES:10485760}
```

### 5.2 `application-druid.yml`

创建 `src/main/resources/application-druid.yml`：

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      master:
        url: ${DB_URL:jdbc:mysql://127.0.0.1:3306/ruoyi?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8}
        username: ${DB_USERNAME:root}
        password: ${DB_PASSWORD:}
      slave:
        enabled: false
        url:
        username:
        password:
      initialSize: 5
      minIdle: 10
      maxActive: 20
      maxWait: 60000
      timeBetweenEvictionRunsMillis: 60000
      minEvictableIdleTimeMillis: 300000
      maxEvictableIdleTimeMillis: 900000
      validationQuery: SELECT 1
      testWhileIdle: true
      testOnBorrow: false
      testOnReturn: false
      statViewServlet:
        enabled: true
        url-pattern: /druid/*
        allow: 127.0.0.1
        login-username: ${DRUID_LOGIN_USERNAME:ruoyi}
        login-password: ${DRUID_LOGIN_PASSWORD:change-me}
```

## 6. 启动后端

在仓库根目录执行：

```powershell
mvn clean package -DskipTests
mvn spring-boot:run
```

也可以在 IDE 中运行：

```text
src/main/java/com/ruoyi/RuoYiApplication.java
```

默认地址：

```text
管理端：http://localhost:8080
登录页：http://localhost:8080/login
Swagger：http://localhost:8080/swagger-ui/index.html
Druid：http://localhost:8080/druid/
```

后台账号由 `sql/ry_20210924.sql` 初始化。首次登录后应立即修改默认密码。

## 7. 启动小程序和 H5

### 7.1 npm/H5

```powershell
cd RuoYi-AbuCoder-UniAppWx/Ruoyi-AbuCoder-UniApp-WX
npm install
npm run dev:h5
```

H5 默认地址：

```text
http://localhost:18081
```

H5 开发代理默认把 `/api`、`/wxapi`、`/profile` 和 `/common` 转发到 `http://127.0.0.1:8080`。后端不在本机时，可在启动前设置：

```powershell
$env:H5_API_PROXY = "http://后端地址:8080"
npm run dev:h5
```

### 7.2 微信小程序

1. 使用 HBuilderX 打开 `RuoYi-AbuCoder-UniAppWx/Ruoyi-AbuCoder-UniApp-WX`。
2. 确认 `manifest.json` 中配置了自己的微信小程序 AppID。
3. 将项目运行到微信开发者工具，或使用 HBuilderX 的发行功能生成微信小程序代码。
4. 打开 `utils/apiconfig.js`，将 `DEFAULT_BASE_URL` 改为后端可访问地址。

电脑本机调试可以使用：

```javascript
let DEFAULT_BASE_URL = 'http://127.0.0.1:8080'
```

手机或局域网调试不能使用 `localhost`，应改成电脑局域网 IP，例如：

```javascript
let DEFAULT_BASE_URL = 'http://192.168.1.10:8080'
```

## 8. 核心 API 路由

以下路由均以 `http://localhost:8080` 为基地址。需要用户身份的接口通过微信用户 Token 或后台会话认证。

### 8.1 微信和公共接口

| 方法 | 路由 | 作用 |
| --- | --- | --- |
| `POST` | `/wxapi/wxlogin` | 微信登录并获取用户会话 |
| `GET` | `/wxapi/me` | 获取当前微信用户 |
| `POST` | `/wxapi/saveUserInfo` | 保存用户资料 |
| `POST` | `/wxapi/uploadAvatar` | 上传微信用户头像 |
| `GET` | `/wxapi/loadBanner` | 查询小程序轮播图 |
| `POST` | `/common/upload` | 上传文件 |
| `GET` | `/common/download` | 下载文件 |

### 8.2 商城和会员

| 模块 | 主要路由 |
| --- | --- |
| 商品 | `GET /api/product/categories`、`GET /api/product/list`、`GET /api/product/{productId}` |
| 购物车 | `GET /api/cart/list`、`POST /api/cart/add`、`PUT /api/cart/update`、`DELETE /api/cart/{cartId}` |
| 订单 | `GET /api/order/list`、`POST /api/order/create`、`PUT /api/order/pay/{orderId}`、`PUT /api/order/cancel/{orderId}`、`PUT /api/order/confirm/{orderId}` |
| 售后 | `POST /api/order/refund/apply`、`GET /api/order/refund/{orderId}` |
| 收货地址 | `GET /api/address/list`、`POST /api/address/add`、`PUT /api/address/update`、`PUT /api/address/setDefault/{addressId}` |
| 会员 | `GET /api/member/info`、`GET /api/member/level-config` |
| 钱包 | `GET /api/wallet/info`、`GET /api/wallet/log`、`GET /api/wallet/recharge/templates`、`GET /api/wallet/recharge/records` |
| 活动 | `GET /api/activity/list`、`POST /api/activity/preview` |
| 线下活动 | `GET /api/offlineActivity/list`、`GET /api/offlineActivity/{activityId}`、`POST /api/offlineActivity/signup`、`GET /api/offlineActivity/my` |

### 8.3 扫码点单

| 模块 | 主要路由 |
| --- | --- |
| 点单菜单 | `GET /api/scanMenu/categories`、`GET /api/scanMenu/products`、`GET /api/scanMenu/products/{productId}` |
| 桌台解析 | `GET /api/scanMenu/table/parse` |
| 扫码购物车 | `GET /api/scanCart/list`、`POST /api/scanCart/add`、`PUT /api/scanCart/update`、`DELETE /api/scanCart/{id}` |
| 扫码订单 | `POST /api/scanOrder/create`、`GET /api/scanOrder/list`、`GET /api/scanOrder/{orderId}`、`PUT /api/scanOrder/pay/{orderId}`、`POST /api/scanOrder/urge/{orderId}` |
| 扫码退款 | `POST /api/scanOrder/refund/apply`、`GET /api/scanOrder/refund/{orderId}` |
| 桌台二维码 | `POST /api/scanTableQrcode/generate`、`POST /api/scanTableQrcode/batchGenerate`、`GET /api/scanTableQrcode/download/{id}` |

### 8.4 商家装修和主题

| 方法 | 路由 | 作用 |
| --- | --- | --- |
| `GET` | `/coffee/decorator/workbench` | 打开 PC 装修工作台 |
| `GET` | `/coffee/decorator/stores` | 查询可管理门店 |
| `GET` | `/coffee/decorator/templates` | 查询系统主题模板 |
| `GET/POST` | `/coffee/decorator/themes` | 查询或创建装修方案 |
| `GET/PUT` | `/coffee/decorator/themes/{themeId}/draft` | 获取或保存草稿 |
| `POST` | `/coffee/decorator/themes/{themeId}/validate` | 发布前校验 |
| `POST` | `/coffee/decorator/themes/{themeId}/publish` | 发布主题版本 |
| `GET` | `/coffee/decorator/themes/{themeId}/versions` | 查询版本历史 |
| `POST` | `/coffee/decorator/themes/{themeId}/versions/{versionId}/restore` | 恢复历史版本 |
| `GET/POST/DELETE` | `/coffee/decorator/assets` | 查询、上传和删除装修素材 |
| `GET` | `/api/wx/stores/{storeCode}/skin` | 小程序读取门店皮肤 |
| `GET` | `/api/mini/skin` | 小程序读取当前主题 |
| `GET` | `/api/wx/skin/preview` | 预览装修主题 |

## 9. 核心服务

后端采用 Controller、Service、Mapper 分层，主要业务服务如下：

| 服务 | 主要职责 |
| --- | --- |
| `TProductServiceImpl` | 商品、分类、库存和商品图片 |
| `TCartServiceImpl` | 商城购物车增删改查 |
| `TOrderServiceImpl` | 商城订单创建、金额重算、支付、取消和确认收货 |
| `OrderRefundServiceImpl` | 商城订单退款申请和处理 |
| `WalletService` | 余额、消费流水、充值模板和充值记录 |
| `MemberService` | 会员等级和折扣配置 |
| `ScanOrderServiceImpl` | 扫码订单、取餐号、履约状态和催单 |
| `ScanOrderRefundServiceImpl` | 扫码订单退款 |
| `ScanTableQrcodeServiceImpl` | 桌台二维码生成和下载 |
| `DecoratorThemeService` | 装修方案、草稿、发布、版本恢复和门店绑定 |
| `DecoratorAssetService` | 装修素材上传、引用和权限校验 |
| `DecoratorPreviewService` | H5/真机预览会话和预览快照 |
| `UserProfileService` | 用户行为画像计算 |
| `ProductRecommendationService` | 基于画像和行为的商品推荐 |
| `ImageAiService` / `ImageAiBatchService` | AI 图片润色、批量任务和任务状态查询 |
| `WxAccessTokenService` | 微信接口访问令牌 |
| `ScanOrderSubscribeMessageService` | 扫码订单订阅消息 |

管理端页面对应的主要路由前缀为：

```text
/coffee/product          商品管理
/coffee/order            商城订单管理
/coffee/scanProduct      扫码商品管理
/coffee/scanOrder        扫码订单履约
/coffee/scanTable        桌台二维码管理
/coffee/member           会员管理
/coffee/rechargeTemplate 充值模板
/coffee/walletLog        余额流水
/coffee/offlineActivity 线下活动管理
/coffee/decorator        商家装修工作台
/monitor/job             定时任务
/system                  用户、角色、菜单和系统配置
```

## 10. 核心功能

- 微信小程序登录、用户信息和头像管理。
- 商品分类、商品详情、库存、商品图片和上下架管理。
- 商城购物车、收货地址、订单创建、余额支付、取消、确认收货和退款申请。
- 会员等级、折扣、钱包余额、消费流水和充值模板。
- 扫码识别桌台、扫码菜单、扫码购物车、扫码订单、取餐号、催单和订单履约。
- 商家后台接单、制作、叫号、完成和退款处理。
- 满减和指定商品营销活动，以及线下活动发布、报名和取消报名。
- 商家多门店装修，主题草稿、版本发布、回滚、门店独立主题和跟随主主题。
- 装修素材管理、H5 实时预览、真机预览和 SkinConfig V1 主题配置。
- 用户行为记录、用户画像和商品推荐。
- AI 商品图片润色和异步批量处理。
- Quartz 定时任务、日志、缓存、在线用户和 Druid 数据源监控。

## 11. 测试和构建

运行后端测试：

```powershell
mvn test
```

只构建后端 JAR：

```powershell
mvn clean package -DskipTests
```

前端构建 H5：

```powershell
cd RuoYi-AbuCoder-UniAppWx/Ruoyi-AbuCoder-UniApp-WX
npm run build:h5
```

数据库测试使用 `src/test/resources/mapper-test-schema.sql` 和 H2，不要把测试数据库脚本当作生产初始化脚本。
