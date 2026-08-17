package com.ruoyi.project.coffee.decorator.api;

public class DecoratorThemeCreateRequest
{
    private String scopeType;
    private Long scopeId;
    private String name;
    private String sourceType;
    private Long sourceThemeId;
    private Long sourceTemplateId;

    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long scopeId) { this.scopeId = scopeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceThemeId() { return sourceThemeId; }
    public void setSourceThemeId(Long sourceThemeId) { this.sourceThemeId = sourceThemeId; }
    public Long getSourceTemplateId() { return sourceTemplateId; }
    public void setSourceTemplateId(Long sourceTemplateId) { this.sourceTemplateId = sourceTemplateId; }
}
