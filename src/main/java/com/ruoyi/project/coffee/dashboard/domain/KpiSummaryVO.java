package com.ruoyi.project.coffee.dashboard.domain;

import java.math.BigDecimal;

public class KpiSummaryVO
{
    private BigDecimal salesAmount;

    private Long orderCount;

    private Long paidOrderCount;

    private BigDecimal averageOrderValue;

    private Long newUserCount;

    private Long pendingShipmentCount;

    public BigDecimal getSalesAmount()
    {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount)
    {
        this.salesAmount = salesAmount;
    }

    public Long getOrderCount()
    {
        return orderCount;
    }

    public void setOrderCount(Long orderCount)
    {
        this.orderCount = orderCount;
    }

    public Long getPaidOrderCount()
    {
        return paidOrderCount;
    }

    public void setPaidOrderCount(Long paidOrderCount)
    {
        this.paidOrderCount = paidOrderCount;
    }

    public BigDecimal getAverageOrderValue()
    {
        return averageOrderValue;
    }

    public void setAverageOrderValue(BigDecimal averageOrderValue)
    {
        this.averageOrderValue = averageOrderValue;
    }

    public Long getNewUserCount()
    {
        return newUserCount;
    }

    public void setNewUserCount(Long newUserCount)
    {
        this.newUserCount = newUserCount;
    }

    public Long getPendingShipmentCount()
    {
        return pendingShipmentCount;
    }

    public void setPendingShipmentCount(Long pendingShipmentCount)
    {
        this.pendingShipmentCount = pendingShipmentCount;
    }
}
