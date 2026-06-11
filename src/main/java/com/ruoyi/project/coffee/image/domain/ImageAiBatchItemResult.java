package com.ruoyi.project.coffee.image.domain;

public class ImageAiBatchItemResult
{
    private Long productId;

    private String productName;

    private String originalUrl;

    private String generatedUrl;

    private String status;

    private String message;

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

    public String getOriginalUrl()
    {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl)
    {
        this.originalUrl = originalUrl;
    }

    public String getGeneratedUrl()
    {
        return generatedUrl;
    }

    public void setGeneratedUrl(String generatedUrl)
    {
        this.generatedUrl = generatedUrl;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
