package com.ruoyi.project.coffee.offlineActivity.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 线下活动对象 t_offline_activity
 */
public class OfflineActivity extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long activityId;

    @Excel(name = "活动标题")
    private String title;

    @Excel(name = "封面图")
    private String coverImage;

    @Excel(name = "活动简介")
    private String summary;

    @Excel(name = "活动详情")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "预约截止时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date signupDeadline;

    @Excel(name = "活动地点")
    private String location;

    @Excel(name = "人数上限")
    private Integer quota;

    @Excel(name = "已预约人数")
    private Integer signupCount;

    @Excel(name = "排序")
    private Integer sortOrder;

    @Excel(name = "状态")
    private Integer status;

    private Integer currentUserSignupStatus;

    private Long currentUserSignupId;

    public Long getActivityId()
    {
        return activityId;
    }

    public void setActivityId(Long activityId)
    {
        this.activityId = activityId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getCoverImage()
    {
        return coverImage;
    }

    public void setCoverImage(String coverImage)
    {
        this.coverImage = coverImage;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public Date getSignupDeadline()
    {
        return signupDeadline;
    }

    public void setSignupDeadline(Date signupDeadline)
    {
        this.signupDeadline = signupDeadline;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public Integer getQuota()
    {
        return quota;
    }

    public void setQuota(Integer quota)
    {
        this.quota = quota;
    }

    public Integer getSignupCount()
    {
        return signupCount == null ? 0 : signupCount;
    }

    public void setSignupCount(Integer signupCount)
    {
        this.signupCount = signupCount;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getCurrentUserSignupStatus()
    {
        return currentUserSignupStatus;
    }

    public void setCurrentUserSignupStatus(Integer currentUserSignupStatus)
    {
        this.currentUserSignupStatus = currentUserSignupStatus;
    }

    public Long getCurrentUserSignupId()
    {
        return currentUserSignupId;
    }

    public void setCurrentUserSignupId(Long currentUserSignupId)
    {
        this.currentUserSignupId = currentUserSignupId;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("activityId", getActivityId())
            .append("title", getTitle())
            .append("coverImage", getCoverImage())
            .append("summary", getSummary())
            .append("content", getContent())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("signupDeadline", getSignupDeadline())
            .append("location", getLocation())
            .append("quota", getQuota())
            .append("signupCount", getSignupCount())
            .append("sortOrder", getSortOrder())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
