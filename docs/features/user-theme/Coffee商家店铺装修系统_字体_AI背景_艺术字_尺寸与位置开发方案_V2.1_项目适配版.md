# Coffee 商家店铺装修系统开发方案
## 字体资源、AI 背景、艺术字、尺寸与位置约束（项目适配版）

> 文档版本：V2.1（项目适配修订版）  
> 修订日期：2026-08-17  
> 适用工程：`coffee-mall-admin` 后端 + `Ruoyi-AbuCoder-UniApp-WX` 顾客端  
> 修订依据：当前仓库源码、数据库脚本和原始 V2.0 方案。  
> 重要说明：本文件是开发方案，不表示所有目标功能已经完成。

## 1. 本次修订结论

原 V2.0 方案把“已存在的基础设施”和“后续规划功能”混写在一起。本版先固定当前事实：

| 能力 | 当前状态 | 代码依据 |
|---|---|---|
| 商家/门店装修主题 | 已实现 | `src/main/java/com/ruoyi/project/coffee/decorator/` |
| SkinConfig V1、草稿、版本、发布、恢复 | 已实现 | `DecoratorThemeService.java`、`ThemeConfigValidator.java` |
| H5 iframe 实时预览、真机预览 | 已实现 | `workbench.html`、`theme/preview-bridge.js` |
| 组件背景素材上传和引用 | 已实现 | `DecoratorAssetService.java`、`theme/skin-registry.js` |
| 21 个 typography 角色 | 已实现 | `theme/typography-registry.js` |
| `fontSize/fontWeight/lineHeight/letterSpacing` | 已实现 | 前端注册表、工作台和服务端校验 |
| `fontFamily/textShadow` 字符串样式 | 已实现但不资源化 | 当前允许写入安全 CSS 字符串，没有字体文件库和加载器 |
| 平台字体资源、`fontId`、字体加载 | 未实现 | 没有 `font_resources`、Font API 或 `font-loader.js` |
| 商品主图 AI 润色 | 已实现，但与装修 AI 独立 | `project/coffee/image/`、`/coffee/imageAi/*` |
| 装修组件背景 AI 生成 | 未实现 | 只有 SQL 表和设计文档，没有 decorator AI Service/Controller |
| AI 艺术字 | 未实现 | 没有 ART_TEXT 素材流程、配置字段和渲染层 |
| 自由尺寸、自由坐标、拖拽布局 | 明确不支持 | `ThemeConfigValidator` 递归拒绝布局字段 |
| LayoutPreset/SafeAreaPreset | 未实现 | 当前只有固定组件尺寸和固定 CSS |

因此，本方案的开发顺序必须是：先对齐现有数据和注册表，再增量开发字体、插槽约束、装修 AI 和艺术字；不能直接按原 V2.0 的“最终架构”假设接口已经存在。

## 2. 当前系统基线

```text
商家 PC 工作台
    ↓  /coffee/decorator/*
TenantContext + DecoratorPermission
    ↓
ThemeConfigValidator
    ↓
theme_drafts（revision 乐观锁）
    ↓
H5 实时预览 / 真机预览
    ↓
theme_versions（不可变版本）
    ↓
store_theme_bindings
    ↓  /api/mini/skin?storeId=...
CustomerThemeController
    ↓
themeRuntime + normalizeSkinConfigV1
    ↓
首页、点单、购物车、结算、我的、TabBar
```

主要入口：

```text
后端装修：src/main/java/com/ruoyi/project/coffee/decorator/
装修工作台：src/main/resources/templates/coffee/decorator/workbench.html
主题协议：src/main/resources/schema/theme-config-v1.schema.json
小程序主题：RuoYi-AbuCoder-UniAppWx/Ruoyi-AbuCoder-UniApp-WX/theme/
素材存储：src/main/java/com/ruoyi/project/common/storage/
商品图片 AI：src/main/java/com/ruoyi/project/coffee/image/
装修数据库：sql/coffee_theme_decorator.sql
Skin 增量：sql/coffee_skin_v1.sql
```

### 2.1 不能混淆的两套 AI

项目中已有的 AI 是“商品图片润色”：商家提供商品主图，服务调用图生图接口，生成一张新的商品主图，再由商家点击应用。

本方案要新增的是“装修素材 AI”：商家选择一个注册的组件背景插槽或艺术字场景，生成候选素材并写入装修主题。两者必须保持独立：

