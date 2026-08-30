# 商家装修 AI 生图代码详解

本文按“点击 AI 生成”这一个动作，说明浏览器、Java 后端、外部图片模型、素材库、主题草稿和小程序预览之间的完整链路。

## 1. 总体流程

    点击生成
      -> workbench.html 组装 JSON
      -> POST /coffee/decorator/ai/tasks
      -> DecoratorAiController
      -> DecoratorAiService 创建 PENDING 任务
      -> 异步调用 Provider 和图片模型
      -> 图片处理、尺寸/透明度校验
      -> ai_generation_results 保存候选图
      -> 前端轮询任务并展示候选
      -> 预览 / 接受 / 应用
      -> 写入素材库和主题草稿
      -> 小程序读取草稿配置展示

三个按钮的区别：

| 操作 | 是否修改主题草稿 | 作用 |
|---|---:|---|
| 预览 | 否 | 临时把候选 URL 发给 H5 iframe |
| 接受 | 否 | 把候选转成正式 DecoratorAsset 素材 |
| 应用 | 是 | 把素材 ID 写入主题 configJson |

## 2. 点击“AI 生成”：前端代码

文件：src/main/resources/templates/coffee/decorator/workbench.html，入口函数约在 928 行：startAiTask()。

它先检查当前主题、草稿、ASSET_MANAGE 和 THEME_EDIT 权限，然后读取当前组件和表单值：

    var request = {
      slotKey: productImageEditor ? 'productCard' : component.key,
      generationType: productImageEditor
          ? 'PRODUCT_IMAGE'
          : (embedded ? 'BACKGROUND_WITH_TEXT' : 'BACKGROUND'),
      candidateCount: Number($('#skinAiCount').val()),
      stylePreset: $('#skinAiBackgroundStyle').val(),
      primaryColor: $('#skinAiPrimaryColor').val(),
      textMode: productImageEditor ? 'NO_TEXT'
          : (embedded ? 'EMBEDDED_TEXT' : 'NO_TEXT'),
      referenceMode: productImageEditor ? null : 'INITIAL_SKIN',
      prompt: $('#skinAiPrompt').val(),
      placementPreset: 'CENTER'
    };

商品主图还会传原图和目标尺寸：

    request.productId = selectedProduct.productId || selectedProduct.id;
    request.referenceImageUrl =
        selectedProduct.imageUrl || selectedProduct.productImage;
    request.targetWidth = targetWidth;
    request.targetHeight = targetHeight;

带文字背景会再传 textContent，并把 sizePreset 设为 LARGE。随后发送：

    api('POST', '/ai/tasks', request).done(function (task) {
      state.aiTaskId = task.id;
      pollAiTask(task.id, contextKey);
    });

浏览器看到的完整接口是 POST /coffee/decorator/ai/tasks。工作台统一使用这个接口，后端根据 generationType 分流。候选数服务端限制为 1 到 4，首页轮播最终最多保留 10 张。

## 3. Controller 接口

文件：src/main/java/com/ruoyi/project/coffee/decorator/api/DecoratorAiController.java。

    @RequestMapping("/coffee/decorator/ai")
    public class DecoratorAiController {
        @PostMapping("/tasks")
        public AjaxResult createTask(@RequestBody DecoratorAiTaskRequest request) {
            return AjaxResult.success(
                aiService.createTask(TenantContextHolder.require(), request));
        }

        @GetMapping("/tasks/{taskId}")
        public AjaxResult task(@PathVariable Long taskId) {
            return AjaxResult.success(
                aiService.task(TenantContextHolder.require(), taskId));
        }

        @GetMapping("/tasks/{taskId}/results")
        public AjaxResult results(@PathVariable Long taskId) {
            return AjaxResult.success(
                aiService.results(TenantContextHolder.require(), taskId));
        }

        @PostMapping("/results/{resultId}/accept")
        public AjaxResult accept(@PathVariable Long resultId) {
            return AjaxResult.success(
                aiService.accept(TenantContextHolder.require(), resultId));
        }

        @PostMapping("/results/{resultId}/apply")
        public AjaxResult apply(@PathVariable Long resultId,
                                @RequestBody DecoratorAiApplyRequest request) {
            return AjaxResult.success(
                aiService.apply(TenantContextHolder.require(), resultId, request));
        }

        @PostMapping("/results/apply-batch")
        public AjaxResult applyBatch(@RequestBody DecoratorAiBatchApplyRequest request) {
            return AjaxResult.success(
                aiService.applyBatch(TenantContextHolder.require(), request));
        }
    }

