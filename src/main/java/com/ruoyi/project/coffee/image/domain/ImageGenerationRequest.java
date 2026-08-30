package com.ruoyi.project.coffee.image.domain;

public class ImageGenerationRequest
{
    private final String sourceImageDataUrl;

    private final String prompt;

    private final String background;

    private final int targetWidth;

    private final int targetHeight;

    public ImageGenerationRequest(String sourceImageDataUrl)
    {
        this(sourceImageDataUrl, null);
    }

    public ImageGenerationRequest(String sourceImageDataUrl, String prompt)
    {
        this(sourceImageDataUrl, prompt, null);
    }

    public ImageGenerationRequest(String sourceImageDataUrl, String prompt, String background)
    {
        this(sourceImageDataUrl, prompt, background, 0, 0);
    }

    public ImageGenerationRequest(String sourceImageDataUrl, String prompt, String background, int targetWidth, int targetHeight)
    {
        this.sourceImageDataUrl = sourceImageDataUrl;
        this.prompt = prompt;
        this.background = background;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
    }

    public String getSourceImageDataUrl()
    {
        return sourceImageDataUrl;
    }

    public String getPrompt()
    {
        return prompt;
    }

    public String getBackground()
    {
        return background;
    }

    public int getTargetWidth()
    {
        return targetWidth;
    }

    public int getTargetHeight()
    {
        return targetHeight;
    }
}
