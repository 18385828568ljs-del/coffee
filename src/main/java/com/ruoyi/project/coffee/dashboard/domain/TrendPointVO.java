package com.ruoyi.project.coffee.dashboard.domain;

import java.math.BigDecimal;

public class TrendPointVO
{
    private String dateLabel;

    private BigDecimal amount;

    private Long count;

    public String getDateLabel()
    {
        return dateLabel;
    }

    public void setDateLabel(String dateLabel)
    {
        this.dateLabel = dateLabel;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public Long getCount()
    {
        return count;
    }

    public void setCount(Long count)
    {
        this.count = count;
    }
}
