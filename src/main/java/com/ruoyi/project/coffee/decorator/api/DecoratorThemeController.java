package com.ruoyi.project.coffee.decorator.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.decorator.context.TenantContext;
import com.ruoyi.project.coffee.decorator.context.TenantContextHolder;
import com.ruoyi.project.coffee.decorator.theme.DecoratorThemeService;
import com.ruoyi.project.coffee.decorator.theme.DecoratorPreviewService;
import com.ruoyi.project.coffee.decorator.theme.ThemeConfigValidationException;
import com.ruoyi.project.coffee.decorator.theme.ThemeConflictException;
import com.ruoyi.project.coffee.decorator.theme.domain.StoreThemeSummary;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemeDraft;

@RestController
@RequestMapping("/coffee/decorator")
public class DecoratorThemeController
{
    @Autowired
    private DecoratorThemeService themeService;

    @Autowired
    private DecoratorPreviewService previewService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/stores")
    public AjaxResult stores()
    {
        return AjaxResult.success(themeService.stores(context()));
    }

    @GetMapping("/templates")
    public AjaxResult templates()
    {
        return AjaxResult.success(themeService.templates(context()));
    }

    @GetMapping("/themes/master")
    public AjaxResult master()
    {
        return AjaxResult.success(themeService.masterTheme(context()));
    }

    @GetMapping("/themes")
    public AjaxResult themes(@RequestParam("scopeType") String scopeType,
            @RequestParam(value = "scopeId", required = false) Long scopeId)
    {
        return AjaxResult.success(themeService.themes(context(), scopeType, scopeId));
    }

    @PostMapping("/themes")
    public AjaxResult createTheme(@RequestBody DecoratorThemeCreateRequest request)
    {
        return AjaxResult.success(themeService.createTheme(context(), request.getScopeType(), request.getScopeId(),
                request.getName(), request.getSourceType(), request.getSourceThemeId(), request.getSourceTemplateId()));
    }

    @PostMapping("/themes/master")
    public AjaxResult initializeMaster(@RequestBody java.util.Map<String, Object> request)
    {
        Object value = request == null ? null : request.get("templateId");
        Long templateId;
        try
        {
            templateId = value == null ? null : Long.valueOf(String.valueOf(value));
        }
        catch (NumberFormatException e)
        {
            templateId = null;
        }
        return AjaxResult.success(themeService.initializeMasterTheme(context(), templateId));
    }

    @GetMapping("/themes/{themeId}/draft")
    public AjaxResult draft(@PathVariable Long themeId)
    {
        return AjaxResult.success(themeService.draft(context(), themeId));
    }

    @PutMapping("/themes/{themeId}/draft")
    public AjaxResult saveDraft(@PathVariable Long themeId, @RequestBody DecoratorDraftRequest request)
    {
        try
        {
            return AjaxResult.success(themeService.saveDraft(context(), themeId, request.getRevision(),
                    objectMapper.writeValueAsString(request.getConfig())));
        }
        catch (JsonProcessingException e)
        {
            return error("THEME_CONFIG_INVALID", "主题配置不是合法 JSON", HttpStatus.BAD_REQUEST.value());
        }
    }

    @PostMapping("/themes/{themeId}/validate")
    public AjaxResult validate(@PathVariable Long themeId, @RequestBody DecoratorDraftRequest request)
    {
        try
        {
            JsonNode config = themeService.validate(context(), themeId,
                    objectMapper.writeValueAsString(request.getConfig()));
            return AjaxResult.success(config);
        }
        catch (JsonProcessingException e)
        {
            return error("THEME_CONFIG_INVALID", "主题配置不是合法 JSON", HttpStatus.BAD_REQUEST.value());
        }
    }

    @PostMapping("/themes/{themeId}/publish")
    public AjaxResult publish(@PathVariable Long themeId, @RequestBody DecoratorPublishRequest request)
    {
        return AjaxResult.success(themeService.publish(context(), themeId, request.getRevision(),
                request.getIdempotencyKey(), request.getPublishNote()));
    }

    @PostMapping("/themes/{themeId}/preview-sessions")
    public AjaxResult createPreview(@PathVariable Long themeId, @RequestBody DecoratorPreviewRequest request)
    {
        return AjaxResult.success(previewService.create(context(), themeId,
                request.getStoreId(), request.getRevision()));
    }

    @DeleteMapping("/preview-sessions/{sessionId}")
    public AjaxResult revokePreview(@PathVariable Long sessionId)
    {
        previewService.revoke(context(), sessionId);
        return AjaxResult.success();
    }

    @GetMapping("/themes/{themeId}/versions")
    public AjaxResult versions(@PathVariable Long themeId)
    {
        return AjaxResult.success(themeService.versions(context(), themeId));
    }

    @GetMapping("/themes/{themeId}/versions/{versionId}")
    public AjaxResult version(@PathVariable Long themeId, @PathVariable Long versionId)
    {
        return AjaxResult.success(themeService.version(context(), themeId, versionId));
    }

    @PostMapping("/themes/{themeId}/versions/{versionId}/restore")
    public AjaxResult restore(@PathVariable Long themeId, @PathVariable Long versionId,
            @RequestBody DecoratorDraftRequest request)
    {
        return AjaxResult.success(themeService.restore(context(), themeId, versionId, request.getRevision()));
    }

    @PostMapping("/stores/{storeId}/independent-theme")
    public AjaxResult independent(@PathVariable Long storeId)
    {
        return AjaxResult.success(themeService.createIndependentTheme(context(), storeId));
    }

    @PostMapping("/stores/{storeId}/follow-master")
    public AjaxResult followMaster(@PathVariable Long storeId)
    {
        return AjaxResult.success(themeService.followMaster(context(), storeId));
    }

    private TenantContext context()
    {
        return TenantContextHolder.require();
    }

    private AjaxResult error(String code, String message, int status)
    {
        AjaxResult result = AjaxResult.error(message);
        result.put("errorCode", code);
        result.put("httpStatus", status);
        return result;
    }

    @ExceptionHandler(ThemeConflictException.class)
    public AjaxResult handleConflict(ThemeConflictException exception)
    {
        return error(exception.getMessage().startsWith("THEME_NOT_CHANGED") ? "THEME_NOT_CHANGED" : "THEME_DRAFT_CONFLICT",
                exception.getMessage(), HttpStatus.CONFLICT.value());
    }

    @ExceptionHandler(ThemeConfigValidationException.class)
    public AjaxResult handleConfig(ThemeConfigValidationException exception)
    {
        AjaxResult result = error("THEME_CONFIG_INVALID", exception.getMessage(), HttpStatus.BAD_REQUEST.value());
        result.put("errors", exception.getErrors());
        return result;
    }

    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleService(ServiceException exception)
    {
        return error("THEME_REQUEST_INVALID", exception.getMessage(), HttpStatus.BAD_REQUEST.value());
    }
}
