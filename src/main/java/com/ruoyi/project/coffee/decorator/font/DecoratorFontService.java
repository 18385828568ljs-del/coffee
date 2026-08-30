package com.ruoyi.project.coffee.decorator.font;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.context.DecoratorPermission;
import com.ruoyi.project.coffee.decorator.context.TenantContext;
import com.ruoyi.project.coffee.decorator.context.TenantContextService;
import com.ruoyi.project.coffee.decorator.font.domain.FontResource;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorFontMapper;

@Service
public class DecoratorFontService
{
    @Autowired
    private DecoratorFontMapper fontMapper;

    @Autowired
    private TenantContextService contextService;

    public List<FontResource> list(TenantContext context)
    {
        contextService.requirePermission(context, DecoratorPermission.THEME_VIEW);
        List<FontResource> fonts = fontMapper.selectAvailableFonts(context.getMerchantId());
        return fonts == null ? Collections.<FontResource>emptyList() : fonts;
    }

    public void validateReferences(Long merchantId, JsonNode config)
    {
        Set<Long> ids = referencedFontIds(config);
        if (ids.isEmpty()) return;
        List<FontResource> fonts = fontMapper.selectUsableFonts(merchantId, new ArrayList<Long>(ids));
        if (fonts == null || fonts.size() != ids.size())
        {
            throw new ServiceException("THEME_FONT_INVALID: 字体不存在、已停用或无权使用");
        }
    }

    public Map<String, Map<String, Object>> resources(Long merchantId, JsonNode config)
    {
        Set<Long> ids = referencedFontIds(config);
        if (ids.isEmpty()) return Collections.emptyMap();
        List<FontResource> fonts = fontMapper.selectUsableFonts(merchantId, new ArrayList<Long>(ids));
        Map<String, Map<String, Object>> result = new LinkedHashMap<String, Map<String, Object>>();
        if (fonts == null) return result;
        for (FontResource font : fonts)
        {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("familyName", font.getFamilyName());
            item.put("url", font.getUrl());
            item.put("weight", font.getFontWeight());
            item.put("style", font.getFontStyle());
            result.put(String.valueOf(font.getId()), item);
        }
        return result;
    }

    private Set<Long> referencedFontIds(JsonNode config)
    {
        Set<Long> result = new LinkedHashSet<Long>();
        JsonNode typography = config == null ? null : config.path("typography");
        if (typography == null || !typography.isObject()) return result;
        java.util.Iterator<JsonNode> tokens = typography.elements();
        while (tokens.hasNext())
        {
            JsonNode fontId = tokens.next().get("fontId");
            if (fontId != null && fontId.isIntegralNumber() && fontId.canConvertToLong() && fontId.asLong() > 0)
            {
                result.add(fontId.asLong());
            }
        }
        return result;
    }
}