另外两个专用入口是 /background/tasks 和 /art-text/tasks。请求 DTO 在 src/main/java/com/ruoyi/project/coffee/decorator/api/DecoratorAiTaskRequest.java，主要字段为：

| 字段 | 作用 |
|---|---|
| slotKey | 装修插槽，如 homeBanner、sectionBanner、productCard |
| generationType | BACKGROUND、BACKGROUND_WITH_TEXT、ART_TEXT、PRODUCT_IMAGE |
| candidateCount | 候选数量 |
| stylePreset、primaryColor | 风格和主色 |
| textMode、textContent | 文字策略和文字 |
| referenceMode、referenceAssetId | 参考图来源 |
| productId、referenceImageUrl | 商品图对应商品和原图 |
| targetWidth、targetHeight | 商品图输出尺寸 |

## 4. Service 分流和参数校验

文件：src/main/java/com/ruoyi/project/coffee/decorator/ai/DecoratorAiService.java，createTask() 约 141 行：

    String type = request == null || request.getGenerationType() == null
            ? BACKGROUND : request.getGenerationType().trim().toUpperCase();
    if (BACKGROUND.equals(type)) return createBackground(context, request);
    if (ART_TEXT.equals(type)) return createArtText(context, request);
    if (BACKGROUND_WITH_TEXT.equals(type))
        return createBackgroundWithText(context, request);
    if (PRODUCT_IMAGE.equals(type))
        return createProductImage(context, request);
    throw new ServiceException("暂不支持该生成类型");

艺术字 createArtText() 额外检查文案最多 20 个字符、字符白名单、位置 LEFT_CENTER/CENTER/RIGHT_CENTER、尺寸 SMALL/MEDIUM/LARGE，并检查当前组件 Profile 是否允许 ART_TEXT 和 ART_TEXT_LAYER。

通用创建方法会限制候选数量、检查参考素材归属，并记录任务快照：

    int count = request.getCandidateCount() == null
            ? 2 : request.getCandidateCount();
    if (count < 1 || count > 4)
        throw new ServiceException("候选数量必须在 1 到 4 之间");

    task.setMerchantId(context.getMerchantId());
    task.setStoreId(request.getStoreId());
    task.setSlotId(slot.getId());
    task.setSlotKey(slot.getComponentKey());
    task.setSlotSpecVersion(slot.getSpecVersion());
    task.setGenerationType(generationType);
    task.setProductId(request.getProductId());
    task.setTargetWidth(request.getTargetWidth());
    task.setTargetHeight(request.getTargetHeight());
    task.setCandidateCount(count);
    task.setTransparentBackground(ART_TEXT.equals(generationType));
    task.setProvider("openai-images");
    task.setStatus("PENDING");
    task.setRequestedBy(context.getUserId());

任务同时保存 aiProfileVersion、promptVersion、文字模式、位置预设和尺寸预设，方便追溯本次生成使用的规则。

## 5. 组件 Profile 和提示词

文件：src/main/java/com/ruoyi/project/coffee/decorator/ai/profile/ComponentAiProfileService.java。

每个装修组件都有 AI Profile，保存启用状态、允许的生成类型、允许的文字模式和提示词配置：

    public boolean allows(ComponentAiProfile profile, String generationType) {
        return values(profile.getGenerationModes()).contains(generationType);
    }

    public boolean allowsText(ComponentAiProfile profile, String textMode) {
        return values(profile.getAllowedTextModes()).contains(textMode);
    }

因此前端即使手工传入类型，后端也不会允许组件生成不支持的内容。

提示词文件是 src/main/java/com/ruoyi/project/coffee/decorator/ai/DecoratorPromptBuilder.java。背景提示词会拼接组件名、风格、参考图要求、目标宽高、渲染模式、禁止元素和商家方向：

    return "Generate a premium coffee shop "
        + slot.getComponentKey() + " component background in "
        + task.getStylePreset() + " style. "
        + "Target " + slot.getOutputWidth() + "x"
        + slot.getOutputHeight() + " pixels exactly. "
        + "Merchant direction: " + task.getPromptText();

productImage() 强调保留原商品身份、轮廓、比例和可识别配料，禁止价格、按钮、Logo、二维码等 UI。artText() 明确要求只生成精确文案和透明 PNG。

## 6. 异步任务和外部模型

任务先入库，再交给线程池：

    aiMapper.insertTask(task);
    final String referenceUrl = ...;
    executor.execute(() -> generate(task, slot, referenceUrl));
    return task;

