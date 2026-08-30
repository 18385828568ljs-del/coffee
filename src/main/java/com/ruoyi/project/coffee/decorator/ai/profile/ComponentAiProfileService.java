package com.ruoyi.project.coffee.decorator.ai.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.asset.BackgroundSlotService;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;
import com.ruoyi.project.coffee.decorator.mapper.ComponentAiProfileMapper;

@Service
public class ComponentAiProfileService
{
    private static final List<String> TEXT_MODE_CODES = Arrays.asList("NO_TEXT", "ART_TEXT_LAYER", "EMBEDDED_TEXT");
    @Autowired private ComponentAiProfileMapper mapper;
    @Autowired private BackgroundSlotService slotService;
    @Autowired private ObjectMapper objectMapper;

    public ComponentAiProfile require(String slotKey)
    {
        String componentKey = componentKey(slotKey);
        ComponentAiProfile profile = mapper.selectActive(componentKey);
        if (profile == null || !Boolean.TRUE.equals(profile.getEnabled()))
            throw new ServiceException("该组件未开放 AI 生成");
        validate(profile);
        return profile;
    }

    public List<ComponentAiCapabilities> capabilities()
    {
        List<ComponentAiCapabilities> result = new ArrayList<ComponentAiCapabilities>();
        List<ComponentAiProfile> profiles = mapper.selectActiveList();
        if (profiles == null) return result;
        for (ComponentAiProfile profile : profiles)
        {
            if (!Boolean.TRUE.equals(profile.getEnabled())) continue;
            try { result.add(capabilities(profile.getSlotKey())); }
            catch (ServiceException ignored) { /* An orphaned profile is not exposed. */ }
        }
        return result;
    }

    public ComponentAiCapabilities capabilities(String slotKey)
    {
        String componentKey = componentKey(slotKey);
        BackgroundSlotSpec slot = slotService.require(componentKey);
        ComponentAiProfile profile = require(componentKey);
        if (!componentKey.equals(slot.getComponentKey())) throw new ServiceException("Profile 与背景插槽不匹配");
        ComponentAiCapabilities result = new ComponentAiCapabilities();
        result.setSlotKey(componentKey);
        result.setSlotSpec(slot);
        result.setProfileVersion(profile.getProfileVersion());
        result.setGenerationModes(values(profile.getGenerationModes()));
        result.setRequiredFields(jsonValues(profile.getRequiredFieldsJson()));
        result.setOptionalFields(jsonValues(profile.getOptionalFieldsJson()));
        result.setPromptTemplateCode(profile.getPromptTemplateCode());
        result.setVisualDensity(profile.getVisualDensity());
        List<ComponentAiCapabilities.TextModeOption> textModes = new ArrayList<ComponentAiCapabilities.TextModeOption>();
        for (String code : values(profile.getAllowedTextModes()))
        {
            textModes.add(new ComponentAiCapabilities.TextModeOption(code, textModeName(code), code.equals(profile.getDefaultTextMode())));
        }
        result.setTextModes(textModes);
        return result;
    }

    public boolean allows(ComponentAiProfile profile, String generationType)
    { return values(profile.getGenerationModes()).contains(generationType); }

    public boolean allowsText(ComponentAiProfile profile, String textMode)
    { return values(profile.getAllowedTextModes()).contains(textMode); }

    public List<String> values(String value)
    {
        if (value == null || value.trim().isEmpty()) return Collections.emptyList();
        try { return jsonValues(value); }
        catch (RuntimeException ignored)
        {
            String normalized = value.replace('[', ' ').replace(']', ' ').replace('"', ' ');
            List<String> result = new ArrayList<String>();
            for (String item : normalized.split(",")) if (!item.trim().isEmpty()) result.add(item.trim());
            return result;
        }
    }

    private void validate(ComponentAiProfile profile)
    {
        List<String> allowed = values(profile.getAllowedTextModes());
        if (profile.getDefaultTextMode() == null || !TEXT_MODE_CODES.contains(profile.getDefaultTextMode()) || !allowed.contains(profile.getDefaultTextMode()))
            throw new ServiceException("Profile 默认文字模式非法");
        if (values(profile.getGenerationModes()).isEmpty()) throw new ServiceException("Profile 生成模式为空");
    }

    private List<String> jsonValues(String value)
    {
        if (value == null || value.trim().isEmpty()) return Collections.emptyList();
        try
        {
            JsonNode node = objectMapper.readTree(value);
            if (!node.isArray()) throw new IllegalArgumentException("not array");
            List<String> result = new ArrayList<String>();
            for (JsonNode item : node) if (item.isTextual()) result.add(item.asText());
            return result;
        }
        catch (Exception e) { throw new IllegalArgumentException("Invalid profile list", e); }
    }

    private String componentKey(String slotKey)
    {
        String value = slotKey == null ? "" : slotKey.trim();
        if (value.indexOf('.') >= 0) value = value.substring(value.lastIndexOf('.') + 1);
        if (!value.matches("^[A-Za-z][A-Za-z0-9]{1,63}$")) throw new ServiceException("背景插槽不存在");
        return value;
    }

    private String textModeName(String code)
    {
        if ("ART_TEXT_LAYER".equals(code)) return "生成独立艺术字，可单独调整";
        if ("EMBEDDED_TEXT".equals(code)) return "文字直接生成在图片中";
        return "不生成文字";
    }
}