```text
商品图片 AI：/coffee/imageAi/* → 商品表 imageUrl/imageUrls

装修素材 AI：/coffee/decorator/ai/* → assets → SkinConfig
```

商品图片 AI 可以复用底层模型客户端的 HTTP 能力，但不能直接复用 `ImageAiService` 的业务语义。后者只接受商品 ID 和原图 URL，不知道租户、Slot、Safe Area、候选图和主题草稿。

## 3. 现有功能的准确说明

### 3.1 现有 SkinConfig 和文字系统

当前 SkinConfig V1 的 `typography` 共有 21 个角色。每个角色实际允许：

```text
color
fontSize        10~32
fontWeight      400/500/600/700/800
lineHeight      1~2
letterSpacing   0~4
fontFamily      最多 120 字符的安全 CSS 字符串
textShadow      最多 120 字符的安全 CSS 字符串
```

对应实现：

- 前端注册和 CSS 变量：`RuoYi-AbuCoder-UniAppWx/Ruoyi-AbuCoder-UniApp-WX/theme/typography-registry.js`；
- 工作台字段：`src/main/resources/templates/coffee/decorator/workbench.html` 的 `renderTypographyEditor()`；
- 后端字段白名单：`src/main/java/com/ruoyi/project/coffee/decorator/theme/ThemeConfigValidator.java`；
- JSON Schema：`src/main/resources/schema/theme-config-v1.schema.json`。

当前的 `fontFamily` 只是 CSS 字体族字符串。它不会上传字体文件，不会调用字体资源接口，也不会在小程序启动时加载自定义字体。V2.1 不能把它描述成“平台字体已经完成”。

### 3.2 当前背景组件注册表

前端 `theme/skin-registry.js` 和后端 `ThemeConfigValidator.SKIN_COMPONENT_KEYS` 当前包含 11 个 Skin 资源键：

| 页面 | componentKey | 逻辑尺寸（rpx） | 当前渲染 |
|---|---|---:|---|
| 首页 | `homeBanner` | 750×360 | `cover`，最多 10 张 |
| 首页 | `actionCard` | 335×180 | `fill` |
| 首页 | `sectionBanner` | 686×144 | `fill` |
| 首页 | `aboutImage` | 686×1000 | `contain` |
| 点单 | `productCard` | 686×320 | `fill` |
| 点单 | `specPanel` | 686×720 | `fill` |
| 购物车 | `emptyCart` | 686×560 | `fill` |
| 购物车 | `cartPanel` | 686×180 | `fill` |
| 确认订单 | `checkoutBar` | 750×124 | `fill` |
| 我的 | `memberCard` | 686×224 | `fill` |
| 全局 | `tabBar` | 750×112 | `fill` |

注意：`sql/coffee_skin_v1.sql` 当前初始化了 10 个 `component_background_slots`，没有 `aboutImage`；前端和 Java 白名单已有 `aboutImage`。这是现存规格不一致，必须在装修 AI 开发前通过迁移脚本补齐或明确将 `aboutImage` 排除在 AI Slot 之外。

组件尺寸、内部间距和页面位置现在由 Vue 页面及组件 CSS 固定。商家配置中不能出现 `x/y/width/height/position/margin/padding/flex/grid` 等字段，服务端会递归拒绝。

### 3.3 当前素材与文件存储

装修素材上传由 `DecoratorAssetService` 完成，当前允许的 `assetType` 只有：

```text
LOGO
HEADER
COMPONENT_BACKGROUND
```

当前上传校验：

- JPG、PNG、GIF；
- 单文件不超过 10 MB；
- 能被 `ImageIO` 识别并取得宽高；
- 写入 `assets` 元数据并建立草稿/版本引用；
- 主题只保存素材 ID 或受支持 URL，不保存二进制。

`CosFileStorageService` 在 COS 配置完整时上传到腾讯云 COS；配置不完整时回退到若依本地 `uploadPath`。因此原方案中的“统一使用 COS”是目标约束，不是当前所有环境的既成事实。

## 4. 当前商品图片 AI 链路（可复用能力）

### 4.1 配置

`src/main/resources/application.yml` 的 `ai.image` 配置为：

