package com.ruoyi.project.coffee.image.domain;

public class ImageAiApplyRequest
{
    private String productType;

    private Long productId;

    private String generatedUrl;

    public String getProductType()
    {
        return productType;
    }

    public void setProductType(String productType)
    {
        this.productType = productType;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public String getGeneratedUrl()
    {
        return generatedUrl;
    }

    public void setGeneratedUrl(String generatedUrl)
    {
        this.generatedUrl = generatedUrl;
    }
}