generate()（约 260 行）更新任务为 RUNNING，按候选数循环调用 Provider：

    aiMapper.updateTaskStatus(task.getId(), "RUNNING", null);
    for (int index = 1; index <= task.getCandidateCount(); index++) {
        int width = PRODUCT_IMAGE.equals(task.getGenerationType())
            ? task.getTargetWidth() : slot.getOutputWidth();
        int height = PRODUCT_IMAGE.equals(task.getGenerationType())
            ? task.getTargetHeight() : slot.getOutputHeight();
        byte[] generated = provider.generate(
            task.getPromptText(), referenceUrl,
            Boolean.TRUE.equals(task.getTransparentBackground()),
            width, height);
        // 下面继续做处理、校验、上传和候选入库
    }
    aiMapper.updateTaskStatus(task.getId(), "SUCCEEDED", null);

Provider 接口：src/main/java/com/ruoyi/project/coffee/decorator/ai/provider/DecoratorAssetGenerationProvider.java。

    byte[] generate(String prompt, String referenceUrl,
                    boolean transparentBackground,
                    int targetWidth, int targetHeight);

实际 HTTP 在 src/main/java/com/ruoyi/project/coffee/image/client/HutoolImageGenerationHttpTransport.java。它支持 JSON/multipart POST，使用 application.yml 中的 ai.base-url、ai.generation-endpoint、ai.edit-endpoint、ai.api-key、ai.model，最多重试 3 次，并拒绝 HTML 或非 JSON 响应。

## 7. 图片处理和候选入库

文件：src/main/java/com/ruoyi/project/coffee/decorator/ai/DecoratorImageProcessor.java。

普通背景默认 COVER：按比例放大覆盖画布，再从中心裁剪：

    double scale = Math.max(
        (double) targetWidth / input.getWidth(),
        (double) targetHeight / input.getHeight());
    int cropWidth = Math.max(1, (int) Math.round(targetWidth / scale));
    int cropHeight = Math.max(1, (int) Math.round(targetHeight / scale));
    int cropX = Math.max(0, (input.getWidth() - cropWidth) / 2);
    int cropY = Math.max(0, (input.getHeight() - cropHeight) / 2);
    return render(input.getSubimage(cropX, cropY, cropWidth, cropHeight),
                  targetWidth, targetHeight, false);

插槽还支持 CONTAIN（完整主体，空白区边缘延展）和 STRETCH/FILL（直接拉伸）。艺术字会输出带 Alpha 通道的 PNG，逐像素检查是否存在透明像素。

CandidateValidator.java 会检查图片可识别、最终宽高、插槽规则和艺术字透明通道。通过后上传文件并写入 ai_generation_results：

    StoredFileInfo stored = storageService.upload(
        new BytesFile("candidate-" + task.getId() + "-" + index + ".png",
                      image.getBytes()));
    result.setTaskId(task.getId());
    result.setCandidateNo(index);
    result.setStorageKey(stored.getUrl());
    result.setMimeType("image/png");
    result.setWidth(image.getWidth());
    result.setHeight(image.getHeight());
    result.setSourceWidth(sourceSize[0]);
    result.setSourceHeight(sourceSize[1]);
    result.setChecksumSha256(sha256(image.getBytes()));
    result.setHasAlpha(image.isAlpha());
    result.setPostProcessed(true);
    result.setStatus("READY");
    aiMapper.insertResult(result);

SQL 映射在 src/main/resources/mybatis/coffee/decorator/DecoratorAiMapper.xml；表结构脚本是 sql/coffee_decorator_v2_assets_fonts_ai.sql 和 sql/coffee_decorator_v3_component_ai_profiles.sql。

## 8. 前端轮询和候选展示

pollAiTask()（约 960 行）请求 GET /ai/tasks/{taskId}，每 1.5 秒查询一次：

    var labels = {
      PENDING: '等待中', RUNNING: '生成中',
      SUCCEEDED: '已完成', FAILED: '生成失败'
    };
    $('#skinAiStatus').text(labels[task.status] || task.status);
    if (task.status === 'SUCCEEDED')
        return loadAiResults(taskId, contextKey);
    if (task.status === 'FAILED')
        return showEditorError(task.errorMessage || 'AI 生成失败');

loadAiResults() 请求 GET /ai/tasks/{taskId}/results，为每个结果绑定“预览、接受、应用”三个按钮。contextKey 用来防止用户切换组件后，旧任务完成并覆盖当前编辑器。

## 9. 预览、接受、应用的完整区别

### 9.1 预览

previewAiResult() 只复制 state.config，把候选 URL 临时写入 assets、productImages 或 decorations，再发送：

    miniProgramPreview.contentWindow.postMessage({
      type: 'SKIN_CONFIG_UPDATE',
      payload: preview,
      assetUrls: urls,
      fontResources: fontResourceMap()
    }, h5PreviewOrigin);

