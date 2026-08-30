package com.ruoyi.project.coffee.decorator.theme;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.context.DecoratorPermission;
import com.ruoyi.project.coffee.decorator.context.TenantContext;
import com.ruoyi.project.coffee.decorator.context.TenantContextService;
import com.ruoyi.project.coffee.decorator.asset.DecoratorAssetService;
import com.ruoyi.project.coffee.decorator.font.DecoratorFontService;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorThemeMapper;
import com.ruoyi.project.coffee.decorator.theme.domain.DecoratorTheme;
import com.ruoyi.project.coffee.decorator.theme.domain.PublishedStoreTheme;
import com.ruoyi.project.coffee.decorator.theme.domain.StoreThemeSummary;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemeDraft;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemeVersion;
import com.ruoyi.project.coffee.decorator.theme.domain.SystemThemeTemplate;

@Service
public class DecoratorThemeService
{
    @Autowired
    private DecoratorThemeMapper themeMapper;

    @Autowired
    private TenantContextService contextService;

    @Autowired
    private ThemeConfigValidator configValidator;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DecoratorAssetService assetService;

    @Autowired
    private DecoratorFontService fontService;

    @Autowired
    private SkinConfigDefaults skinConfigDefaults;

    public DecoratorTheme masterTheme(TenantContext context)
    {
        contextService.requirePermission(context, DecoratorPermission.THEME_VIEW);
        DecoratorTheme theme = activeTheme(context, "MERCHANT", context.getMerchantId());
        if (theme == null)
        {
            throw new ServiceException("商家统一主题尚未初始化");
        }
        return theme;
    }

    public List<SystemThemeTemplate> templates(TenantContext context)
    {
        contextService.requirePermission(context, DecoratorPermission.THEME_VIEW);
        return themeMapper.selectTemplates();
    }

    public List<DecoratorTheme> themes(TenantContext context, String scopeType, Long scopeId)
    {
        contextService.requirePermission(context, DecoratorPermission.THEME_VIEW);
        String normalizedScope = normalizeScope(scopeType);
        Long normalizedScopeId = scopeId;
        if ("MERCHANT".equals(normalizedScope))
        {
            normalizedScopeId = context.getMerchantId();
        }
        else
        {
            if (normalizedScopeId == null) throw new ServiceException("门店范围不能为空");
            contextService.requireStore(context, normalizedScopeId);
            requireActiveStore(context, normalizedScopeId);
        }
        List<DecoratorTheme> result = themeMapper.selectThemes(context.getMerchantId(), normalizedScope,
                normalizedScopeId);
        return result == null ? Collections.<DecoratorTheme>emptyList() : result;
    }

    /** Returns every accessible scheme whose draft differs from its latest published version. */
    public List<DecoratorTheme> drafts(TenantContext context)
    {
        contextService.requirePermission(context, DecoratorPermission.THEME_VIEW);
        // TenantContext exposes an unmodifiable set. MyBatis OGNL evaluates collection
        // expressions reflectively, which fails on that JDK collection type.
        // Pass a regular list so dynamic SQL can safely evaluate size/foreach nodes.
        List<Long> accessibleStoreIds = new ArrayList<Long>(context.getStoreIds());
        List<DecoratorTheme> result = themeMapper.selectDraftThemes(context.getMerchantId(), accessibleStoreIds);
        return result == null ? Collections.<DecoratorTheme>emptyList() : result;
    }

    @Transactional(rollbackFor = Exception.class)
    public DecoratorTheme createTheme(TenantContext context, String scopeType, Long scopeId,
            String name, String sourceType, Long sourceThemeId, Long sourceTemplateId)
    {
        contextService.requirePermission(context, DecoratorPermission.THEME_EDIT);
        String normalizedScope = normalizeScope(scopeType);
        Long ownerStoreId = null;
        if ("STORE".equals(normalizedScope))
        {
            if (scopeId == null) throw new ServiceException("门店范围不能为空");
            contextService.requireStore(context, scopeId);
            requireActiveStore(context, scopeId);
            ownerStoreId = scopeId;
        }
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty() || normalizedName.length() > 120)
        {
            throw new ServiceException("装修方案名称不能为空且不能超过 120 个字符");
        }

