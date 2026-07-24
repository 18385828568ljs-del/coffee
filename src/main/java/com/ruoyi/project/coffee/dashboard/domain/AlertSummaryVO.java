package com.ruoyi.project.coffee.dashboard.domain;

public class AlertSummaryVO
{
    private Long pendingShipmentCount;

    private Long lowStockCount;

    private Long endingActivityCount;

    // 扫码点单特有告警
    private Long pendingAcceptCount;

    private Long highUrgeCount;

    public Long getPendingShipmentCount()
    {
        return pendingShipmentCount;
    }

    public void setPendingShipmentCount(Long pendingShipmentCount)
    {
        this.pendingShipmentCount = pendingShipmentCount;
    }

    public Long getLowStockCount()
    {
        return lowStockCount;
    }

    public void setLowStockCount(Long lowStockCount)
    {
        this.lowStockCount = lowStockCount;
    }

    public Long getEndingActivityCount()
    {
        return endingActivityCount;
    }

    public void setEndingActivityCount(Long endingActivityCount)
    {
        this.endingActivityCount = endingActivityCount;
    }

    public Long getPendingAcceptCount()
    {
        return pendingAcceptCount;
    }

    public void setPendingAcceptCount(Long pendingAcceptCount)
    {
        this.pendingAcceptCount = pendingAcceptCount;
    }

    public Long getHighUrgeCount()
    {
        return highUrgeCount;
    }

    public void setHighUrgeCount(Long highUrgeCount)
    {
        this.highUrgeCount = highUrgeCount;
    }
}
