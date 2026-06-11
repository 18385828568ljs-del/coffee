package com.ruoyi.project.coffee.image.domain;

public class ImageGenerationRequest
{
    private final String sourceImageDataUrl;

    private final String prompt;

    public ImageGenerationRequest(String sourceImageDataUrl)
    {
        this(sourceImageDataUrl, null);
    }

    public ImageGenerationRequest(String sourceImageDataUrl, String prompt)
    {
        this.sourceImageDataUrl = sourceImageDataUrl;
        this.prompt = prompt;
    }

    public String getSourceImageDataUrl()
    {
        return sourceImageDataUrl;
    }

    public String getPrompt()
    {
        return prompt;
    }
}
