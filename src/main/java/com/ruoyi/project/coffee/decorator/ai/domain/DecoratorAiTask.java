package com.ruoyi.project.coffee.decorator.ai.domain;

import java.util.Date;

public class DecoratorAiTask
{
    private Long id;
    private Long merchantId;
    private Long storeId;
    private Long slotId;
    private String slotKey;
    private Integer slotSpecVersion;
    private Integer aiProfileVersion;
    private Long referenceAssetId;
    private String generationType;
    private String promptText;
    private String promptVersion;
    private String textContent;
    private String textMode;
    private String stylePreset;
    private String placementPreset;
    private String sizePreset;
    private Integer candidateCount;
    private Boolean transparentBackground;
    private String provider;
    private String status;
    private Long requestedBy;
    private String errorMessage;
    private Long productId;
    private Integer targetWidth;
    private Integer targetHeight;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }
    public String getSlotKey() { return slotKey; }
    public void setSlotKey(String slotKey) { this.slotKey = slotKey; }
    public Integer getSlotSpecVersion() { return slotSpecVersion; }
    public void setSlotSpecVersion(Integer slotSpecVersion) { this.slotSpecVersion = slotSpecVersion; }
    public Integer getAiProfileVersion() { return aiProfileVersion; }
    public void setAiProfileVersion(Integer aiProfileVersion) { this.aiProfileVersion = aiProfileVersion; }
    public Long getReferenceAssetId() { return referenceAssetId; }
    public void setReferenceAssetId(Long referenceAssetId) { this.referenceAssetId = referenceAssetId; }
    public String getGenerationType() { return generationType; }
    public void setGenerationType(String generationType) { this.generationType = generationType; }
    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }
    public String getTextMode() { return textMode; }
    public void setTextMode(String textMode) { this.textMode = textMode; }
    public String getStylePreset() { return stylePreset; }
    public void setStylePreset(String stylePreset) { this.stylePreset = stylePreset; }
    public String getPlacementPreset() { return placementPreset; }
    public void setPlacementPreset(String placementPreset) { this.placementPreset = placementPreset; }
    public String getSizePreset() { return sizePreset; }
    public void setSizePreset(String sizePreset) { this.sizePreset = sizePreset; }
    public Integer getCandidateCount() { return candidateCount; }
    public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }
    public Boolean getTransparentBackground() { return transparentBackground; }
    public void setTransparentBackground(Boolean transparentBackground) { this.transparentBackground = transparentBackground; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getTargetWidth() { return targetWidth; }
    public void setTargetWidth(Integer targetWidth) { this.targetWidth = targetWidth; }
    public Integer getTargetHeight() { return targetHeight; }
    public void setTargetHeight(Integer targetHeight) { this.targetHeight = targetHeight; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
