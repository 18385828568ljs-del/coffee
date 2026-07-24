package com.ruoyi.project.coffee.image.client;

import com.ruoyi.project.coffee.image.domain.ImageGenerationRequest;
import com.ruoyi.project.coffee.image.domain.ImageGenerationResult;

public interface ImageGenerationClient
{
    ImageGenerationResult generate(ImageGenerationRequest request);
}
