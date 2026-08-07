package com.ruoyi.project.coffee.profile.domain;

import java.math.BigDecimal;
import java.util.Date;

/** 两个点单场景的有效订单汇总。 */
public class ProfileOrderSummary
{
    private Integer orderCount;
    private BigDecimal totalAmount;
    private Date lastOrderTime;

    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Date getLastOrderTime() { return lastOrderTime; }
    public void setLastOrderTime(Date lastOrderTime) { this.lastOrderTime = lastOrderTime; }
}
