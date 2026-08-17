package com.ruoyi.project.coffee.decorator.theme.domain;

import java.util.Map;

public class PublishedStoreTheme
{
    private String storeCode;
    private String storeName;
    private String storeStatus;
    private Long merchantId;
    private Long storeId;
    private Long themeId;
    private Long versionId;
    private Integer versionNo;
    private String schemaVersion;
    private String configJson;
    private String configHash;
    private Map<String, String> assetUrls;

    public String getStoreCode() { return storeCode; }
    public void setStoreCode(String storeCode) { this.storeCode = storeCode; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getStoreStatus() { return storeStatus; }
    public void setStoreStatus(String storeStatus) { this.storeStatus = storeStatus; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getThemeId() { return themeId; }
    public void setThemeId(Long themeId) { this.themeId = themeId; }
    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public String getConfigHash() { return configHash; }
    public void setConfigHash(String configHash) { this.configHash = configHash; }
    public Map<String, String> getAssetUrls() { return assetUrls; }
    public void setAssetUrls(Map<String, String> assetUrls) { this.assetUrls = assetUrls; }
}
