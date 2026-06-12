package com.ruoyi.project.coffee.offlineActivity.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 线下活动预约对象 t_offline_activity_signup
 */
public class OfflineActivitySignup extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long signupId;

    private Long activityId;

    private Long userId;

    @Excel(name = "预约状态")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "预约时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date signupTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cancelTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkinTime;

    private String activityTitle;

    private String activityCoverImage;

    private Date activityStartTime;

    private Date activityEndTime;

    private String activityLocation;

    private String nickname;

    private String avatar;

    public Long getSignupId()
    {
        return signupId;
    }

    public void setSignupId(Long signupId)
    {
        this.signupId = signupId;
    }

    public Long getActivityId()
    {
        return activityId;
    }

    public void setActivityId(Long activityId)
    {
        this.activityId = activityId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Date getSignupTime()
    {
        return signupTime;
    }

    public void setSignupTime(Date signupTime)
    {
        this.signupTime = signupTime;
    }

    public Date getCancelTime()
    {
        return cancelTime;
    }

    public void setCancelTime(Date cancelTime)
    {
        this.cancelTime = cancelTime;
    }

    public Date getCheckinTime()
    {
        return checkinTime;
    }

    public void setCheckinTime(Date checkinTime)
    {
        this.checkinTime = checkinTime;
    }

    public String getActivityTitle()
    {
        return activityTitle;
    }

    public void setActivityTitle(String activityTitle)
    {
        this.activityTitle = activityTitle;
    }

    public String getActivityCoverImage()
    {
        return activityCoverImage;
    }

    public void setActivityCoverImage(String activityCoverImage)
    {
        this.activityCoverImage = activityCoverImage;
    }

    public Date getActivityStartTime()
    {
        return activityStartTime;
    }

    public void setActivityStartTime(Date activityStartTime)
    {
        this.activityStartTime = activityStartTime;
    }

    public Date getActivityEndTime()
    {
        return activityEndTime;
    }

    public void setActivityEndTime(Date activityEndTime)
    {
        this.activityEndTime = activityEndTime;
    }

    public String getActivityLocation()
    {
        return activityLocation;
    }

    public void setActivityLocation(String activityLocation)
    {
        this.activityLocation = activityLocation;
    }

    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("signupId", getSignupId())
            .append("activityId", getActivityId())
            .append("userId", getUserId())
            .append("status", getStatus())
            .append("signupTime", getSignupTime())
            .append("cancelTime", getCancelTime())
            .append("checkinTime", getCheckinTime())
            .append("activityTitle", getActivityTitle())
            .append("nickname", getNickname())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
