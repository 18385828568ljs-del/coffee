# AI 卡片功能启用说明

## 1. 执行数据库脚本

先备份数据库，再在项目当前使用的 MySQL 数据库中执行：

```sql
source sql/coffee_ai_card.sql;
```

脚本只新增 4 张业务表和 `2300-2306` 菜单权限，不会修改现有订单、商品或会员数据。脚本使用 `CREATE TABLE IF NOT EXISTS` 和 `INSERT IGNORE`，可安全重复执行。

## 2. 配置 AI API

推荐通过环境变量填写，不要把真实 Key 提交到 Git：

```powershell
$env:NEWAPI_API_KEY = "你的AI API Key"
$env:AI_CARD_ENABLED = "true"
```

如果你的网关地址或模型不同，再配置：

```powershell
$env:AI_IMAGE_BASE_URL = "https://你的网关地址"
$env:AI_IMAGE_ENDPOINT = "/v1/images/edits"
$env:AI_IMAGE_MODEL = "你的图片模型名称"
```

`gpt-image-2` 使用 `images-edits` 适配器。请求会把参考图作为 `images[].image_url` Data URL 发送，响应兼容 Base64 图片和 HTTP 图片地址。只有明确支持图片输出的普通多模态模型才使用 `chat-completions` 适配器。

生产环境建议参数：

```powershell
$env:AI_CARD_CONCURRENCY = "2"
$env:AI_CARD_MAX_ATTEMPTS = "3"
$env:AI_CARD_STALE_MINUTES = "15"
```

环境变量设置后需要重启后端。Windows 服务、Docker 或面板部署时，应在对应服务/容器的环境变量中填写，而不是只在临时 PowerShell 窗口中填写。

## 3. 检查 COS

AI 插画和最终卡片都上传到项目现有腾讯云 COS。确认 `cos.bucket`、`cos.region`、`cos.secret-id`、`cos.secret-key` 和 `cos.base-url` 可用，并将 COS/CDN 域名加入微信小程序的下载图片合法域名。

服务器建议安装 `Noto Sans CJK SC` 字体；未安装时渲染器会依次回退到微软雅黑、苹方和 Java SansSerif。

## 4. 商家后台使用流程

1. 登录后台，进入“咖啡商城 -> AI卡片”。
2. 新建活动，填写标题、入口图和活动时间，先保持草稿。
3. 点击“卡片”，新增 1 张或多张候选卡。
4. 动态填写标题、左右商品、风味描述、品牌、Logo、插画主题、配色和抽中权重。
5. 保存后点击“AI生成”。后台任务可离开页面运行，服务重启后也会从数据库恢复。
6. 状态变成“待发布”后检查成品图。
7. 修改文字后点击“重新合成”，不会调用 AI；点击“重新生成”才会再次产生图片 API 费用。
8. 返回活动列表点击“发布”。发布后，小程序首页自动出现抽卡入口。

每位微信用户在每个活动中只能抽取一次。抽卡时不调用 AI，只读取已经发布到 COS 的成品卡片。

## 5. 小程序页面

- 抽卡：`/pages/card/draw`
- 我的卡片：`/pages/card/mine`

发布小程序前，重新使用 HBuilderX/微信开发者工具构建，并确认后端 API 域名和 COS 下载域名都已加入微信公众平台合法域名。