        DecoratorTheme sourceTheme = null;
        ThemeVersion sourceVersion = null;
        JsonNode sourceConfig = null;
        String normalizedSource = sourceType == null ? "ACTIVE_VERSION" : sourceType.trim().toUpperCase();
        if ("THEME".equals(normalizedSource))
        {
            if (sourceThemeId == null) throw new ServiceException("请选择要复制的装修方案");
            sourceTheme = requireTheme(context, sourceThemeId, DecoratorPermission.THEME_VIEW);
            if (!normalizedScope.equals(sourceTheme.getScopeType()))
            {
                throw new ServiceException("只能复制当前装修范围内的方案");
            }
            sourceVersion = themeMapper.selectLatestVersion(context.getMerchantId(), sourceThemeId);
            sourceConfig = sourceVersion == null ? draftConfig(context, sourceThemeId) :
                    configValidator.validate(sourceVersion.getConfigJson());
        }
        else if ("TEMPLATE".equals(normalizedSource))
        {
            SystemThemeTemplate template = themeMapper.selectTemplate(sourceTemplateId);
            if (template == null) throw new ServiceException("系统主题模板不存在");
            sourceConfig = configValidator.validate(template.getConfigJson());
            sourceTemplateId = template.getId();
        }
        else if ("DEFAULT".equals(normalizedSource))
        {
            sourceConfig = skinConfigDefaults.create();
        }
        else if ("ACTIVE_VERSION".equals(normalizedSource))
        {
            sourceTheme = activeTheme(context, normalizedScope, ownerStoreId);
            if (sourceTheme != null)
            {
                sourceVersion = themeMapper.selectLatestVersion(context.getMerchantId(), sourceTheme.getId());
                sourceConfig = sourceVersion == null ? draftConfig(context, sourceTheme.getId()) :
                        configValidator.validate(sourceVersion.getConfigJson());
            }
            if (sourceConfig == null) sourceConfig = skinConfigDefaults.create();
        }
        else
        {
            throw new ServiceException("不支持的方案来源");
        }

        DecoratorTheme theme = new DecoratorTheme();
        theme.setMerchantId(context.getMerchantId());
        theme.setName(normalizedName);
        theme.setScopeType(normalizedScope);
        theme.setOwnerStoreId(ownerStoreId);
        theme.setSourceTemplateId(sourceTemplateId);
        theme.setClonedFromThemeId(sourceTheme == null ? sourceThemeId : sourceTheme.getId());
        theme.setClonedFromVersionId(sourceVersion == null ? null : sourceVersion.getId());
        theme.setStatus("ACTIVE");
        theme.setCreatedBy(context.getUserId());
        themeMapper.insertTheme(theme);

