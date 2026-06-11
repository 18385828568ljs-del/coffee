package com.ruoyi.project.coffee.image.domain;

public class ImageAiGenerateResult
{
    private String originalUrl;

    private String generatedUrl;

    public ImageAiGenerateResult()
    {
    }

    public ImageAiGenerateResult(String originalUrl, String generatedUrl)
    {
        this.originalUrl = originalUrl;
        this.generatedUrl = generatedUrl;
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
}
