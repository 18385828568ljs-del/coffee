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

    // 扫码点单特有指标
    private Long pendingAcceptCount;

    private Long makingCount;

    private BigDecimal averageWaitMinutes;

    private Long urgeCount;

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

    public Long getPendingAcceptCount()
    {
        return pendingAcceptCount;
    }

    public void setPendingAcceptCount(Long pendingAcceptCount)
    {
        this.pendingAcceptCount = pendingAcceptCount;
    }

    public Long getMakingCount()
    {
        return makingCount;
    }

    public void setMakingCount(Long makingCount)
    {
        this.makingCount = makingCount;
    }

    public BigDecimal getAverageWaitMinutes()
    {
        return averageWaitMinutes;
    }

    public void setAverageWaitMinutes(BigDecimal averageWaitMinutes)
    {
        this.averageWaitMinutes = averageWaitMinutes;
    }

    public Long getUrgeCount()
    {
        return urgeCount;
    }

    public void setUrgeCount(Long urgeCount)
    {
        this.urgeCount = urgeCount;
    }
}
