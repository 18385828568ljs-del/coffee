package com.ruoyi.project.coffee.dashboard.domain;

import java.util.Date;

public class ActivityWarningVO
{
    private Long activityId;

    private String title;

    private Integer type;

    private String typeLabel;

    private Date endTime;

    private String endDate;

    private Long remainingDays;

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

    public Integer getType()
    {
        return type;
    }

    public void setType(Integer type)
    {
        this.type = type;
    }

    public String getTypeLabel()
    {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel)
    {
        this.typeLabel = typeLabel;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public String getEndDate()
    {
        return endDate;
    }

    public void setEndDate(String endDate)
    {
        this.endDate = endDate;
    }

    public Long getRemainingDays()
    {
        return remainingDays;
    }

    public void setRemainingDays(Long remainingDays)
    {
        this.remainingDays = remainingDays;
    }
}
