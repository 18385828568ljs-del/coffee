package com.ruoyi.project.coffee.profile.domain;

/** 近 30 天有效订单明细中的商品销量。 */
public class ProductPopularity
{
    private Long productId;
    private Long popularity;

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getPopularity()
    {
        return popularity;
    }

    public void setPopularity(Long popularity)
    {
        this.popularity = popularity;
    }
}