        ThemeDraft draft = new ThemeDraft();
        draft.setMerchantId(context.getMerchantId());
        draft.setThemeId(theme.getId());
        draft.setBasedOnVersionId(sourceVersion == null ? null : sourceVersion.getId());
        draft.setSchemaVersion(schemaVersion(sourceConfig));
        draft.setConfigJson(canonicalJson(sourceConfig));
        draft.setRevision(1);
        draft.setUpdatedBy(context.getUserId());
        themeMapper.insertDraft(draft);
        return theme;
    }

    /**
     * Removes a scheme from the workbench while preserving its immutable
     * versions and asset references for audit/history. A live scheme must be
     * switched away from the store before it can be removed.
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTheme(TenantContext context, Long themeId)
    {
        DecoratorTheme theme = requireTheme(context, themeId, DecoratorPermission.THEME_EDIT);
        DecoratorTheme active = activeTheme(context, theme.getScopeType(),
                "STORE".equals(theme.getScopeType()) ? theme.getOwnerStoreId() : context.getMerchantId());
        if (active != null && themeId.equals(active.getId()))
        {
            throw new ServiceException("当前线上方案不能删除，请先切换到其他方案并发布");
        }
        if (themeMapper.softDeleteTheme(context.getMerchantId(), themeId, context.getUserId()) != 1)
        {
            throw new ServiceException("装修方案不存在或已删除");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(TenantContext context, Long themeId)
    {
        DecoratorTheme theme = requireTheme(context, themeId, DecoratorPermission.THEME_EDIT);
        ThemeDraft draft = themeMapper.selectDraftForUpdate(context.getMerchantId(), themeId);
        if (draft == null)
        {
            return;
        }
        ThemeVersion latest = themeMapper.selectLatestVersion(context.getMerchantId(), themeId);
        if (latest == null)
        {
            DecoratorTheme active = activeTheme(context, theme.getScopeType(),
                    "STORE".equals(theme.getScopeType()) ? theme.getOwnerStoreId() : context.getMerchantId());
            if (active != null && themeId.equals(active.getId()))
            {
                throw new ServiceException("当前线上方案没有可删除的草稿");
            }
            if (themeMapper.softDeleteTheme(context.getMerchantId(), themeId, context.getUserId()) != 1)
            {
                throw new ServiceException("装修草稿不存在或已删除");
            }
            return;
        }
        JsonNode publishedConfig = configValidator.validate(latest.getConfigJson());
        int updated = themeMapper.updateDraft(context.getMerchantId(), themeId, draft.getRevision(),
                canonicalJson(publishedConfig), latest.getSchemaVersion(), context.getUserId(), latest.getId());
        if (updated != 1)
        {
            throw new ThemeConflictException("THEME_DRAFT_CONFLICT: 草稿已被其他人更新");
        }
        ThemeDraft reset = themeMapper.selectDraft(context.getMerchantId(), themeId);
        if (assetService != null && reset != null)
        {
            assetService.replaceDraftReferences(context.getMerchantId(), themeId, reset.getId(), publishedConfig);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public DecoratorTheme initializeMasterTheme(TenantContext context, Long templateId)
    {
        contextService.requirePermission(context, DecoratorPermission.THEME_EDIT);
        DecoratorTheme existing = themeMapper.selectMasterTheme(context.getMerchantId());
        if (existing != null)
        {
            return existing;
        }
        SystemThemeTemplate template = themeMapper.selectTemplate(templateId);
        if (template == null)
        {
            throw new ServiceException("系统主题模板不存在");
        }
        JsonNode config = configValidator.validate(template.getConfigJson());
        DecoratorTheme theme = new DecoratorTheme();
        theme.setMerchantId(context.getMerchantId());
        theme.setName(template.getName());
        theme.setScopeType("MERCHANT");
        theme.setSourceTemplateId(template.getId());
        theme.setStatus("ACTIVE");
        theme.setCreatedBy(context.getUserId());
        themeMapper.insertTheme(theme);

        ThemeDraft draft = new ThemeDraft();
        draft.setMerchantId(context.getMerchantId());
        draft.setThemeId(theme.getId());
        draft.setSchemaVersion(ThemeConfigValidator.SCHEMA_VERSION);
        draft.setConfigJson(canonicalJson(config));
        draft.setRevision(1);
        draft.setUpdatedBy(context.getUserId());
        themeMapper.insertDraft(draft);
        return theme;
    }

    public List<StoreThemeSummary> stores(TenantContext context)
    {
        contextService.requirePermission(context, DecoratorPermission.THEME_VIEW);
        List<StoreThemeSummary> stores = themeMapper.selectStores(context.getMerchantId(),
                context.getMemberId(), context.getStoreScope());
        return stores == null ? Collections.<StoreThemeSummary>emptyList() : stores;
    }

    public ThemeDraft draft(TenantContext context, Long themeId)
    {
        requireTheme(context, themeId, DecoratorPermission.THEME_VIEW);
        ThemeDraft draft = themeMapper.selectDraft(context.getMerchantId(), themeId);
        if (draft == null)
        {
            DecoratorTheme theme = themeMapper.selectTheme(context.getMerchantId(), themeId);
            if (theme == null) throw new ServiceException("主题不存在");
            ThemeVersion published = themeMapper.selectLatestVersion(context.getMerchantId(), themeId);
            JsonNode initialConfig = published == null
                    ? skinConfigDefaults.create()
                    : configValidator.validate(published.getConfigJson());
            draft = new ThemeDraft();
            draft.setMerchantId(context.getMerchantId());
            draft.setThemeId(themeId);
            draft.setBasedOnVersionId(published == null ? null : published.getId());
            draft.setSchemaVersion(schemaVersion(initialConfig));
            draft.setConfigJson(canonicalJson(initialConfig));
            draft.setRevision(0);
            draft.setUpdatedBy(context.getUserId());
        }
        return draft;
    }

    public JsonNode validate(TenantContext context, Long themeId, String configJson)
    {
        requireTheme(context, themeId, DecoratorPermission.THEME_EDIT);
        JsonNode config = configValidator.validate(configJson);
        validateAssetReferences(context.getMerchantId(), config);
        if (fontService != null) fontService.validateReferences(context.getMerchantId(), config);
        return config;
    }

    @Transactional(rollbackFor = Exception.class)
    public ThemeDraft saveDraft(TenantContext context, Long themeId, Integer revision, String configJson)
    {
        requireTheme(context, themeId, DecoratorPermission.THEME_EDIT);
        if (revision == null)
        {
            throw new ThemeConflictException("THEME_DRAFT_CONFLICT: 请刷新后重新保存");
        }
        JsonNode normalized = configValidator.validate(configJson);
        validateAssetReferences(context.getMerchantId(), normalized);
        String canonicalJson = canonicalJson(normalized);
        if (revision == 0)
        {
            ThemeDraft initial = new ThemeDraft();
            initial.setMerchantId(context.getMerchantId());
            initial.setThemeId(themeId);
            initial.setSchemaVersion(schemaVersion(normalized));
            initial.setConfigJson(canonicalJson);
            initial.setRevision(1);
            initial.setUpdatedBy(context.getUserId());
            if (themeMapper.insertDraftIfAbsent(initial) != 1)
            {
                throw new ThemeConflictException("THEME_DRAFT_CONFLICT: 草稿已被其他窗口创建");
            }
            ThemeDraft saved = themeMapper.selectDraft(context.getMerchantId(), themeId);
            if (saved == null) throw new ServiceException("主题草稿不存在");
            if (assetService != null) assetService.replaceDraftReferences(context.getMerchantId(), themeId,
                    saved.getId(), normalized);
            return saved;
        }
        int updated = themeMapper.updateDraft(context.getMerchantId(), themeId, revision,
                canonicalJson, schemaVersion(normalized), context.getUserId(), null);
        if (updated != 1)
        {
            throw new ThemeConflictException("THEME_DRAFT_CONFLICT: 草稿已被其他人更新");
        }
        ThemeDraft saved = themeMapper.selectDraft(context.getMerchantId(), themeId);
        if (saved == null) throw new ServiceException("主题草稿不存在");
        if (assetService != null) assetService.replaceDraftReferences(context.getMerchantId(), themeId,
                saved.getId(), normalized);
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publish(TenantContext context, Long themeId, Integer revision,
            String idempotencyKey, String publishNote)
    {
        DecoratorTheme theme = requireTheme(context, themeId, DecoratorPermission.THEME_PUBLISH);
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty() || idempotencyKey.length() > 96)
        {
            throw new ServiceException("发布幂等键不能为空且不能超过 96 个字符");
        }
        String normalizedKey = idempotencyKey.trim();
        ThemeVersion existing = themeMapper.selectVersionByIdempotencyKey(
                context.getMerchantId(), themeId, normalizedKey);
        if (existing != null)
        {
            return publishResult(existing, 0, true);
        }

        ThemeDraft draft = themeMapper.selectDraftForUpdate(context.getMerchantId(), themeId);
        if (draft == null || revision == null || !revision.equals(draft.getRevision()))
        {
            throw new ThemeConflictException("THEME_DRAFT_CONFLICT: 发布前请刷新草稿");
        }
        JsonNode validConfig = configValidator.validate(draft.getConfigJson());
        validateAssetReferences(context.getMerchantId(), validConfig);
        if (fontService != null) fontService.validateReferences(context.getMerchantId(), validConfig);
        String canonicalJson = canonicalJson(validConfig);
        String configHash = sha256(canonicalJson);
        ThemeVersion latest = themeMapper.selectLatestVersion(context.getMerchantId(), themeId);
        if (latest != null && configHash.equals(latest.getConfigHash()))
        {
            throw new ThemeConflictException("THEME_NOT_CHANGED: 当前草稿与最新发布版本相同");
        }

        ThemeVersion version = new ThemeVersion();
        version.setMerchantId(context.getMerchantId());
        version.setThemeId(themeId);
        version.setVersionNo(themeMapper.selectNextVersionNo(context.getMerchantId(), themeId));
        version.setSourceDraftRevision(draft.getRevision());
        version.setSchemaVersion(schemaVersion(validConfig));
        version.setConfigJson(canonicalJson);
        version.setConfigHash(configHash);
        version.setPublishNote(trimTo(publishNote, 255));
        version.setIdempotencyKey(normalizedKey);
        version.setPublishedBy(context.getUserId());
        themeMapper.insertVersion(version);
        if (assetService != null) assetService.replaceVersionReferences(context.getMerchantId(), themeId,
                version.getId(), validConfig);

        int affected;
        if ("MERCHANT".equals(theme.getScopeType()))
        {
            affected = themeMapper.bindFollowingStores(context.getMerchantId(), themeId,
                    version.getId(), context.getUserId());
        }
        else
        {
            contextService.requireStore(context, theme.getOwnerStoreId());
            StoreThemeSummary store = requireActiveStore(context, theme.getOwnerStoreId());
            affected = themeMapper.bindIndependentStore(context.getMerchantId(), store.getStoreId(),
                    themeId, version.getId(), context.getUserId());
        }
        return publishResult(version, affected, false);
    }

    public List<ThemeVersion> versions(TenantContext context, Long themeId)
    {
        requireTheme(context, themeId, DecoratorPermission.VERSION_VIEW);
        return themeMapper.selectVersions(context.getMerchantId(), themeId);
    }

    public ThemeVersion version(TenantContext context, Long themeId, Long versionId)
    {
        requireTheme(context, themeId, DecoratorPermission.VERSION_VIEW);
        ThemeVersion version = themeMapper.selectVersion(context.getMerchantId(), themeId, versionId);
        if (version == null)
        {
            throw new ServiceException("THEME_VERSION_NOT_FOUND: 版本不存在或无权访问");
        }
        return version;
    }

    @Transactional(rollbackFor = Exception.class)
    public ThemeDraft restore(TenantContext context, Long themeId, Long versionId, Integer revision)
    {
        requireTheme(context, themeId, DecoratorPermission.THEME_EDIT);
        ThemeVersion version = version(context, themeId, versionId);
        ThemeDraft draft = themeMapper.selectDraftForUpdate(context.getMerchantId(), themeId);
        if (draft == null || revision == null || !revision.equals(draft.getRevision()))
        {
            throw new ThemeConflictException("THEME_DRAFT_CONFLICT: 恢复前请刷新草稿");
        }
        int updated = themeMapper.updateDraft(context.getMerchantId(), themeId, revision,
                version.getConfigJson(), version.getSchemaVersion(), context.getUserId(), versionId);
        if (updated != 1)
        {
            throw new ThemeConflictException("THEME_DRAFT_CONFLICT: 草稿已被其他人更新");
        }
        return themeMapper.selectDraft(context.getMerchantId(), themeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DecoratorTheme createIndependentTheme(TenantContext context, Long storeId)
    {
        contextService.requirePermission(context, DecoratorPermission.THEME_SCOPE_CHANGE);
        contextService.requireStore(context, storeId);
        StoreThemeSummary store = requireActiveStore(context, storeId);
        if ("INDEPENDENT".equals(store.getBindingMode()))
        {
            List<DecoratorTheme> existingThemes = themeMapper.selectThemes(
                    context.getMerchantId(), "STORE", storeId);
            if (existingThemes != null && !existingThemes.isEmpty())
            {
                return existingThemes.get(0);
            }
        }
        if (store.getThemeId() == null || store.getPublishedVersionId() == null)
        {
            throw new ServiceException("门店尚无可复制的已发布主题");
        }
        ThemeVersion source = themeMapper.selectVersion(context.getMerchantId(),
                store.getThemeId(), store.getPublishedVersionId());
        if (source == null)
        {
            throw new ServiceException("门店当前发布版本不可用");
        }

        DecoratorTheme theme = new DecoratorTheme();
        theme.setMerchantId(context.getMerchantId());
        theme.setName(store.getStoreName() + "独立装修");
        theme.setScopeType("STORE");
        theme.setOwnerStoreId(storeId);
        theme.setStatus("ACTIVE");
        theme.setCreatedBy(context.getUserId());
        themeMapper.insertTheme(theme);

        ThemeDraft draft = new ThemeDraft();
        draft.setMerchantId(context.getMerchantId());
        draft.setThemeId(theme.getId());
        draft.setBasedOnVersionId(source.getId());
        draft.setSchemaVersion(source.getSchemaVersion());
        draft.setConfigJson(source.getConfigJson());
        draft.setRevision(1);
        draft.setUpdatedBy(context.getUserId());
        themeMapper.insertDraft(draft);
        // Freeze the current live snapshot until the store theme publishes its own V1.
        themeMapper.bindIndependentStore(context.getMerchantId(), storeId, store.getThemeId(),
                source.getId(), context.getUserId());
        return theme;
    }

    @Transactional(rollbackFor = Exception.class)
    public StoreThemeSummary followMaster(TenantContext context, Long storeId)
    {
        contextService.requirePermission(context, DecoratorPermission.THEME_SCOPE_CHANGE);
        contextService.requireStore(context, storeId);
        requireActiveStore(context, storeId);
        DecoratorTheme master = masterTheme(context);
        ThemeVersion latest = themeMapper.selectLatestVersion(context.getMerchantId(), master.getId());
        if (latest == null)
        {
            throw new ServiceException("商家统一主题尚未发布");
        }
        themeMapper.bindStoreToMaster(context.getMerchantId(), storeId, master.getId(),
                latest.getId(), context.getUserId());
        return themeMapper.selectStore(context.getMerchantId(), storeId);
    }

    public PublishedStoreTheme publishedStoreTheme(String storeCode)
    {
        if (storeCode == null || !storeCode.matches("^[A-Za-z0-9_-]{1,64}$"))
        {
            throw new ServiceException("门店主题不存在");
        }
        PublishedStoreTheme result = themeMapper.selectPublishedStoreTheme(storeCode);
        if (result == null)
        {
            throw new ServiceException("门店主题不存在");
        }
        configValidator.validate(result.getConfigJson());
        if (assetService != null) result.setAssetUrls(assetService.assetUrls(result.getMerchantId(), result.getVersionId()));
        return result;
    }

    private DecoratorTheme requireTheme(TenantContext context, Long themeId, DecoratorPermission permission)
    {
        contextService.requirePermission(context, permission);
        DecoratorTheme theme = themeMapper.selectTheme(context.getMerchantId(), themeId);
        if (theme == null)
        {
            throw new ServiceException("主题不存在或无权访问");
        }
        if ("STORE".equals(theme.getScopeType()))
        {
            contextService.requireStore(context, theme.getOwnerStoreId());
        }
        return theme;
    }

    private DecoratorTheme activeTheme(TenantContext context, String scopeType, Long scopeId)
    {
        DecoratorTheme active = themeMapper.selectActiveTheme(context.getMerchantId(), scopeType, scopeId);
        if (active == null && "STORE".equals(scopeType) && scopeId != null)
        {
            StoreThemeSummary binding = themeMapper.selectStore(context.getMerchantId(), scopeId);
            if (binding != null && binding.getThemeId() != null)
            {
                active = themeMapper.selectTheme(context.getMerchantId(), binding.getThemeId());
            }
        }
        if (active == null && "MERCHANT".equals(scopeType)) active = themeMapper.selectMasterTheme(context.getMerchantId());
        return active;
    }

    private JsonNode draftConfig(TenantContext context, Long themeId)
    {
        ThemeDraft draft = themeMapper.selectDraft(context.getMerchantId(), themeId);
        return draft == null ? null : configValidator.validate(draft.getConfigJson());
    }

    private String normalizeScope(String scopeType)
    {
        String normalized = scopeType == null ? "" : scopeType.trim().toUpperCase();
        if (!"MERCHANT".equals(normalized) && !"STORE".equals(normalized))
        {
            throw new ServiceException("装修范围必须是商家统一装修或门店装修");
        }
        return normalized;
    }

    private StoreThemeSummary requireActiveStore(TenantContext context, Long storeId)
    {
        StoreThemeSummary store = themeMapper.selectStore(context.getMerchantId(), storeId);
        if (store == null || !"ACTIVE".equals(store.getStoreStatus()))
        {
            throw new ServiceException("门店不存在、已停用或无权访问");
        }
        return store;
    }

    private Map<String, Object> publishResult(ThemeVersion version, int affectedStores, boolean idempotent)
    {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("versionId", String.valueOf(version.getId()));
        result.put("versionNo", version.getVersionNo());
        result.put("configHash", version.getConfigHash());
        result.put("affectedStores", affectedStores);
        result.put("idempotent", idempotent);
        return result;
    }

    private String canonicalJson(JsonNode value)
    {
        try
        {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("主题配置序列化失败");
        }
    }

    private String schemaVersion(JsonNode config)
    {
        JsonNode value = config == null ? null : config.get("schemaVersion");
        return value != null && value.isIntegralNumber() ? String.valueOf(value.asInt()) : ThemeConfigValidator.SCHEMA_VERSION;
    }

    private void validateAssetReferences(Long merchantId, JsonNode config)
    {
        if (assetService != null)
        {
            assetService.validateReferences(merchantId, config);
            return;
        }
        LinkedHashSet<Long> assetIds = new LinkedHashSet<Long>();
        addAssetId(config.path("brand").get("logoAssetId"), assetIds);
        addAssetId(config.path("brand").get("headerAssetId"), assetIds);
        JsonNode components = config.path("components");
        java.util.Iterator<String> names = components.fieldNames();
        while (names.hasNext())
        {
            addAssetId(components.path(names.next()).path("background").get("assetId"), assetIds);
        }
        if (!assetIds.isEmpty())
        {
            List<Long> ids = new ArrayList<Long>(assetIds);
            if (themeMapper.countUsableAssets(merchantId, ids) != ids.size())
            {
                throw new ServiceException("THEME_ASSET_INVALID: 素材不存在、未审核或无权使用");
            }
        }
    }

    private void addAssetId(JsonNode value, LinkedHashSet<Long> assetIds)
    {
        if (value != null && value.isTextual())
        {
            assetIds.add(Long.valueOf(value.asText()));
        }
    }

    private String sha256(String value)
    {
        try
        {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : hash)
            {
                result.append(String.format("%02x", item & 0xff));
            }
            return result.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String trimTo(String value, int maxLength)
    {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
