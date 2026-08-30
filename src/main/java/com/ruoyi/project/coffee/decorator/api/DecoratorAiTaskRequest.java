package com.ruoyi.project.coffee.decorator.api;

public class DecoratorAiTaskRequest
{
    private String slotKey;
    private Long storeId;
    private Long referenceAssetId;
    /** INITIAL_SKIN uses the component's built-in vintage skin texture as the reference. */
    private String referenceMode;
    private String prompt;
    private Integer candidateCount;
    private String textContent;
    private String stylePreset;
    private String placementPreset;
    private String sizePreset;
    private String generationType;
    private String textMode;
    private String colorTone;
    private String primaryColor;
    private String description;
    private String merchantDescription;
    private String mainElements;
    private String texturePreference;
    private Long productId;
    private String productName;
    private String productDescription;
    private String referenceImageUrl;
    private Integer targetWidth;
    private Integer targetHeight;

    public String getSlotKey() { return slotKey; }
    public void setSlotKey(String slotKey) { this.slotKey = slotKey; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getReferenceAssetId() { return referenceAssetId; }
    public void setReferenceAssetId(Long referenceAssetId) { this.referenceAssetId = referenceAssetId; }
    public String getReferenceMode() { return referenceMode; }
    public void setReferenceMode(String referenceMode) { this.referenceMode = referenceMode; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public Integer getCandidateCount() { return candidateCount; }
    public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }
    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }
    public String getStylePreset() { return stylePreset; }
    public void setStylePreset(String stylePreset) { this.stylePreset = stylePreset; }
    public String getPlacementPreset() { return placementPreset; }
    public void setPlacementPreset(String placementPreset) { this.placementPreset = placementPreset; }
    public String getSizePreset() { return sizePreset; }
    public void setSizePreset(String sizePreset) { this.sizePreset = sizePreset; }
    public String getGenerationType() { return generationType; }
    public void setGenerationType(String generationType) { this.generationType = generationType; }
    public String getTextMode() { return textMode; }
    public void setTextMode(String textMode) { this.textMode = textMode; }
    public String getColorTone() { return colorTone; }
    public void setColorTone(String colorTone) { this.colorTone = colorTone; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMerchantDescription() { return merchantDescription; }
    public void setMerchantDescription(String merchantDescription) { this.merchantDescription = merchantDescription; }
    public String getMainElements() { return mainElements; }
    public void setMainElements(String mainElements) { this.mainElements = mainElements; }
    public String getTexturePreference() { return texturePreference; }
    public void setTexturePreference(String texturePreference) { this.texturePreference = texturePreference; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
    public String getReferenceImageUrl() { return referenceImageUrl; }
    public void setReferenceImageUrl(String referenceImageUrl) { this.referenceImageUrl = referenceImageUrl; }
    public Integer getTargetWidth() { return targetWidth; }
    public void setTargetWidth(Integer targetWidth) { this.targetWidth = targetWidth; }
    public Integer getTargetHeight() { return targetHeight; }
    public void setTargetHeight(Integer targetHeight) { this.targetHeight = targetHeight; }
}
