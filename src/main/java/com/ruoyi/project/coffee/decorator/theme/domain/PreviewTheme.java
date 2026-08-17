package com.ruoyi.project.coffee.decorator.theme.domain;

import java.util.Date;
import java.util.Map;

public class PreviewTheme
{
    private Long previewId;
    private Long themeId;
    private String storeCode;
    private String storeName;
    private Long merchantId;
    private Integer draftRevision;
    private String schemaVersion;
    private String configJson;
    private Date expiresAt;
    private Map<String, String> assetUrls;

    public Long getPreviewId() { return previewId; }
    public void setPreviewId(Long previewId) { this.previewId = previewId; }
    public Long getThemeId() { return themeId; }
    public void setThemeId(Long themeId) { this.themeId = themeId; }

    public String getStoreCode() { return storeCode; }
    public void setStoreCode(String storeCode) { this.storeCode = storeCode; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Integer getDraftRevision() { return draftRevision; }
    public void setDraftRevision(Integer draftRevision) { this.draftRevision = draftRevision; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
    public Map<String, String> getAssetUrls() { return assetUrls; }
    public void setAssetUrls(Map<String, String> assetUrls) { this.assetUrls = assetUrls; }
}
