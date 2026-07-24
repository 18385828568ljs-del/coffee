package com.ruoyi.project.coffee.image.domain;

public class ImageGenerationResult
{
    private final String imageBase64;

    public ImageGenerationResult(String imageBase64)
    {
        this.imageBase64 = imageBase64;
    }

    public String getImageBase64()
    {
        return imageBase64;
    }
}
