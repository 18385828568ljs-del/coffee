package com.ruoyi.project.coffee.dashboard.domain;

public class AlertSummaryVO
{
    private Long pendingShipmentCount;

    private Long lowStockCount;

    private Long endingActivityCount;

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
}
