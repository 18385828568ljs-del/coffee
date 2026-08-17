package com.ruoyi.project.coffee.decorator.theme.domain;

import java.util.Date;

public class DecoratorTheme
{
    private Long id;
    private Long merchantId;
    private String name;
    private String scopeType;
    private Long ownerStoreId;
    private Long sourceTemplateId;
    private String status;
    private Long createdBy;
    private Long clonedFromThemeId;
    private Long clonedFromVersionId;
    private Integer latestVersionNo;
    private Integer draftRevision;
    private Boolean hasUnpublishedChanges;
    private Boolean active;
    private Date updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public Long getOwnerStoreId() { return ownerStoreId; }
    public void setOwnerStoreId(Long ownerStoreId) { this.ownerStoreId = ownerStoreId; }
    public Long getSourceTemplateId() { return sourceTemplateId; }
    public void setSourceTemplateId(Long sourceTemplateId) { this.sourceTemplateId = sourceTemplateId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getClonedFromThemeId() { return clonedFromThemeId; }
    public void setClonedFromThemeId(Long clonedFromThemeId) { this.clonedFromThemeId = clonedFromThemeId; }
    public Long getClonedFromVersionId() { return clonedFromVersionId; }
    public void setClonedFromVersionId(Long clonedFromVersionId) { this.clonedFromVersionId = clonedFromVersionId; }
    public Integer getLatestVersionNo() { return latestVersionNo; }
    public void setLatestVersionNo(Integer latestVersionNo) { this.latestVersionNo = latestVersionNo; }
    public Integer getDraftRevision() { return draftRevision; }
    public void setDraftRevision(Integer draftRevision) { this.draftRevision = draftRevision; }
    public Boolean getHasUnpublishedChanges() { return hasUnpublishedChanges; }
    public void setHasUnpublishedChanges(Boolean hasUnpublishedChanges) { this.hasUnpublishedChanges = hasUnpublishedChanges; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
