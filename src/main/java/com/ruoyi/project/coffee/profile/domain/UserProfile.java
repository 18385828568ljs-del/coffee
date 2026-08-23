package com.ruoyi.project.coffee.profile.domain;

import java.util.Date;

/** 微信顾客统一画像快照。 */
public class UserProfile
{
    private Long userId;
    private String profileStatus;
    private String profileData;
    private Date calculateTime;
    private Date createTime;
    private Date updateTime;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
