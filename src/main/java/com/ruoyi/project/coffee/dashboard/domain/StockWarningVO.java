package com.ruoyi.project.coffee.dashboard.domain;

public class StockWarningVO
{
    private Long productId;

    private String productName;

    private Long stock;

    private String imageUrl;

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public Long getStock()
    {
        return stock;
    }

    public void setStock(Long stock)
    {
        this.stock = stock;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }
}
