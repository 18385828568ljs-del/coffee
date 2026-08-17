package com.ruoyi.project.coffee.decorator.theme.domain;

import java.util.Date;

public class ThemeVersion
{
    private Long id;
    private Long merchantId;
    private Long themeId;
    private Integer versionNo;
    private Integer sourceDraftRevision;
    private String schemaVersion;
    private String configJson;
    private String configHash;
    private String publishNote;
    private String idempotencyKey;
    private Long publishedBy;
    private Date publishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getThemeId() { return themeId; }
    public void setThemeId(Long themeId) { this.themeId = themeId; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public Integer getSourceDraftRevision() { return sourceDraftRevision; }
    public void setSourceDraftRevision(Integer sourceDraftRevision) { this.sourceDraftRevision = sourceDraftRevision; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public String getConfigHash() { return configHash; }
    public void setConfigHash(String configHash) { this.configHash = configHash; }
    public String getPublishNote() { return publishNote; }
    public void setPublishNote(String publishNote) { this.publishNote = publishNote; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Long getPublishedBy() { return publishedBy; }
    public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }
    public Date getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Date publishedAt) { this.publishedAt = publishedAt; }
}