```yaml
ai:
  image:
    provider: chat-completions
    base-url: ${AI_IMAGE_BASE_URL:https://newapi.lxhei.xyz}
    endpoint: /v1/chat/completions
    api-key: ${AI_IMAGE_API_KEY:}
    model: ${AI_IMAGE_MODEL:gpt-image-2}
    timeout-seconds: 300
    max-source-image-bytes: 10485760
```

API Key 通过环境变量注入，不应提交真实密钥。

### 4.2 调用流程

```text
后台商品页
    ↓ POST /coffee/imageAi/generate
ImageAiController
    ↓ 校验 mall/scan 商品权限
ImageAiService
    ↓ 下载原图、限制 10 MB、转 data:image/*;base64
ChatCompletionsImageGenerationClient
    ↓ POST {baseUrl}{endpoint}
解析响应中的 Base64 图片
    ↓
FileStorageService.upload()
    ↓
返回 generatedUrl
    ↓ POST /coffee/imageAi/apply
更新商城商品或扫码商品主图
```

批量接口由 `ImageAiBatchService` 处理，最多 50 张、最多 3 个并发 worker；任务状态当前只保存在进程内 `ConcurrentHashMap`，重启后不会恢复。

### 4.3 复用边界

可以复用：

- `ImageGenerationClient` 的模型调用抽象；
- `ChatCompletionsImageGenerationClient` 的 OpenAI-compatible HTTP 传输；
- `FileStorageService` 的文件落库/上传能力。

不能直接复用：

- 商品 ID 校验；
- 商品主图 `imageUrl/imageUrls` 更新逻辑；
- 只有一张输出图的返回结构；
- 当前批处理的内存任务模型；
- 没有 Slot 尺寸、Safe Area、候选结果和租户主题上下文的接口。

## 5. V2.1 目标范围

### 5.1 字体资源化

目标是把当前“任意安全字体族字符串”升级为“平台字体资源白名单”：

```text
fontId + 受控 fontSize/fontWeight/fontStyle
        ↓
后端返回 fontResources
        ↓
小程序加载字体文件
        ↓
加载失败回退系统字体
```

第一版只支持平台维护字体，不开放商家任意上传字体。原因是字体授权、文件安全、微信端兼容和缓存失效都需要单独治理。

### 5.2 装修组件 AI 背景

第一版只开放 `homeBanner`，完成稳定闭环后再扩展 `sectionBanner`、`actionCard`、`memberCard`。每个 AI 请求必须绑定：

```text
merchantId
storeId（可空）
slotKey
slotSpecVersion
referenceAssetId（可空）
promptVersion
candidateCount
```

AI 只生成背景视觉，不生成页面结构、按钮、价格、Logo、二维码或真实业务文字。

### 5.3 AI 艺术字

艺术字属于透明或半透明图片素材，不属于 typography。第一版只支持：

```text
homeBanner
sectionBanner
```

艺术字配置只保存：

```json
{
  "assetId": 501,
  "placementPreset": "LEFT_CENTER",
  "sizePreset": "MEDIUM"
}
```

不保存任意 `x/y/width/height`。商品名、价格、按钮、订单状态和 Tab 仍然必须是程序真实文字。

## 6. Typography V2 实施方案

### 6.1 新增数据表

新增 `font_resources`，建议字段：

```sql
CREATE TABLE font_resources (
    id BIGINT NOT NULL AUTO_INCREMENT,
    font_code VARCHAR(64) NOT NULL,
    font_name VARCHAR(128) NOT NULL,
    family_name VARCHAR(128) NOT NULL,
    source_type VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    merchant_id BIGINT NULL,
    storage_key VARCHAR(512) NOT NULL,
    preview_storage_key VARCHAR(512) NULL,
    mime_type VARCHAR(80) NOT NULL,
    font_weight INT NOT NULL DEFAULT 400,
    font_style VARCHAR(16) NOT NULL DEFAULT 'normal',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_font_code (font_code),
    KEY idx_font_scope_status (merchant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台和商家字体资源';
```

这张表是建议设计，当前仓库没有该表。应新增独立 SQL 迁移，不修改历史初始化脚本的语义。

### 6.2 SkinConfig 扩展

在现有 21 个角色上增量加入 `fontId` 和受控的 `fontStyle`：

