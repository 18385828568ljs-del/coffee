package com.ruoyi.project.coffee.dashboard.domain;

import java.math.BigDecimal;

public class DiscountOrderSummaryVO
{
    private Long orderCount;

    private BigDecimal discountAmount;

    public Long getOrderCount()
    {
        return orderCount;
    }

    public void setOrderCount(Long orderCount)
    {
        this.orderCount = orderCount;
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
