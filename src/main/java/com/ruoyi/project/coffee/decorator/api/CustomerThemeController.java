package com.ruoyi.project.coffee.decorator.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.decorator.theme.DecoratorThemeService;
import com.ruoyi.project.coffee.decorator.theme.DecoratorPreviewService;
import com.ruoyi.project.coffee.decorator.theme.domain.PublishedStoreTheme;
import com.ruoyi.project.coffee.decorator.theme.domain.PreviewTheme;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping({ "/api/wx", "/api" })
public class CustomerThemeController
{
    @Autowired
    private DecoratorThemeService themeService;

    @Autowired
    private DecoratorPreviewService previewService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/stores/{storeCode}/theme")
    public AjaxResult published(@PathVariable String storeCode)
    {
        PublishedStoreTheme theme = themeService.publishedStoreTheme(storeCode);
        return AjaxResult.success(theme);
    }

    /** Public aliases matching the mini-program Skin API contract. */
    @GetMapping("/stores/{storeCode}/skin")
    public AjaxResult skin(@PathVariable String storeCode)
    {
        PublishedStoreTheme theme = themeService.publishedStoreTheme(storeCode);
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
        result.put("source", "ACTIVE");
        result.put("themeId", theme.getThemeId() == null ? null : String.valueOf(theme.getThemeId()));
        result.put("versionId", theme.getVersionId() == null ? null : String.valueOf(theme.getVersionId()));
        result.put("version", theme.getVersionNo() == null ? theme.getVersionId() : theme.getVersionNo());
        try
        {
            result.put("config", objectMapper.readTree(theme.getConfigJson()));
        }
        catch (Exception e)
        {
            result.put("config", theme.getConfigJson());
        }
        result.put("storeCode", theme.getStoreCode());
        result.put("assetUrls", theme.getAssetUrls());
        return AjaxResult.success(result);
    }

    /**
     * Unified mini-program skin contract. A preview token always wins over the
     * active binding and returns the immutable draft snapshot it references.
     */
    @GetMapping("/mini/skin")
    public AjaxResult miniSkin(@RequestParam(value = "storeId", required = false) String storeId,
            @RequestParam(value = "previewToken", required = false) String previewToken)
    {
        if (previewToken != null && !previewToken.trim().isEmpty())
        {
            PreviewTheme preview = previewService.resolve(previewToken.trim());
            if (storeId != null && !storeId.trim().isEmpty() && !storeId.equals(preview.getStoreCode()))
            {
                throw new ServiceException("THEME_PREVIEW_EXPIRED");
            }
            return AjaxResult.success(previewPayload(preview));
        }

        if (storeId == null || storeId.trim().isEmpty()) throw new ServiceException("门店主题不存在");
        PublishedStoreTheme theme = themeService.publishedStoreTheme(storeId.trim());
        return AjaxResult.success(activePayload(theme));
    }

    private java.util.Map<String, Object> activePayload(PublishedStoreTheme theme)
    {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
        result.put("source", "ACTIVE");
        result.put("themeId", theme.getThemeId() == null ? null : String.valueOf(theme.getThemeId()));
        result.put("versionId", theme.getVersionId() == null ? null : String.valueOf(theme.getVersionId()));
        result.put("versionNo", theme.getVersionNo());
        result.put("storeId", theme.getStoreCode());
        result.put("storeCode", theme.getStoreCode());
        result.put("config", readConfig(theme.getConfigJson()));
        result.put("assetUrls", theme.getAssetUrls());
        return result;
    }

    private java.util.Map<String, Object> previewPayload(PreviewTheme preview)
    {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
        result.put("source", "PREVIEW");
        result.put("themeId", preview.getThemeId() == null ? null : String.valueOf(preview.getThemeId()));
        result.put("previewId", preview.getPreviewId() == null ? null : String.valueOf(preview.getPreviewId()));
        result.put("draftRevision", preview.getDraftRevision());
        result.put("storeId", preview.getStoreCode());
        result.put("storeCode", preview.getStoreCode());
        result.put("expiresAt", preview.getExpiresAt());
        result.put("config", readConfig(preview.getConfigJson()));
        result.put("assetUrls", preview.getAssetUrls());
        return result;
    }

    private Object readConfig(String configJson)
    {
        try
        {
            return objectMapper.readTree(configJson);
        }
        catch (Exception e)
        {
            return configJson;
        }
    }

    @GetMapping("/stores/{storeCode}/skin/version")
    public AjaxResult skinVersion(@PathVariable String storeCode)
    {
        PublishedStoreTheme theme = themeService.publishedStoreTheme(storeCode);
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
        result.put("versionId", theme.getVersionId());
        result.put("version", theme.getVersionNo() == null ? theme.getVersionId() : theme.getVersionNo());
        return AjaxResult.success(result);
    }

    @GetMapping("/theme-preview/{previewToken}")
    public AjaxResult preview(@PathVariable String previewToken)
    {
        return AjaxResult.success(previewService.resolve(previewToken));
    }

    @GetMapping("/skin/preview")
    public AjaxResult skinPreview(@org.springframework.web.bind.annotation.RequestParam("token") String token)
    {
        return AjaxResult.success(previewService.resolve(token));
    }

    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleService(ServiceException exception)
    {
        AjaxResult result = AjaxResult.error("门店主题不存在");
        String code = exception.getMessage() != null && exception.getMessage().startsWith("THEME_PREVIEW_EXPIRED")
                ? "THEME_PREVIEW_EXPIRED" : "THEME_VERSION_NOT_FOUND";
        result.put("errorCode", code);
        return result;
    }
}
