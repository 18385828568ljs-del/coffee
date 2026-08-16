package com.ruoyi.project.coffee.card.domain;

import com.ruoyi.framework.web.domain.BaseEntity;

public class AiCard extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long cardId;
    private Long campaignId;
    private String campaignTitle;
    private String title;
    private String englishTitle;
    private String leftProductType;
    private Long leftProductId;
    private String leftProductName;
    private String leftDescription;
    private String leftProductImage;
    private String rightProductType;
    private Long rightProductId;
    private String rightProductName;
    private String rightDescription;
    private String rightProductImage;
    private String brandName;
    private String logoUrl;
    private String themePrompt;
    private String templateCode;
    private String paletteCode;
    private String artworkUrl;
    private String finalImageUrl;
    /** 0=draft, 1=generating, 2=ready, 3=published, 4=failed, 5=disabled. */
    private Integer status;
    private Integer weight;
    private Integer version;
    private String lastError;

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getCampaignTitle() { return campaignTitle; }
    public void setCampaignTitle(String campaignTitle) { this.campaignTitle = campaignTitle; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getEnglishTitle() { return englishTitle; }
    public void setEnglishTitle(String englishTitle) { this.englishTitle = englishTitle; }
    public String getLeftProductType() { return leftProductType; }
    public void setLeftProductType(String leftProductType) { this.leftProductType = leftProductType; }
    public Long getLeftProductId() { return leftProductId; }
    public void setLeftProductId(Long leftProductId) { this.leftProductId = leftProductId; }
    public String getLeftProductName() { return leftProductName; }
    public void setLeftProductName(String leftProductName) { this.leftProductName = leftProductName; }
    public String getLeftDescription() { return leftDescription; }
    public void setLeftDescription(String leftDescription) { this.leftDescription = leftDescription; }
    public String getLeftProductImage() { return leftProductImage; }
    public void setLeftProductImage(String leftProductImage) { this.leftProductImage = leftProductImage; }
    public String getRightProductType() { return rightProductType; }
    public void setRightProductType(String rightProductType) { this.rightProductType = rightProductType; }
    public Long getRightProductId() { return rightProductId; }
    public void setRightProductId(Long rightProductId) { this.rightProductId = rightProductId; }
    public String getRightProductName() { return rightProductName; }
    public void setRightProductName(String rightProductName) { this.rightProductName = rightProductName; }
    public String getRightDescription() { return rightDescription; }
    public void setRightDescription(String rightDescription) { this.rightDescription = rightDescription; }
    public String getRightProductImage() { return rightProductImage; }
    public void setRightProductImage(String rightProductImage) { this.rightProductImage = rightProductImage; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getThemePrompt() { return themePrompt; }
    public void setThemePrompt(String themePrompt) { this.themePrompt = themePrompt; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getPaletteCode() { return paletteCode; }
    public void setPaletteCode(String paletteCode) { this.paletteCode = paletteCode; }
    public String getArtworkUrl() { return artworkUrl; }
    public void setArtworkUrl(String artworkUrl) { this.artworkUrl = artworkUrl; }
    public String getFinalImageUrl() { return finalImageUrl; }
    public void setFinalImageUrl(String finalImageUrl) { this.finalImageUrl = finalImageUrl; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getWeight() { return weight == null ? 1 : weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public Integer getVersion() { return version == null ? 1 : version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
