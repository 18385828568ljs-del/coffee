package com.ruoyi.project.coffee.card.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.framework.web.domain.BaseEntity;

public class CardCampaign extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long campaignId;
    private String title;
    private String subtitle;
    private String coverImage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /** 0=draft, 1=published, 2=ended. */
    private Integer status;
    private Integer sortOrder;
    private Integer cardCount;
    private Integer readyCardCount;
    private Long currentUserDrawId;

    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getCardCount() { return cardCount == null ? 0 : cardCount; }
    public void setCardCount(Integer cardCount) { this.cardCount = cardCount; }
    public Integer getReadyCardCount() { return readyCardCount == null ? 0 : readyCardCount; }
    public void setReadyCardCount(Integer readyCardCount) { this.readyCardCount = readyCardCount; }
    public Long getCurrentUserDrawId() { return currentUserDrawId; }
    public void setCurrentUserDrawId(Long currentUserDrawId) { this.currentUserDrawId = currentUserDrawId; }
}
