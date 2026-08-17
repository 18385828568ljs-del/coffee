package com.ruoyi.project.coffee.decorator.theme;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.context.DecoratorPermission;
import com.ruoyi.project.coffee.decorator.context.TenantContext;
import com.ruoyi.project.coffee.decorator.context.TenantContextService;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorThemeMapper;
import com.ruoyi.project.coffee.decorator.theme.domain.DecoratorTheme;
import com.ruoyi.project.coffee.decorator.theme.domain.PreviewTheme;
import com.ruoyi.project.coffee.decorator.theme.domain.StoreThemeSummary;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemeDraft;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemePreviewSession;
import com.ruoyi.project.coffee.decorator.asset.DecoratorAssetService;
import com.ruoyi.project.coffee.scanOrder.wx.WxaCodeService;

@Service
public class DecoratorPreviewService
{
    private static final long PREVIEW_TTL_MILLIS = 30L * 60L * 1000L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private DecoratorThemeMapper themeMapper;

    @Autowired
    private TenantContextService contextService;

    @Autowired
    private ThemeConfigValidator configValidator;

    @Autowired
    private DecoratorAssetService assetService;

    @Autowired(required = false)
    private WxaCodeService wxaCodeService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(TenantContext context, Long themeId, Long storeId, Integer revision)
    {
        contextService.requirePermission(context, DecoratorPermission.THEME_PREVIEW);
        contextService.requireStore(context, storeId);
        DecoratorTheme theme = themeMapper.selectTheme(context.getMerchantId(), themeId);
        StoreThemeSummary store = themeMapper.selectStore(context.getMerchantId(), storeId);
        if (theme == null || store == null || !"ACTIVE".equals(store.getStoreStatus()))
        {
            throw new ServiceException("THEME_PREVIEW_EXPIRED: 预览资源不存在或无权访问");
        }
        if ("STORE".equals(theme.getScopeType()) && !storeId.equals(theme.getOwnerStoreId()))
        {
            throw new ServiceException("THEME_PREVIEW_EXPIRED: 预览资源不存在或无权访问");
        }
        ThemeDraft draft = themeMapper.selectDraft(context.getMerchantId(), themeId);
        if (draft == null || revision == null || !revision.equals(draft.getRevision()))
        {
            throw new ThemeConflictException("THEME_DRAFT_CONFLICT: 请保存并刷新草稿后再创建预览");
        }
        configValidator.validate(draft.getConfigJson());

        byte[] tokenBytes = new byte[16];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Date expiresAt = new Date(System.currentTimeMillis() + PREVIEW_TTL_MILLIS);

        ThemePreviewSession session = new ThemePreviewSession();
        session.setMerchantId(context.getMerchantId());
        session.setStoreId(storeId);
        session.setThemeId(themeId);
        session.setDraftId(draft.getId());
        session.setDraftRevision(draft.getRevision());
        session.setSchemaVersion(draft.getSchemaVersion());
        session.setConfigJson(draft.getConfigJson());
        session.setTokenHash(sha256(token));
        session.setCreatedBy(context.getUserId());
        session.setExpiresAt(expiresAt);
        themeMapper.insertPreviewSession(session);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("sessionId", String.valueOf(session.getId()));
        result.put("previewToken", token);
        result.put("previewPath", "/pages/decorator-preview/index?previewToken=" + token);
        result.put("expiresAt", expiresAt);
        result.put("draftRevision", draft.getRevision());
        result.put("storeCode", store.getStoreCode());
        addWxaCode(result, session, token);
        return result;
    }

    public void revoke(TenantContext context, Long sessionId)
    {
        contextService.requirePermission(context, DecoratorPermission.THEME_PREVIEW);
        if (sessionId == null || themeMapper.revokePreviewSession(
                context.getMerchantId(), sessionId, context.getUserId()) != 1)
        {
            throw new ServiceException("THEME_PREVIEW_EXPIRED: 预览会话不存在或已失效");
        }
    }

    public PreviewTheme resolve(String token)
    {
        if (token == null || !token.matches("^(?:[A-Za-z0-9_-]{22}|[A-Za-z0-9_-]{43})$"))
        {
            throw new ServiceException("THEME_PREVIEW_EXPIRED");
        }
        PreviewTheme preview = themeMapper.selectPreviewTheme(sha256(token));
        if (preview == null)
        {
            throw new ServiceException("THEME_PREVIEW_EXPIRED");
        }
        com.fasterxml.jackson.databind.JsonNode config = configValidator.validate(preview.getConfigJson());
        if (assetService != null) preview.setAssetUrls(assetService.assetUrls(preview.getMerchantId(),
                config));
        return preview;
    }

    private void addWxaCode(Map<String, Object> result, ThemePreviewSession session, String token)
    {
        if (wxaCodeService == null)
        {
            result.put("qrUnavailableReason", "微信小程序码服务未启用");
            return;
        }
        try
        {
            String fileName = "decorator_preview_" + session.getId() + "_" + System.currentTimeMillis();
            result.put("qrCodeUrl", wxaCodeService.generateWxaCode(
                    "pages/decorator-preview/index", token, fileName));
        }
        catch (ServiceException e)
        {
            result.put("qrUnavailableReason", e.getMessage());
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
}
