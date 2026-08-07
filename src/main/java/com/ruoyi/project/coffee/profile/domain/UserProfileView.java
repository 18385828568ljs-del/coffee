package com.ruoyi.project.coffee.profile.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Read-only merchant view of a user profile. */
public class UserProfileView
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
    private Date calculateTime;
    private List<ProfilePreference> mallCategories = new ArrayList<>();
    private List<ProfilePreference> mallProducts = new ArrayList<>();
    private List<RecommendedProduct> recommendations = new ArrayList<>();

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
    public Date getCalculateTime() { return calculateTime; }
    public void setCalculateTime(Date calculateTime) { this.calculateTime = calculateTime; }
    public List<ProfilePreference> getMallCategories() { return mallCategories; }
    public void setMallCategories(List<ProfilePreference> mallCategories) { this.mallCategories = mallCategories; }
    public List<ProfilePreference> getMallProducts() { return mallProducts; }
    public void setMallProducts(List<ProfilePreference> mallProducts) { this.mallProducts = mallProducts; }
    public List<RecommendedProduct> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendedProduct> recommendations) { this.recommendations = recommendations; }
}
