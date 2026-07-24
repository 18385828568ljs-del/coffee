package com.ruoyi.project.coffee.activity.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 营销活动对象 t_marketing_activity
 */
public class TMarketingActivity extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long activityId;

    @Excel(name = "活动标题")
    private String title;

    @Excel(name = "活动类型")
    private Integer type;

    /** 适用场景: mall=仅商城, scan=仅点单, both=两者都用 */
    @Excel(name = "适用场景")
    private String targetType;

    @Excel(name = "适用范围类型")
    private Integer scopeType;

    @Excel(name = "状态")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endTime;

    private String scopeIdsText;

    private List<Long> scopeIds = Collections.emptyList();

    @Excel(name = "满额门槛")
    private BigDecimal conditionMinAmount;

    @Excel(name = "满件门槛")
    private Long conditionMinQuantity;

    @Excel(name = "仅新用户")
    private Integer conditionNewUserOnly;

    @Excel(name = "优惠方式")
    private String effectMode;

    @Excel(name = "优惠值")
    private BigDecimal effectValue;

    private Long giftProductId;

    private Long giftQuantity;

    private BigDecimal shippingBaseFreight;

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

    public String getTargetType()
    {
        return targetType;
    }

    public void setTargetType(String targetType)
    {
        this.targetType = targetType;
    }

    public Integer getScopeType()
    {
        return scopeType;
    }

    public void setScopeType(Integer scopeType)
    {
        this.scopeType = scopeType;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
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

    public String getScopeIdsText()
    {
        return scopeIdsText;
    }

    public void setScopeIdsText(String scopeIdsText)
    {
        this.scopeIdsText = scopeIdsText;
    }

    public List<Long> getScopeIds()
    {
        return scopeIds == null ? Collections.<Long>emptyList() : scopeIds;
    }

    public void setScopeIds(List<Long> scopeIds)
    {
        this.scopeIds = scopeIds == null ? Collections.<Long>emptyList() : new ArrayList<Long>(scopeIds);
    }

    public BigDecimal getConditionMinAmount()
    {
        return conditionMinAmount;
    }

    public void setConditionMinAmount(BigDecimal conditionMinAmount)
    {
        this.conditionMinAmount = conditionMinAmount;
    }

    public Long getConditionMinQuantity()
    {
        return conditionMinQuantity;
    }

    public void setConditionMinQuantity(Long conditionMinQuantity)
    {
        this.conditionMinQuantity = conditionMinQuantity;
    }

    public Integer getConditionNewUserOnly()
    {
        return conditionNewUserOnly;
    }

    public void setConditionNewUserOnly(Integer conditionNewUserOnly)
    {
        this.conditionNewUserOnly = conditionNewUserOnly;
    }

    public String getEffectMode()
    {
        return effectMode;
    }

    public void setEffectMode(String effectMode)
    {
        this.effectMode = effectMode;
    }

    public BigDecimal getEffectValue()
    {
        return effectValue;
    }

    public void setEffectValue(BigDecimal effectValue)
    {
        this.effectValue = effectValue;
    }

    public Long getGiftProductId()
    {
        return giftProductId;
    }

    public void setGiftProductId(Long giftProductId)
    {
        this.giftProductId = giftProductId;
    }

    public Long getGiftQuantity()
    {
        return giftQuantity;
    }

    public void setGiftQuantity(Long giftQuantity)
    {
        this.giftQuantity = giftQuantity;
    }

    public BigDecimal getShippingBaseFreight()
    {
        return shippingBaseFreight;
    }

    public void setShippingBaseFreight(BigDecimal shippingBaseFreight)
    {
        this.shippingBaseFreight = shippingBaseFreight;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("activityId", getActivityId())
            .append("title", getTitle())
            .append("type", getType())
            .append("targetType", getTargetType())
            .append("scopeType", getScopeType())
            .append("status", getStatus())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("scopeIdsText", getScopeIdsText())
            .append("scopeIds", getScopeIds())
            .append("conditionMinAmount", getConditionMinAmount())
            .append("conditionMinQuantity", getConditionMinQuantity())
            .append("conditionNewUserOnly", getConditionNewUserOnly())
            .append("effectMode", getEffectMode())
            .append("effectValue", getEffectValue())
            .append("giftProductId", getGiftProductId())
            .append("giftQuantity", getGiftQuantity())
            .append("shippingBaseFreight", getShippingBaseFreight())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
