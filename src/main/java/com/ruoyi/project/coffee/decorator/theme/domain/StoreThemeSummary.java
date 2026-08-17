package com.ruoyi.project.coffee.decorator.theme.domain;

public class StoreThemeSummary
{
    private Long storeId;
    private String storeCode;
    private String storeName;
    private String storeStatus;
    private String bindingMode;
    private Long themeId;
    private Long publishedVersionId;

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getStoreCode() { return storeCode; }
    public void setStoreCode(String storeCode) { this.storeCode = storeCode; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getStoreStatus() { return storeStatus; }
    public void setStoreStatus(String storeStatus) { this.storeStatus = storeStatus; }
    public String getBindingMode() { return bindingMode; }
    public void setBindingMode(String bindingMode) { this.bindingMode = bindingMode; }
    public Long getThemeId() { return themeId; }
    public void setThemeId(Long themeId) { this.themeId = themeId; }
    public Long getPublishedVersionId() { return publishedVersionId; }
    public void setPublishedVersionId(Long publishedVersionId) { this.publishedVersionId = publishedVersionId; }
}
