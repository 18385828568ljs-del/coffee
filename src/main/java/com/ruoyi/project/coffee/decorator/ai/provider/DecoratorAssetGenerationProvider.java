package com.ruoyi.project.coffee.decorator.ai.provider;

public interface DecoratorAssetGenerationProvider
{
    byte[] generate(String prompt, String referenceUrl, boolean transparentBackground);

    default byte[] generate(String prompt, String referenceUrl, boolean transparentBackground, int targetWidth, int targetHeight)
    {
        return generate(prompt, referenceUrl, transparentBackground);
    }
}
