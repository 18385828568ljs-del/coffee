package com.ruoyi.project.coffee.decorator.ai.profile;

/** Semantic generation policy for one registered decorator component. */
public class ComponentAiProfile
{
    private Long id;
    private String slotKey;
    private Integer profileVersion;
    private Boolean enabled;
    private String generationModes;
    private String allowedTextModes;
    private String defaultTextMode;
    private String promptTemplateCode;
    private String safeAreaPreset;
    private String visualDensity;
    private String requiredFieldsJson;
    private String optionalFieldsJson;
    private String allowedElementsJson;
    private String forbiddenElementsJson;
    private String validationRulesJson;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlotKey() { return slotKey; }
    public void setSlotKey(String slotKey) { this.slotKey = slotKey; }
    public Integer getProfileVersion() { return profileVersion; }
    public void setProfileVersion(Integer profileVersion) { this.profileVersion = profileVersion; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getGenerationModes() { return generationModes; }
    public void setGenerationModes(String generationModes) { this.generationModes = generationModes; }
    public String getAllowedTextModes() { return allowedTextModes; }
    public void setAllowedTextModes(String allowedTextModes) { this.allowedTextModes = allowedTextModes; }
    public String getDefaultTextMode() { return defaultTextMode; }
    public void setDefaultTextMode(String defaultTextMode) { this.defaultTextMode = defaultTextMode; }
    public String getPromptTemplateCode() { return promptTemplateCode; }
    public void setPromptTemplateCode(String promptTemplateCode) { this.promptTemplateCode = promptTemplateCode; }
    public String getSafeAreaPreset() { return safeAreaPreset; }
    public void setSafeAreaPreset(String safeAreaPreset) { this.safeAreaPreset = safeAreaPreset; }
    public String getVisualDensity() { return visualDensity; }
    public void setVisualDensity(String visualDensity) { this.visualDensity = visualDensity; }
    public String getRequiredFieldsJson() { return requiredFieldsJson; }
    public void setRequiredFieldsJson(String requiredFieldsJson) { this.requiredFieldsJson = requiredFieldsJson; }
    public String getOptionalFieldsJson() { return optionalFieldsJson; }
    public void setOptionalFieldsJson(String optionalFieldsJson) { this.optionalFieldsJson = optionalFieldsJson; }
    public String getAllowedElementsJson() { return allowedElementsJson; }
    public void setAllowedElementsJson(String allowedElementsJson) { this.allowedElementsJson = allowedElementsJson; }
    public String getForbiddenElementsJson() { return forbiddenElementsJson; }
    public void setForbiddenElementsJson(String forbiddenElementsJson) { this.forbiddenElementsJson = forbiddenElementsJson; }
    public String getValidationRulesJson() { return validationRulesJson; }
    public void setValidationRulesJson(String validationRulesJson) { this.validationRulesJson = validationRulesJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
