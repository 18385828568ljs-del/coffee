package com.ruoyi.project.coffee.dashboard.domain;

import java.util.Date;

public class DashboardQueryDTO
{
    private String period;

    private String periodLabel;

    private Date startTime;

    private Date endTimeExclusive;

    private Date nowTime;

    private Date endingSoonTime;

    private Integer lowStockThreshold;

    public String getPeriod()
    {
        return period;
    }

    public void setPeriod(String period)
    {
        this.period = period;
    }

    public String getPeriodLabel()
    {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel)
    {
        this.periodLabel = periodLabel;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTimeExclusive()
    {
        return endTimeExclusive;
    }

    public void setEndTimeExclusive(Date endTimeExclusive)
    {
        this.endTimeExclusive = endTimeExclusive;
    }

    public Date getNowTime()
    {
        return nowTime;
    }

    public void setNowTime(Date nowTime)
    {
        this.nowTime = nowTime;
    }

    public Date getEndingSoonTime()
    {
        return endingSoonTime;
    }

    public void setEndingSoonTime(Date endingSoonTime)
    {
        this.endingSoonTime = endingSoonTime;
    }

    public Integer getLowStockThreshold()
    {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold)
    {
        this.lowStockThreshold = lowStockThreshold;
    }
}
