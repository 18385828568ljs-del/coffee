package com.ruoyi.project.coffee.decorator.font.domain;

import java.util.Date;

public class FontResource
{
    private Long id;
    private String fontCode;
    private String fontName;
    private String familyName;
    private String sourceType;
    private Long merchantId;
    private String storageKey;
    private String previewStorageKey;
    private String mimeType;
    private Integer fontWeight;
    private String fontStyle;
    private String status;
    private Long createdBy;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFontCode() { return fontCode; }
    public void setFontCode(String fontCode) { this.fontCode = fontCode; }
    public String getFontName() { return fontName; }
    public void setFontName(String fontName) { this.fontName = fontName; }
    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getPreviewStorageKey() { return previewStorageKey; }
    public void setPreviewStorageKey(String previewStorageKey) { this.previewStorageKey = previewStorageKey; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Integer getFontWeight() { return fontWeight; }
    public void setFontWeight(Integer fontWeight) { this.fontWeight = fontWeight; }
    public String getFontStyle() { return fontStyle; }
    public void setFontStyle(String fontStyle) { this.fontStyle = fontStyle; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getUrl() { return storageKey; }
    public String getPreviewUrl() { return previewStorageKey; }
}
