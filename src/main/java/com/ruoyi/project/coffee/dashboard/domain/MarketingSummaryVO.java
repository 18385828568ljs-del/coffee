package com.ruoyi.project.coffee.dashboard.domain;

import java.math.BigDecimal;

public class MarketingSummaryVO
{
    private Long activeActivityCount;

    private Long endingSoonCount;

    private Long discountedOrderCount;

    private BigDecimal discountAmount;

    public Long getActiveActivityCount()
    {
        return activeActivityCount;
    }

    public void setActiveActivityCount(Long activeActivityCount)
    {
        this.activeActivityCount = activeActivityCount;
    }

    public Long getEndingSoonCount()
    {
        return endingSoonCount;
    }

    public void setEndingSoonCount(Long endingSoonCount)
    {
        this.endingSoonCount = endingSoonCount;
    }

    public Long getDiscountedOrderCount()
    {
        return discountedOrderCount;
    }

    public void setDiscountedOrderCount(Long discountedOrderCount)
    {
        this.discountedOrderCount = discountedOrderCount;
    }

    public BigDecimal getDiscountAmount()
    {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount)
    {
        this.discountAmount = discountAmount;
    }
}