```json
{
  "typography": {
    "bannerTitle": {
      "fontId": 12,
      "color": "#332C28",
      "fontSize": 26,
      "fontWeight": 700,
      "fontStyle": "normal",
      "lineHeight": 1.2,
      "letterSpacing": 0
    }
  }
}
```

兼容规则：

- 没有 `fontId`：继续使用系统字体和现有 `fontFamily`；
- `fontId` 不存在、停用或无权使用：服务端拒绝发布，运行时回退默认字体；
- `fontFamily` 暂时保留读取兼容，工作台新界面不再允许商家自由输入任意字体族；
- `fontStyle` 只允许 `normal`、`italic`。

必须同步修改：

```text
ThemeConfigValidator.java
theme-config-v1.schema.json
SkinConfigDefaults.java
theme/defaults.js
theme/normalize.js
theme/typography-registry.js
theme/runtime.js
workbench.html
CustomerThemeController.java
```

### 6.3 字体接口与小程序加载

建议新增：

```http
GET /coffee/decorator/fonts
GET /api/mini/skin?storeId=...
```

顾客端主题返回：

```json
{
  "fontResources": {
    "12": {
      "familyName": "CoffeeRetro",
      "url": "https://cdn.example.com/fonts/coffee-retro.woff2",
      "weight": 700,
      "style": "normal"
    }
  }
}
```

新增 `theme/font-loader.js`，负责收集 `fontId`、去重、加载、失败回退和缓存。应封装 H5 `FontFace` 与微信端/uni-app 字体加载 API，不能让页面组件各自实现加载逻辑。

## 7. Slot 尺寸、上传与位置方案

### 7.1 Slot 规格唯一来源

当前有两份注册来源：

- 前端显示和渲染：`theme/skin-registry.js`；
- 数据库插槽：`component_background_slots`。

AI 开发前必须先解决两者的差异，尤其是 `aboutImage` 缺失问题。建议以 `skin-registry.js` 的 11 个 `componentKey` 为页面白名单，以数据库 Slot 作为 AI/上传处理规格源，并通过启动校验或测试保证两者键集合一致。

建议为 `component_background_slots` 增加：

```text
logical_width
logical_height
output_width
output_height
aspect_ratio
render_mode
upload_min_width
upload_min_height
max_file_size
ai_enabled
safe_area_json
```

当前表已有 `output_width/output_height/render_mode/safe_area_json`，但没有 `ai_enabled`、最小上传尺寸和最大文件尺寸字段。

### 7.2 当前实际尺寸

逻辑尺寸以 `theme/skin-registry.js` 为准，当前 11 个组件见第 3.2 节。`coffee_skin_v1.sql` 中已初始化的 AI/图片输出尺寸大致为逻辑尺寸的 2 倍，但没有 `aboutImage`。因此不能在方案中再写一套“示例尺寸”并直接作为实现依据。

推荐规则：

```text
页面布局尺寸：logicalWidth × logicalHeight（rpx）
AI/上传输出尺寸：由 Slot 的 outputWidth × outputHeight（px）决定
组件渲染：由 renderMode 决定 cover / contain / stretch
```

如果模型不能直接输出目标比例，由后端完成裁切、缩放、压缩和格式转换，再把最终尺寸写入候选结果。

### 7.3 上传校验

现有 `DecoratorAssetService` 只做类型、10 MB、图片可读性检查。V2.1 需要在同一服务中增加 Slot 约束：

```text
上传
  → 读取 MIME、宽高和文件大小
  → 校验 slotKey 和用途
  → 校验最小尺寸、比例和最大文件
  → 裁切/缩放/压缩
  → FileStorageService（COS 或本地降级）
  → assets
  → theme_asset_refs
```

不要允许商家直接提交任意 URL 绕过素材归属和尺寸校验。

### 7.4 LayoutPreset 与 SafeAreaPreset

当前项目不支持任意位置，V2.1 继续保持这个安全边界。新增的只是预设：

```text
LEFT_CENTER
CENTER
RIGHT_CENTER
```

Theme JSON 只保存：

```json
{
  "layout": {
    "homeBanner": {
      "contentPreset": "RIGHT_CENTER",
      "safeAreaPreset": "RIGHT_CENTER_LARGE"
    }
  }
}
```

预设到 class/flex/alignment 的映射由 `skin-registry.js` 或独立 `layout-registry.js` 固定。Schema、Java Validator、normalize、页面模板和工作台必须使用同一组枚举。禁止保存 CSS 或数值坐标。

