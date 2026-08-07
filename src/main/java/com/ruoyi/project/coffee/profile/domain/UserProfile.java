package com.ruoyi.project.coffee.profile.domain;

import java.math.BigDecimal;
import java.util.Date;

/** 微信顾客统一画像快照。 */
public class UserProfile
{
    private Long userId;
    private Integer orderCount;
    private BigDecimal totalAmount;
    private BigDecimal avgOrderAmount;
    private BigDecimal preferredPriceMin;
    private BigDecimal preferredPriceMax;
    private Date lastOrderTime;
    private Date lastActiveTime;
    private Integer evidenceCount;
    private String profileStatus;
    private String profileData;
    private Date calculateTime;
    private Date createTime;
    private Date updateTime;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getAvgOrderAmount() { return avgOrderAmount; }
    public void setAvgOrderAmount(BigDecimal avgOrderAmount) { this.avgOrderAmount = avgOrderAmount; }
    public BigDecimal getPreferredPriceMin() { return preferredPriceMin; }
    public void setPreferredPriceMin(BigDecimal preferredPriceMin) { this.preferredPriceMin = preferredPriceMin; }
    public BigDecimal getPreferredPriceMax() { return preferredPriceMax; }
    public void setPreferredPriceMax(BigDecimal preferredPriceMax) { this.preferredPriceMax = preferredPriceMax; }
    public Date getLastOrderTime() { return lastOrderTime; }
    public void setLastOrderTime(Date lastOrderTime) { this.lastOrderTime = lastOrderTime; }
    public Date getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(Date lastActiveTime) { this.lastActiveTime = lastActiveTime; }
    public Integer getEvidenceCount() { return evidenceCount; }
    public void setEvidenceCount(Integer evidenceCount) { this.evidenceCount = evidenceCount; }
    public String getProfileStatus() { return profileStatus; }
    public void setProfileStatus(String profileStatus) { this.profileStatus = profileStatus; }
    public String getProfileData() { return profileData; }
    public void setProfileData(String profileData) { this.profileData = profileData; }
    public Date getCalculateTime() { return calculateTime; }
    public void setCalculateTime(Date calculateTime) { this.calculateTime = calculateTime; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