这一步不写数据库，刷新页面后消失。

### 9.2 接受

前端请求 POST /ai/results/{resultId}/accept。Service 的 accept() 会检查商家权限、候选有效期和任务状态，然后创建正式 DecoratorAsset：

    asset.setAssetType(ART_TEXT.equals(task.getGenerationType())
        ? "ART_TEXT" : "COMPONENT_BACKGROUND");
    asset.setSourceType("AI");
    asset.setAuditStatus("APPROVED");
    asset.setStatus("ACTIVE");
    asset.setGenerationResultId(result.getId());
    assetMapper.insertAsset(asset);
    aiMapper.acceptResult(context.getMerchantId(),
                          resultId, asset.getId());

接受只进入素材库，不自动改变主题草稿。

### 9.3 应用

前端请求 POST /ai/results/{resultId}/apply，并携带 themeId 和当前 revision。Service 先接受候选，再检查 revision，防止覆盖他人刚保存的草稿。然后修改 configJson：

    if (ART_TEXT.equals(task.getGenerationType())) {
        ObjectNode d = config.with("decorations")
            .putObject(task.getSlotKey() + "ArtText");
        d.put("assetId", asset.getId());
        d.put("placementPreset", task.getPlacementPreset());
        d.put("sizePreset", task.getSizePreset());
    } else if (PRODUCT_IMAGE.equals(task.getGenerationType())) {
        config.with("productImages")
            .put(String.valueOf(productId), asset.getId());
    } else if ("homeBanner".equals(task.getSlotKey())) {
        // 单个应用替换轮播第一张
        config.with("assets").withArray("homeBanner")
            .set(0, objectMapper.getNodeFactory()
            .numberNode(asset.getId()));
    } else {
        config.with("assets").put(task.getSlotKey(), asset.getId());
    }
    return themeService.saveDraft(...,
        objectMapper.writeValueAsString(config));

保存成功后，前端重新读取草稿和素材并调用 fillEditor()、renderPreview()，管理端预览立即更新。

## 10. 首页轮播批量应用

applySelectedAiResults() 发送 themeId、revision、resultIds。后端只允许同一任务的 homeBanner 非艺术字候选，并检查最终轮播不超过 10 张。每张候选仍会先转成正式素材，再把素材 ID 追加到 config.assets.homeBanner，最后一次保存草稿。

## 11. 四种类型最终写入哪里

| 类型 | 处理方式 | 草稿字段 |
|---|---|---|
| BACKGROUND | 组件尺寸裁剪/补边 | config.assets[slotKey] |
| BACKGROUND_WITH_TEXT | 普通背景处理，提示词加入精确文案 | config.assets[slotKey] |
| ART_TEXT | 透明 PNG，校验 Alpha | config.decorations[slotKey + 'ArtText'] |
| PRODUCT_IMAGE | 商品目标尺寸处理 | config.productImages[productId] |

商品图只是装修覆盖配置，不会修改商品业务表原始图片。

## 12. 出错定位顺序

1. 浏览器 Network：/ai/tasks 是否返回任务 ID；
2. ai_generation_tasks.status：PENDING/RUNNING/SUCCEEDED/FAILED；
3. error_message：外部模型或图片处理失败原因；
4. Provider/Transport：检查 Base URL、endpoint、Key、model 和 JSON 响应；
5. ImageProcessor/Validator：检查图片格式、尺寸和透明通道；
6. 应用失败：检查草稿 revision，常见错误是 THEME_DRAFT_CONFLICT。

相关测试覆盖任务创建、艺术字限制、组件能力、轮播上限、尺寸裁剪、contain 和透明通道。商家装修测试最近一次为 15 个测试类、77 个用例，0 failures、0 errors。

## 13. 源码索引

- 前端：src/main/resources/templates/coffee/decorator/workbench.html
- 接口：src/main/java/com/ruoyi/project/coffee/decorator/api/DecoratorAiController.java
- 业务：src/main/java/com/ruoyi/project/coffee/decorator/ai/DecoratorAiService.java
- 提示词：src/main/java/com/ruoyi/project/coffee/decorator/ai/DecoratorPromptBuilder.java
- 图片处理：src/main/java/com/ruoyi/project/coffee/decorator/ai/DecoratorImageProcessor.java
- 候选校验：src/main/java/com/ruoyi/project/coffee/decorator/ai/image/CandidateValidator.java
- HTTP：src/main/java/com/ruoyi/project/coffee/image/client/HutoolImageGenerationHttpTransport.java
- SQL：src/main/resources/mybatis/coffee/decorator/DecoratorAiMapper.xml
