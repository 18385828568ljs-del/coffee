package com.ruoyi.project.coffee.decorator.ai.profile;

import java.util.List;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;

public class ComponentAiCapabilities
{
    private String slotKey;
    private BackgroundSlotSpec slotSpec;
    private Integer profileVersion;
    private List<String> generationModes;
    private List<TextModeOption> textModes;
    private List<String> requiredFields;
    private List<String> optionalFields;
    private String promptTemplateCode;
    private String visualDensity;

    public String getSlotKey() { return slotKey; }
    public void setSlotKey(String slotKey) { this.slotKey = slotKey; }
    public BackgroundSlotSpec getSlotSpec() { return slotSpec; }
    public void setSlotSpec(BackgroundSlotSpec slotSpec) { this.slotSpec = slotSpec; }
    public Integer getProfileVersion() { return profileVersion; }
    public void setProfileVersion(Integer profileVersion) { this.profileVersion = profileVersion; }
    public List<String> getGenerationModes() { return generationModes; }
    public void setGenerationModes(List<String> generationModes) { this.generationModes = generationModes; }
    public List<TextModeOption> getTextModes() { return textModes; }
    public void setTextModes(List<TextModeOption> textModes) { this.textModes = textModes; }
    public List<String> getRequiredFields() { return requiredFields; }
    public void setRequiredFields(List<String> requiredFields) { this.requiredFields = requiredFields; }
    public List<String> getOptionalFields() { return optionalFields; }
    public void setOptionalFields(List<String> optionalFields) { this.optionalFields = optionalFields; }
    public String getPromptTemplateCode() { return promptTemplateCode; }
    public void setPromptTemplateCode(String promptTemplateCode) { this.promptTemplateCode = promptTemplateCode; }
    public String getVisualDensity() { return visualDensity; }
    public void setVisualDensity(String visualDensity) { this.visualDensity = visualDensity; }

    public static class TextModeOption
    {
        private String code;
        private String name;
        private boolean recommended;

        public TextModeOption() { }
        public TextModeOption(String code, String name, boolean recommended)
        { this.code = code; this.name = name; this.recommended = recommended; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isRecommended() { return recommended; }
        public void setRecommended(boolean recommended) { this.recommended = recommended; }
    }
}
