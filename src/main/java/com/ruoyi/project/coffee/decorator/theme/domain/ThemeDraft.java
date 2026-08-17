package com.ruoyi.project.coffee.decorator.theme.domain;

import java.util.Date;

public class ThemeDraft
{
    private Long id;
    private Long merchantId;
    private Long themeId;
    private Long basedOnVersionId;
    private String schemaVersion;
    private String configJson;
    private Integer revision;
    private Long updatedBy;
    private Date updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getThemeId() { return themeId; }
    public void setThemeId(Long themeId) { this.themeId = themeId; }
    public Long getBasedOnVersionId() { return basedOnVersionId; }
    public void setBasedOnVersionId(Long basedOnVersionId) { this.basedOnVersionId = basedOnVersionId; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public Integer getRevision() { return revision; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