## 8. 装修 AI 背景实施方案

### 8.1 新增后端模块

建议新增目录：

```text
src/main/java/com/ruoyi/project/coffee/decorator/ai/
├─ DecoratorAiController.java
├─ DecoratorAiService.java
├─ DecoratorPromptBuilder.java
├─ DecoratorAssetGenerationProvider.java
├─ DecoratorAiTaskMapper.java
├─ domain/
└─ provider/
```

业务层负责租户、Slot、任务、配额、候选和素材；Provider 只负责调用模型。这样不会把 `gpt-image-2` 或某个第三方 URL 写死在装修业务中。

建议接口前缀使用项目已有的 `/coffee/decorator`，不要沿用原方案中不存在的 `/admin/decorator/ai-*`：

```http
POST /coffee/decorator/ai/background/tasks
POST /coffee/decorator/ai/art-text/tasks
GET  /coffee/decorator/ai/tasks/{taskId}
GET  /coffee/decorator/ai/tasks/{taskId}/results
POST /coffee/decorator/ai/results/{resultId}/accept
POST /coffee/decorator/ai/results/{resultId}/apply
```

所有接口必须经过 `TenantContextInterceptor` 和装修权限校验。

### 8.2 任务与候选结果

当前 SQL 已有 `ai_generation_tasks` 和 `ai_generation_results`，但没有 Java Domain、Mapper、Service 或 Controller 使用它们。现有字段可作为基础，但建议增加：

任务：

```text
generation_type       BACKGROUND / ART_TEXT
slot_key
prompt_version
text_content          艺术字文案，可空
style_preset          艺术字风格，可空
placement_preset
size_preset
candidate_count
```

结果：

```text
mime_type
has_alpha
post_processed
accepted_at
expires_at
```

候选图流程：

```text
创建任务
  → PENDING/RUNNING
  → Provider 生成候选
  → 后处理尺寸、比例、透明度和审核
  → ai_generation_results
  → 商家预览
  → accept
  → 转为正式 assets
  → apply 写入草稿
  → 保存、预览、发布
```

不能在 AI 返回后直接修改线上主题，也不能把候选图直接写入 `assets.<componentKey>` 而跳过商家选择。

### 8.3 Provider 复用策略

现有 `ImageGenerationClient` 的输入是商品原图 data URL + 文本 Prompt，输出是一张 Base64 图片。装修 AI 需要的输入更多：

```text
slotKey
slotSpecVersion
targetWidth/targetHeight
referenceAssetId
safeAreaPreset
candidateCount
promptVersion
```

建议新增 `DecoratorAssetGenerationProvider`，底层可以复用 `HutoolImageGenerationHttpTransport`，但不要让装修 AI 直接依赖 `ImageAiService`。第一版可以实现一个 `ChatCompletionsDecoratorProvider`，后续再替换其他 Provider。

### 8.4 Prompt 分层

商家只填写简短的风格描述，平台生成最终 Prompt：

```text
Platform Prompt
  + Scene Prompt
  + Slot Spec
  + Safe Area Prompt
  + Merchant Description
```

背景固定规则：

- 不生成可读文字；
- 不生成按钮、价格、二维码和 Logo；
- 不改变组件结构；
- Safe Area 保持低复杂度；
- 参考图只提取色彩、材质和风格，不复制文字与排版。

Prompt、版本、Provider、模型和错误信息必须可追溯到任务记录。

## 9. AI 艺术字实施方案

### 9.1 数据模型

现有 `assets.asset_type` 不支持 `ART_TEXT`，现有 `DecoratorAssetService` 也只允许三种素材类型。需要新增：

```text
ART_TEXT
TEXTURE（可选）
FRAME（可选）
```

艺术字任务应使用 `generation_type=ART_TEXT`，并记录：

```text
text_content
style_preset
placement_preset
size_preset
transparent_background
```

### 9.2 配置和渲染

新增配置前必须同步更新：

```text
theme-config-v1.schema.json
ThemeConfigValidator.java
SkinConfigDefaults.java
theme/normalize.js
theme/runtime.js
theme/decoration-registry.js
```

推荐配置：

```json
{
  "decorations": {
    "homeBannerArtText": {
      "assetId": 501,
      "placementPreset": "LEFT_CENTER",
      "sizePreset": "MEDIUM"
    }
  }
}
```

