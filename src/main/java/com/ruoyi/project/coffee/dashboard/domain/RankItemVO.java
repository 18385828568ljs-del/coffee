package com.ruoyi.project.coffee.dashboard.domain;

import java.math.BigDecimal;

public class RankItemVO
{
    private Long productId;

    private String name;

    private BigDecimal amount;

    private Long quantity;

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public Long getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Long quantity)
    {
        this.quantity = quantity;
    }
}
