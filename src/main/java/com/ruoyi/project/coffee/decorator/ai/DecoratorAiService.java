package com.ruoyi.project.coffee.decorator.ai;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.ai.domain.DecoratorAiResult;
import com.ruoyi.project.coffee.decorator.ai.domain.DecoratorAiTask;
import com.ruoyi.project.coffee.decorator.ai.provider.DecoratorAssetGenerationProvider;
import com.ruoyi.project.coffee.decorator.ai.image.CandidateValidator;
import com.ruoyi.project.coffee.decorator.api.DecoratorAiApplyRequest;
import com.ruoyi.project.coffee.decorator.api.DecoratorAiBatchApplyRequest;
import com.ruoyi.project.coffee.decorator.api.DecoratorAiTaskRequest;
import com.ruoyi.project.coffee.decorator.asset.BackgroundSlotService;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;
import com.ruoyi.project.coffee.decorator.asset.domain.DecoratorAsset;
import com.ruoyi.project.coffee.decorator.ai.profile.ComponentAiProfile;
import com.ruoyi.project.coffee.decorator.ai.profile.ComponentAiProfileService;
import com.ruoyi.project.coffee.decorator.context.DecoratorPermission;
import com.ruoyi.project.coffee.decorator.context.TenantContext;
import com.ruoyi.project.coffee.decorator.context.TenantContextService;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorAiMapper;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorAssetMapper;
import com.ruoyi.project.coffee.decorator.theme.DecoratorThemeService;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemeDraft;
import com.ruoyi.project.common.storage.FileStorageService;
import com.ruoyi.project.common.storage.StoredFileInfo;