页面渲染层必须明确分层：

```text
Background Layer  → 组件背景
Decoration Layer  → 纹理/边框/插画
Art Text Layer    → 艺术字图片
Content Layer     → 真实标题、价格、按钮和业务数据
```

艺术字不能替代商品名称、价格、订单状态、按钮或 Tab 文案。第一版建议让 Banner 的“真实标题”和“艺术字标题”二选一，避免重复显示。

## 10. 工作台改造范围

当前工作台已经支持：

- 选择组件背景素材；
- 上传 `COMPONENT_BACKGROUND`；
- 修改 21 个文字角色的颜色、字号、字重、行高、字距、字体族和阴影；
- 商品主图素材映射；
- H5 实时预览、草稿保存和发布。

V2.1 新增区域建议按阶段打开：

```text
字体资源：字体下拉框、字体预览、加载失败提示
Slot 约束：当前尺寸、目标尺寸、适配方式、AI 是否可用
AI 背景：生成、任务状态、候选图、接受、应用到草稿
位置预设：仅显示允许的 contentPreset/safeAreaPreset
艺术字：文案、风格、候选图、placementPreset、sizePreset
```

按钮调用必须对应真实后端接口。工作台不能先做“假按钮”再等待后端补实现。

实时预览仍沿用：

```text
SKIN_CONFIG_UPDATE
SKIN_PREVIEW_NAVIGATE
SKIN_EDITOR_SELECT
```

新增字体、布局和艺术字后，需要扩展消息中的 `fontResources` 和配置字段，但不能改变已有消息类型语义。

## 11. 推荐数据库迁移

新增独立迁移，例如：

```text
sql/coffee_decorator_v2_assets_fonts_ai.sql
```

迁移内容建议包括：

1. 补齐 `aboutImage` Slot，或明确将它标为不可 AI 生成；
2. 给 `component_background_slots` 增加 `ai_enabled`、最小尺寸、最大文件和比例字段；
3. 给 `assets` 增加 `source_type`、`slot_key`、`slot_spec_version`、`generation_result_id`；
4. 增加 `font_resources`；
5. 扩展 `ai_generation_tasks/results` 的背景/艺术字字段；
6. 增加必要的索引、租户外键和状态枚举约束；
7. 初始化第一版只启用 `homeBanner` 背景 AI。

注意：当前 `assets` 表的 `storage_key` 实际被 `DecoratorAssetService` 写入 `StoredFileInfo.url`，因此后续若要保存 COS objectKey 和公开 URL，必须同时调整 Domain、Mapper、DTO 和素材 URL 解析，不能只加一列。

## 12. 开发阶段

### Phase 0：基线对齐（必须先完成）

- 统一 `skin-registry.js` 与 `component_background_slots` 的组件键；
- 决定 `aboutImage` 是否为 AI Slot；
- 确认逻辑尺寸、输出尺寸、renderMode 和文件限制；
- 补充当前代码的回归测试；
- 明确 COS 与本地存储的环境行为。

### Phase 1：Typography V2

- 建 `font_resources` 和查询接口；
- 增加 `fontId/fontStyle` Schema、Validator、Defaults、normalize；
- 增加字体资源返回结构；
- 实现 `font-loader.js` 和失败回退；
- 工作台改成平台字体下拉框；
- 保留旧 `fontFamily` 读取兼容；
- 完成 H5、微信端、缓存和权限测试。

### Phase 2：Slot 约束

- 完成 Slot 规格迁移；
- 增加上传尺寸、比例和处理规则；
- 统一素材类型、来源和 URL/objectKey；
- 先不开放 AI，确保上传、预览、发布稳定。

### Phase 3：装修 AI Background MVP

只支持 `homeBanner`：

1. `DecoratorAiController`；
2. `DecoratorAiService`；
3. `DecoratorPromptBuilder`；
4. Provider 适配器；
5. 任务和候选结果持久化；
6. 固定输出尺寸和 Safe Area；
7. COS 临时文件和正式素材转换；
8. 工作台候选图预览和接受；
9. 接受结果写入草稿 `assets.homeBanner`；
10. H5、真机预览、发布和回滚。

### Phase 4：位置预设

