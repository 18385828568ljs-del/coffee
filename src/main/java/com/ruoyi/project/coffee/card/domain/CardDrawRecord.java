package com.ruoyi.project.coffee.card.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class CardDrawRecord
{
    private Long drawId;
    private String requestNo;
    private Long campaignId;
    private String campaignTitle;
    private Long cardId;
    private Long userId;
    private String cardTitle;
    private String finalImageUrl;
    private String leftProductType;
    private Long leftProductId;
    private String rightProductType;
    private Long rightProductId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date drawTime;

    public Long getDrawId() { return drawId; }
    public void setDrawId(Long drawId) { this.drawId = drawId; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getCampaignTitle() { return campaignTitle; }
    public void setCampaignTitle(String campaignTitle) { this.campaignTitle = campaignTitle; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCardTitle() { return cardTitle; }
    public void setCardTitle(String cardTitle) { this.cardTitle = cardTitle; }
    public String getFinalImageUrl() { return finalImageUrl; }
    public void setFinalImageUrl(String finalImageUrl) { this.finalImageUrl = finalImageUrl; }
    public String getLeftProductType() { return leftProductType; }
    public void setLeftProductType(String leftProductType) { this.leftProductType = leftProductType; }
    public Long getLeftProductId() { return leftProductId; }
    public void setLeftProductId(Long leftProductId) { this.leftProductId = leftProductId; }
    public String getRightProductType() { return rightProductType; }
    public void setRightProductType(String rightProductType) { this.rightProductType = rightProductType; }
    public Long getRightProductId() { return rightProductId; }
    public void setRightProductId(Long rightProductId) { this.rightProductId = rightProductId; }
    public Date getDrawTime() { return drawTime; }
    public void setDrawTime(Date drawTime) { this.drawTime = drawTime; }
}
