package com.ruoyi.project.coffee.dashboard.domain;

import java.math.BigDecimal;

public class PieItemVO
{
    private String name;

    private BigDecimal value;

    private Long count;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public BigDecimal getValue()
    {
        return value;
    }

    public void setValue(BigDecimal value)
    {
        this.value = value;
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