- 增加 `layout-registry.js`；
- 增加 `contentPreset/safeAreaPreset`；
- 更新 Schema、Validator、normalize、默认值和页面渲染；
- 仅支持 `LEFT_CENTER/CENTER/RIGHT_CENTER` 的白名单组合；
- 不实现自由拖拽和任意数字坐标。

### Phase 5：AI Art Text MVP

只支持 `homeBanner` 和 `sectionBanner`：

- `ART_TEXT` 资源类型；
- 文案长度和字符限制；
- 生成透明背景候选；
- `placementPreset + sizePreset`；
- 候选接受后转正式 Asset；
- `decorations` 配置、预览和发布；
- 禁止用于价格、按钮、Tab 和订单状态。

### Phase 6：扩展与运营

- 扩展 `sectionBanner/actionCard/memberCard` 背景 AI；
- 增加配额、费用、审核、限流和审计；
- 增加候选清理、失败重试和 Provider 降级；
- 再评估 Texture、Frame、Illustration 和高级结构控制。

## 13. 测试清单

### 13.1 字体

- `fontId` 不存在、停用、跨租户时被拒绝；
- 字体加载成功和失败回退；
- 多个角色共用字体只加载一次；
- 老配置无 `fontId` 时保持原样；
- H5、微信端、发布和版本恢复一致。

### 13.2 Slot 和图片

- 非图片、超大文件、过小尺寸和错误比例；
- `cover/contain/stretch` 结果一致；
- Slot 键不存在或用途不匹配；
- `aboutImage` 前后端注册表一致；
- COS 与本地存储两种环境都能读取。

### 13.3 AI 背景

- 任务租户隔离和权限；
- Slot 禁用时拒绝创建；
- 参考素材越权和不存在；
- Provider 超时、空响应、Base64 无效；
- 任务重启后可查询；
- 候选结果尺寸、比例、安全区和审核；
- 接受结果只修改草稿；
- revision 冲突、发布、回滚和素材引用保护。

### 13.4 艺术字和位置

- 中文、英文和长度限制；
- 模型生成多余文字的审核/拒绝；
- 透明背景和非透明背景处理；
- 只允许登记的 Placement/Size Preset；
- 不能写入 x/y/width/height；
- 艺术字与真实标题不重复；
- 艺术字不能出现在价格、按钮、Tab 等禁止角色。

## 14. 明确不做

本版本不做：

- 顾客个人换肤；
- AI 生成完整页面或 Vue/WXML/CSS/JavaScript；
- AI 生成价格、按钮、导航和订单业务文字；
- 商家自由拖拽和任意坐标；
- 商家任意上传字体（首版只做平台字体）；
- Photoshop 式图层编辑；
- 直接在生产主题上覆盖候选图；
- 自研模型、ControlNet 服务或训练流程；
- 把商品图片 AI 接口直接当作装修 AI 业务接口。

## 15. 最终架构

```text
商家 Workbench
      │
      ├── Typography Resource ── font_resources ── font-loader ── themeRuntime
      │
      ├── Upload Asset ── assets ── theme_asset_refs
      │
      ├── AI Background ── ai_generation_tasks/results
      │                         │
      │                         └── accept ── assets
      │
      └── AI Art Text ── ai_generation_tasks/results
                                │
                                └── accept ── ART_TEXT assets
                                      │
                                  SkinConfig
                                      │
                              draft / preview / publish
                                      │
                                  themeRuntime
                                      │
                              微信小程序真实 UI
```

核心边界：

```text
文件：COS 或当前环境的本地存储
元数据：MySQL
主题：只保存 assetId/fontId/preset
背景：固定 Slot + 固定比例 + Safe Area
真实文字：Typography + 程序渲染
艺术字：图片素材 + PlacementPreset + SizePreset
AI：只生成候选素材，不接管业务 UI
```

## 16. 方案执行判断

在当前仓库中，最先可直接开发的是字体资源化和 Slot 规格对齐；装修 AI 和艺术字需要新增后端模块、Mapper、迁移、工作台控件和小程序渲染字段，不能只修改 Prompt 或复用现有商品图 AI 接口。

开发验收必须以“保存草稿 → H5 预览 → 真机预览 → 发布 → 顾客端读取”完整闭环为准。任何只生成图片但没有素材归属、主题引用、版本和回滚的实现，都不算装修系统功能完成。
