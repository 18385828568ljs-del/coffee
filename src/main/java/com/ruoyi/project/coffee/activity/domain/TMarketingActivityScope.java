package com.ruoyi.project.coffee.activity.domain;

/**
 * 营销活动适用范围明细对象 t_marketing_activity_scope
 */
public class TMarketingActivityScope
{
    private Long scopeId;

    private Long activityId;

    private Integer scopeType;

    private Long scopeTargetId;

    public Long getScopeId()
    {
        return scopeId;
    }

    public void setScopeId(Long scopeId)
    {
        this.scopeId = scopeId;
    }

    public Long getActivityId()
    {
        return activityId;
    }

    public void setActivityId(Long activityId)
    {
        this.activityId = activityId;
    }

    public Integer getScopeType()
    {
        return scopeType;
    }

    public void setScopeType(Integer scopeType)
    {
        this.scopeType = scopeType;
    }

    public Long getScopeTargetId()
    {
        return scopeTargetId;
    }

    public void setScopeTargetId(Long scopeTargetId)
    {
        this.scopeTargetId = scopeTargetId;
    }
}