@Service
public class DecoratorAiService
{
    private static final String BACKGROUND = "BACKGROUND";
    private static final String ART_TEXT = "ART_TEXT";
    private static final String BACKGROUND_WITH_TEXT = "BACKGROUND_WITH_TEXT";
    private static final String PRODUCT_IMAGE = "PRODUCT_IMAGE";
    private static final Pattern ART_TEXT_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{Zs}\\p{P}&+]+$");
    private static final String INITIAL_SKIN_REFERENCE = "INITIAL_SKIN";

    @Autowired private DecoratorAiMapper aiMapper;
    @Autowired private DecoratorAssetMapper assetMapper;
    @Autowired private BackgroundSlotService slotService;
    @Autowired private TenantContextService contextService;
    @Autowired private DecoratorAssetGenerationProvider provider;
    @Autowired private DecoratorPromptBuilder promptBuilder;
    @Autowired private DecoratorImageProcessor imageProcessor;
    @Autowired private FileStorageService storageService;
    @Autowired private DecoratorThemeService themeService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ComponentAiProfileService profileService;
    @Autowired private CandidateValidator candidateValidator;
    @Autowired @Qualifier("threadPoolTaskExecutor") private ThreadPoolTaskExecutor executor;
    @Value("${decorator.ai.initial-skin-base-url:${decorator.preview.h5-url:}}")
    private String initialSkinBaseUrl;

    private static final java.util.Map<String, String> INITIAL_SKIN_PATHS;
    static
    {
        java.util.Map<String, String> paths = new java.util.LinkedHashMap<String, String>();
        paths.put("shopHeader", "/static/skin/vintage/home-banner.png");
        paths.put("homeBanner", "/static/skin/vintage/home-banner.png");
        paths.put("actionCard", "/static/skin/vintage/action-card.png");
        paths.put("sectionBanner", "/static/skin/vintage/section-banner.png");
        paths.put("aboutImage", "/static/banner/about-us.jpg");
        paths.put("productCard", "/static/skin/vintage/product-card.png");
        paths.put("specPanel", "/static/skin/vintage/spec-panel.png");
        paths.put("emptyCart", "/static/skin/vintage/empty-cart.png");
        paths.put("cartPanel", "/static/skin/vintage/cart-panel.png");
        paths.put("checkoutBar", "/static/skin/vintage/checkout-bar.png");
        paths.put("memberCard", "/static/skin/vintage/member-card.png");
        paths.put("tabBar", "/static/skin/vintage/tab-bar.png");
        INITIAL_SKIN_PATHS = java.util.Collections.unmodifiableMap(paths);
    }

    public DecoratorAiTask createBackground(TenantContext context, DecoratorAiTaskRequest request)
    {
        requireManage(context);
        if (request == null) throw new ServiceException("AI 生成参数不能为空");
        if (request.getPlacementPreset() != null
                && !isOneOf(request.getPlacementPreset(), "LEFT_CENTER", "CENTER", "RIGHT_CENTER"))
            throw new ServiceException("背景安全区预设非法");
        BackgroundSlotSpec slot = slotService.require(request.getSlotKey());
        if (!Boolean.TRUE.equals(slot.getAiEnabled())) throw new ServiceException("该插槽未开放 AI 生成");
        ComponentAiProfile profile = profileService == null ? null : profileService.require(slot.getComponentKey());
        String textMode = request.getTextMode() == null ? (profile == null ? "NO_TEXT" : profile.getDefaultTextMode()) : request.getTextMode();
        if (profile != null)
        {
            if (!"NO_TEXT".equals(textMode)) throw new ServiceException("背景接口仅支持不生成文字，请使用对应文字任务");
            if (!profileService.allows(profile, BACKGROUND)) throw new ServiceException("该组件不支持背景生成");
        }
        String stylePreset = request.getStylePreset() == null ? "" : request.getStylePreset().trim();
        if (stylePreset.isEmpty()) stylePreset = "VINTAGE_COFFEE";
        if (stylePreset.codePointCount(0, stylePreset.length()) > 32) throw new ServiceException("目标风格不能超过 32 个字符");
        request.setStylePreset(stylePreset);
        request.setTextMode(textMode);
        return create(context, request, slot, BACKGROUND);
    }

    public DecoratorAiTask createArtText(TenantContext context, DecoratorAiTaskRequest request)
    {
        requireManage(context);
        if (request == null) throw new ServiceException("AI 生成参数不能为空");
        String text = request.getTextContent() == null ? "" : request.getTextContent().trim();
        if (text.isEmpty()) throw new ServiceException("艺术字文案不能为空");
        if (text.codePointCount(0, text.length()) > 20) throw new ServiceException("艺术字文案不能超过 20 个字符");
        if (!ART_TEXT_PATTERN.matcher(text).matches()) throw new ServiceException("艺术字文案包含不支持的字符");
        if (!isOneOf(request.getPlacementPreset(), "LEFT_CENTER", "CENTER", "RIGHT_CENTER"))
            throw new ServiceException("艺术字位置预设非法");
        if (!isOneOf(request.getSizePreset(), "SMALL", "MEDIUM", "LARGE"))
            throw new ServiceException("艺术字尺寸预设非法");
        if (!isOneOf(request.getStylePreset(), "RETRO", "MINIMAL", "HANDWRITTEN"))
            throw new ServiceException("艺术字风格预设非法");
        BackgroundSlotSpec slot = slotService.require(request.getSlotKey());
        ComponentAiProfile profile = profileService == null ? null : profileService.require(slot.getComponentKey());
        if (profile != null && (!profileService.allows(profile, ART_TEXT) || !profileService.allowsText(profile, "ART_TEXT_LAYER")))
            throw new ServiceException("该组件不支持独立艺术字");
        request.setTextContent(text);
        request.setTextMode("ART_TEXT_LAYER");
        return create(context, request, slot, ART_TEXT);
    }

    public DecoratorAiTask createTask(TenantContext context, DecoratorAiTaskRequest request)
    {
        String type = request == null || request.getGenerationType() == null ? BACKGROUND
                : request.getGenerationType().trim().toUpperCase();
        if (BACKGROUND.equals(type)) return createBackground(context, request);
        if (ART_TEXT.equals(type)) return createArtText(context, request);
        if (BACKGROUND_WITH_TEXT.equals(type)) return createBackgroundWithText(context, request);
        if (PRODUCT_IMAGE.equals(type)) return createProductImage(context, request);
        throw new ServiceException("暂不支持该生成类型");
    }

    private DecoratorAiTask createProductImage(TenantContext context, DecoratorAiTaskRequest request)
    {
        requireManage(context);
        if (request == null || request.getProductId() == null || request.getProductId() <= 0)
            throw new ServiceException("商品主图生成缺少商品编号");
        if (request.getReferenceImageUrl() == null || request.getReferenceImageUrl().trim().isEmpty())
            throw new ServiceException("商品主图生成必须提供原商品图片");
        try
        {
            URI reference = URI.create(request.getReferenceImageUrl().trim());
            if (!reference.isAbsolute() || !("http".equalsIgnoreCase(reference.getScheme()) || "https".equalsIgnoreCase(reference.getScheme())))
                throw new IllegalArgumentException();
        }
        catch (RuntimeException e) { throw new ServiceException("原商品图片地址必须是可访问的 http(s) 地址"); }
        BackgroundSlotSpec slot = slotService.require("productCard");
        ComponentAiProfile profile = profileService == null ? null : profileService.require("productCard");
        if (profile != null && !profileService.allows(profile, BACKGROUND)) throw new ServiceException("商品主图未开放 AI 生成");
        int width = request.getTargetWidth() == null ? 1024 : request.getTargetWidth();
        int height = request.getTargetHeight() == null ? 1024 : request.getTargetHeight();
        if (width < 256 || height < 256 || width > 4096 || height > 4096) throw new ServiceException("商品主图尺寸必须在 256 到 4096 像素之间");
        request.setSlotKey("productCard"); request.setGenerationType(PRODUCT_IMAGE);
        request.setTextMode("NO_TEXT"); request.setTargetWidth(width); request.setTargetHeight(height);
        String stylePreset = request.getStylePreset() == null ? "" : request.getStylePreset().trim();
        if (stylePreset.isEmpty()) stylePreset = "VINTAGE_COFFEE";
        request.setStylePreset(stylePreset);
        return create(context, request, slot, PRODUCT_IMAGE);
    }

    private DecoratorAiTask createBackgroundWithText(TenantContext context, DecoratorAiTaskRequest request)
    {
        requireManage(context);
        if (request == null) throw new ServiceException("AI 生成参数不能为空");
        BackgroundSlotSpec slot = slotService.require(request.getSlotKey());
        ComponentAiProfile profile = profileService.require(slot.getComponentKey());
        if (!profileService.allows(profile, BACKGROUND_WITH_TEXT) || !profileService.allowsText(profile, "EMBEDDED_TEXT"))
            throw new ServiceException("该组件不允许文字直接生成在图片中");
        String text = request.getTextContent() == null ? "" : request.getTextContent().trim();
        if (text.isEmpty()) throw new ServiceException("图片中文字不能为空");
        if (text.codePointCount(0, text.length()) > 20 || !ART_TEXT_PATTERN.matcher(text).matches())
            throw new ServiceException("图片中文字必须为 20 个以内的可用字符");
        if (!isOneOf(request.getPlacementPreset(), "LEFT_CENTER", "CENTER", "RIGHT_CENTER"))
            throw new ServiceException("文字位置预设非法");
        if (request.getSizePreset() == null || request.getSizePreset().trim().isEmpty()) request.setSizePreset("LARGE");
        if (!isOneOf(request.getSizePreset(), "SMALL", "MEDIUM", "LARGE"))
            throw new ServiceException("文字尺寸预设非法");
        request.setTextContent(text); request.setTextMode("EMBEDDED_TEXT");
        String stylePreset = request.getStylePreset() == null ? "" : request.getStylePreset().trim();
        if (stylePreset.isEmpty()) stylePreset = "VINTAGE_COFFEE";
        if (stylePreset.codePointCount(0, stylePreset.length()) > 32) throw new ServiceException("目标风格不能超过 32 个字符");
        request.setStylePreset(stylePreset);
        return create(context, request, slot, BACKGROUND_WITH_TEXT);
    }

    private DecoratorAiTask create(TenantContext context, DecoratorAiTaskRequest request,
            BackgroundSlotSpec slot, String generationType)
    {
        if (request.getStoreId() != null) contextService.requireStore(context, request.getStoreId());
        int count = request.getCandidateCount() == null ? 2 : request.getCandidateCount();
        if (count < 1 || count > 4) throw new ServiceException("候选数量必须在 1 到 4 之间");
        DecoratorAsset reference = null;
        if (request.getReferenceAssetId() != null)
        {
            reference = assetMapper.selectAsset(context.getMerchantId(), request.getReferenceAssetId());
            if (reference == null || !"ACTIVE".equals(reference.getStatus())) throw new ServiceException("参考素材不存在或无权使用");
            if (reference.getSourceStoreId() != null) contextService.requireStore(context, reference.getSourceStoreId());
        }
        DecoratorAiTask task = new DecoratorAiTask();
        task.setMerchantId(context.getMerchantId()); task.setStoreId(request.getStoreId());
        task.setSlotId(slot.getId()); task.setSlotKey(slot.getComponentKey()); task.setSlotSpecVersion(slot.getSpecVersion());
        task.setReferenceAssetId(request.getReferenceAssetId()); task.setGenerationType(generationType);
        task.setProductId(request.getProductId());
        task.setTargetWidth(request.getTargetWidth()); task.setTargetHeight(request.getTargetHeight());
        task.setPromptText(merchantDirection(request)); task.setPromptVersion(DecoratorPromptBuilder.PROMPT_VERSION);
        task.setTextContent(request.getTextContent()); task.setStylePreset(request.getStylePreset());
        ComponentAiProfile profile = profileService == null ? null : profileService.require(slot.getComponentKey());
        task.setAiProfileVersion(profile == null ? null : profile.getProfileVersion());
        task.setTextMode(request.getTextMode() == null ? (profile == null ? "NO_TEXT" : profile.getDefaultTextMode()) : request.getTextMode());
        task.setPlacementPreset(request.getPlacementPreset() == null ? "LEFT_CENTER" : request.getPlacementPreset());
        task.setSizePreset(request.getSizePreset() == null ? "MEDIUM" : request.getSizePreset());
        task.setCandidateCount(count); task.setTransparentBackground(ART_TEXT.equals(generationType));
        task.setProvider("openai-images"); task.setStatus("PENDING"); task.setRequestedBy(context.getUserId());
        task.setPromptText(PRODUCT_IMAGE.equals(generationType) ? promptBuilder.productImage(task, slot)
                : ((BACKGROUND.equals(generationType) || BACKGROUND_WITH_TEXT.equals(generationType))
                ? promptBuilder.background(task, slot) : promptBuilder.artText(task, slot)));
        aiMapper.insertTask(task);
        final String referenceUrl = reference == null
                ? (PRODUCT_IMAGE.equals(generationType) ? request.getReferenceImageUrl() : initialSkinReferenceUrl(request, slot, generationType))
                : reference.getUrl();
        executor.execute(() -> generate(task, slot, referenceUrl));
        return task;
    }

    private String initialSkinReferenceUrl(DecoratorAiTaskRequest request, BackgroundSlotSpec slot, String generationType)
    {
        if (ART_TEXT.equals(generationType)) return null;
        String mode = request.getReferenceMode() == null ? INITIAL_SKIN_REFERENCE : request.getReferenceMode().trim().toUpperCase();
        if (!INITIAL_SKIN_REFERENCE.equals(mode)) return null;
        String path = INITIAL_SKIN_PATHS.get(slot.getComponentKey());
        if (path == null || initialSkinBaseUrl == null || initialSkinBaseUrl.trim().isEmpty()) return null;
        try
        {
            URI base = URI.create(initialSkinBaseUrl.trim());
            if (!base.isAbsolute() || base.getHost() == null) return null;
            return new URI(base.getScheme(), base.getAuthority(), path, null, null).toString();
        }
        catch (Exception e) { return null; }
    }

    private void generate(DecoratorAiTask task, BackgroundSlotSpec slot, String referenceUrl)
    {
        aiMapper.updateTaskStatus(task.getId(), "RUNNING", null);
        try
        {
            for (int index = 1; index <= task.getCandidateCount(); index++)
            {
                int targetWidth = PRODUCT_IMAGE.equals(task.getGenerationType()) ? task.getTargetWidth() : slot.getOutputWidth();
                int targetHeight = PRODUCT_IMAGE.equals(task.getGenerationType()) ? task.getTargetHeight() : slot.getOutputHeight();
                byte[] generated = provider.generate(task.getPromptText(), referenceUrl,
                        Boolean.TRUE.equals(task.getTransparentBackground()), targetWidth, targetHeight);
                int[] sourceSize = imageProcessor.sourceSize(generated);
                DecoratorImageProcessor.ProcessedImage image = PRODUCT_IMAGE.equals(task.getGenerationType())
                        ? imageProcessor.background(generated, targetWidth, targetHeight)
                        : !ART_TEXT.equals(task.getGenerationType())
                        ? imageProcessor.background(generated, slot)
                        : imageProcessor.artText(generated, slot.getOutputWidth(), slot.getOutputHeight());
                candidateValidator.validate(image, slot, ART_TEXT.equals(task.getGenerationType()), targetWidth, targetHeight);
                StoredFileInfo stored = storageService.upload(new BytesFile("candidate-" + task.getId() + "-" + index + ".png", image.getBytes()));
                DecoratorAiResult result = new DecoratorAiResult();
                result.setMerchantId(task.getMerchantId()); result.setTaskId(task.getId()); result.setCandidateNo(index);
                result.setStorageKey(stored.getUrl()); result.setMimeType("image/png"); result.setWidth(image.getWidth());
                result.setHeight(image.getHeight()); result.setByteSize((long) image.getBytes().length);
                result.setSourceWidth(sourceSize[0]); result.setSourceHeight(sourceSize[1]);
                result.setFinalWidth(image.getWidth()); result.setFinalHeight(image.getHeight());
                result.setChecksumSha256(sha256(image.getBytes())); result.setHasAlpha(image.isAlpha());
                result.setPostProcessed(true); result.setTextValidationStatus("NOT_CHECKED");
                result.setTextValidationDetail("OCR/Vision validator is not configured"); result.setStatus("READY");
                result.setExpiresAt(new Date(System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L));
                aiMapper.insertResult(result);
            }
            aiMapper.updateTaskStatus(task.getId(), "SUCCEEDED", null);
        }
        catch (Exception e)
        {
            aiMapper.updateTaskStatus(task.getId(), "FAILED", trim(e.getMessage(), 1000));
        }
    }

    public DecoratorAiTask task(TenantContext context, Long taskId)
    {
        contextService.requirePermission(context, DecoratorPermission.ASSET_VIEW);
        DecoratorAiTask task = aiMapper.selectTask(context.getMerchantId(), taskId);
        if (task == null) throw new ServiceException("AI 任务不存在或无权访问");
        if (task.getStoreId() != null) contextService.requireStore(context, task.getStoreId());
        return task;
    }

    public List<DecoratorAiResult> results(TenantContext context, Long taskId)
    {
        task(context, taskId);
        List<DecoratorAiResult> results = aiMapper.selectResults(context.getMerchantId(), taskId);
        return results == null ? Collections.<DecoratorAiResult>emptyList() : results;
    }

    @Transactional(rollbackFor = Exception.class)
    public DecoratorAsset accept(TenantContext context, Long resultId)
    {
        requireManage(context);
        DecoratorAiResult result = aiMapper.selectResult(context.getMerchantId(), resultId);
        if (result == null) throw new ServiceException("AI 候选不存在或无权访问");
        if (result.getExpiresAt() != null && result.getExpiresAt().before(new Date())) throw new ServiceException("AI 候选已过期");
        if (result.getAcceptedAssetId() != null) return assetMapper.selectAsset(context.getMerchantId(), result.getAcceptedAssetId());
        DecoratorAiTask task = task(context, result.getTaskId());
        if (!"SUCCEEDED".equals(task.getStatus())) throw new ServiceException("AI 任务尚未完成");
        DecoratorAsset asset = new DecoratorAsset();
        asset.setScopeType("MERCHANT"); asset.setMerchantId(context.getMerchantId()); asset.setSourceStoreId(task.getStoreId());
        asset.setAssetType(ART_TEXT.equals(task.getGenerationType()) ? "ART_TEXT" : "COMPONENT_BACKGROUND");
        asset.setName((ART_TEXT.equals(task.getGenerationType()) ? "AI艺术字-" : "AI背景-") + task.getSlotKey());
        asset.setStorageKey(result.getStorageKey()); asset.setMimeType(result.getMimeType()); asset.setWidth(result.getWidth()); asset.setHeight(result.getHeight());
        asset.setByteSize(result.getByteSize()); asset.setChecksumSha256(result.getChecksumSha256());
        asset.setAuditStatus("APPROVED"); asset.setStatus("ACTIVE"); asset.setSourceType("AI");
        asset.setSlotKey(task.getSlotKey()); asset.setSlotSpecVersion(task.getSlotSpecVersion());
        asset.setGenerationResultId(result.getId()); asset.setCreatedBy(context.getUserId());
        assetMapper.insertAsset(asset);
        if (aiMapper.acceptResult(context.getMerchantId(), resultId, asset.getId()) != 1) throw new ServiceException("候选已被接受");
        return assetMapper.selectAsset(context.getMerchantId(), asset.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ThemeDraft apply(TenantContext context, Long resultId, DecoratorAiApplyRequest request)
    {
        if (request == null || request.getThemeId() == null || request.getRevision() == null)
            throw new ServiceException("主题和草稿 revision 不能为空");
        DecoratorAsset asset = accept(context, resultId);
        DecoratorAiResult result = aiMapper.selectResult(context.getMerchantId(), resultId);
        DecoratorAiTask task = task(context, result.getTaskId());
        ThemeDraft draft = themeService.draft(context, request.getThemeId());
        if (!request.getRevision().equals(draft.getRevision())) throw new ServiceException("THEME_DRAFT_CONFLICT: 请刷新后重试");
        try
        {
            ObjectNode config = (ObjectNode) objectMapper.readTree(draft.getConfigJson());
            if (ART_TEXT.equals(task.getGenerationType()))
            {
                ObjectNode decorations = config.with("decorations");
                ObjectNode decoration = decorations.putObject(task.getSlotKey() + "ArtText");
                decoration.put("assetId", asset.getId()); decoration.put("placementPreset", task.getPlacementPreset());
                decoration.put("sizePreset", task.getSizePreset());
                if ("homeBanner".equals(task.getSlotKey())) config.with("content").with("homeBanner").put("visible", false);
            }
            else if (PRODUCT_IMAGE.equals(task.getGenerationType()))
            {
                Long productId = request.getProductId() == null ? task.getProductId() : request.getProductId();
                if (productId == null || productId <= 0) throw new ServiceException("应用商品主图候选缺少商品编号");
                config.with("productImages").put(String.valueOf(productId), asset.getId());
            }
            else if ("homeBanner".equals(task.getSlotKey()))
            {
                JsonNode existing = config.with("assets").get("homeBanner");
                ArrayNode banners = existing != null && existing.isArray() ? (ArrayNode) existing : objectMapper.createArrayNode();
                if (banners.size() == 0) banners.add(asset.getId()); else banners.set(0, objectMapper.getNodeFactory().numberNode(asset.getId()));
                config.with("assets").set("homeBanner", banners);
            }
            else
            {
                config.with("assets").put(task.getSlotKey(), asset.getId());
            }
            return themeService.saveDraft(context, request.getThemeId(), request.getRevision(), objectMapper.writeValueAsString(config));
        }
        catch (ServiceException e) { throw e; }
        catch (Exception e) { throw new ServiceException("应用 AI 素材失败: " + e.getMessage()); }
    }

    @Transactional(rollbackFor = Exception.class)
    public ThemeDraft applyBatch(TenantContext context, DecoratorAiBatchApplyRequest request)
    {
        if (request == null || request.getThemeId() == null || request.getRevision() == null)
            throw new ServiceException("主题和草稿 revision 不能为空");
        Set<Long> resultIds = request.getResultIds() == null
                ? Collections.<Long>emptySet() : new LinkedHashSet<Long>(request.getResultIds());
        resultIds.remove(null);
        if (resultIds.isEmpty()) throw new ServiceException("请至少选择一个 AI 候选");
        if (resultIds.size() > 10) throw new ServiceException("一次最多应用 10 个 AI 候选");

        Long taskId = null;
        for (Long resultId : resultIds)
        {
            DecoratorAiResult result = aiMapper.selectResult(context.getMerchantId(), resultId);
            if (result == null) throw new ServiceException("AI 候选不存在或无权访问");
            DecoratorAiTask task = task(context, result.getTaskId());
            if (!"homeBanner".equals(task.getSlotKey()) || ART_TEXT.equals(task.getGenerationType()))
                throw new ServiceException("只有轮播图背景候选支持批量应用");
            if (taskId == null) taskId = task.getId();
            else if (!taskId.equals(task.getId())) throw new ServiceException("请选择同一生成任务中的候选");
        }

        ThemeDraft draft = themeService.draft(context, request.getThemeId());
        if (!request.getRevision().equals(draft.getRevision()))
            throw new ServiceException("THEME_DRAFT_CONFLICT: 请刷新后重试");
        try
        {
            ObjectNode config = (ObjectNode) objectMapper.readTree(draft.getConfigJson());
            JsonNode existing = config.with("assets").get("homeBanner");
            ArrayNode banners = existing != null && existing.isArray()
                    ? (ArrayNode) existing : objectMapper.createArrayNode();
            if (existing != null && !existing.isNull() && !existing.isArray()) banners.add(existing);
            Set<Long> currentAssetIds = new LinkedHashSet<Long>();
            for (JsonNode banner : banners)
            {
                if (banner.canConvertToLong()) currentAssetIds.add(banner.longValue());
                else if (banner.isTextual() && banner.textValue().matches("\\d+"))
                    currentAssetIds.add(Long.valueOf(banner.textValue()));
            }

            for (Long resultId : resultIds)
            {
                DecoratorAsset asset = accept(context, resultId);
                if (currentAssetIds.add(asset.getId())) banners.add(asset.getId());
            }
            if (banners.size() > 10) throw new ServiceException("轮播图最多 10 张，请先移除部分图片");
            config.with("assets").set("homeBanner", banners);
            return themeService.saveDraft(context, request.getThemeId(), request.getRevision(),
                    objectMapper.writeValueAsString(config));
        }
        catch (ServiceException e) { throw e; }
        catch (Exception e) { throw new ServiceException("批量应用 AI 素材失败: " + e.getMessage()); }
    }

    private void requireManage(TenantContext context)
    {
        contextService.requirePermission(context, DecoratorPermission.ASSET_MANAGE);
        contextService.requirePermission(context, DecoratorPermission.THEME_EDIT);
    }

    private boolean isOneOf(String value, String... allowed)
    { for (String item : allowed) if (item.equals(value)) return true; return false; }

    private String trim(String value, int max)
    { String text = value == null ? "" : value.trim(); return text.length() <= max ? text : text.substring(0, max); }

    private String merchantDirection(DecoratorAiTaskRequest request)
    {
        StringBuilder result = new StringBuilder();
        appendDirection(result, request.getPrompt()); appendDirection(result, request.getDescription());
        appendDirection(result, request.getMerchantDescription());
        if (request.getPrimaryColor() != null) appendDirection(result, "Primary color: " + request.getPrimaryColor());
        if (request.getProductName() != null) appendDirection(result, "Product: " + request.getProductName());
        if (request.getProductDescription() != null) appendDirection(result, "Product description: " + request.getProductDescription());
        if (request.getColorTone() != null) appendDirection(result, "Color tone: " + request.getColorTone());
        if (request.getMainElements() != null) appendDirection(result, "Main elements: " + request.getMainElements());
        if (request.getTexturePreference() != null) appendDirection(result, "Texture: " + request.getTexturePreference());
        return trim(result.toString(), 500);
    }

    private void appendDirection(StringBuilder result, String value)
    {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) return;
        if (result.length() > 0) result.append("; ");
        result.append(text);
    }

    private String sha256(byte[] bytes)
    {
        try { StringBuilder result = new StringBuilder(); for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) result.append(String.format("%02x", value & 0xff)); return result.toString(); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    private static class BytesFile implements MultipartFile
    {
        private final String filename; private final byte[] bytes;
        BytesFile(String filename, byte[] bytes) { this.filename=filename; this.bytes=bytes; }
        public String getName(){return "file";} public String getOriginalFilename(){return filename;}
        public String getContentType(){return "image/png";} public boolean isEmpty(){return bytes.length==0;}
        public long getSize(){return bytes.length;} public byte[] getBytes(){return bytes;}
        public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}
        public void transferTo(File dest) throws IOException { org.apache.commons.io.FileUtils.writeByteArrayToFile(dest, bytes); }
    }
}
