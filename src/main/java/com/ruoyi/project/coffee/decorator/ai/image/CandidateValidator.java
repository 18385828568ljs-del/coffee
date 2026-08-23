package com.ruoyi.project.coffee.decorator.ai.image;

import org.springframework.stereotype.Component;
import com.ruoyi.project.coffee.decorator.ai.DecoratorImageProcessor.ProcessedImage;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;

@Component
public class CandidateValidator
{
    public void validate(ProcessedImage image, BackgroundSlotSpec slot, boolean requiresAlpha)
    { validate(image, slot, requiresAlpha, slot.getOutputWidth(), slot.getOutputHeight()); }

    public void validate(ProcessedImage image, BackgroundSlotSpec slot, boolean requiresAlpha,
            int expectedWidth, int expectedHeight)
    {
        if (image == null || image.getWidth() != expectedWidth || image.getHeight() != expectedHeight)
            throw new IllegalStateException("候选图片尺寸不符合插槽规格");
        if (image.getBytes() == null || image.getBytes().length == 0)
            throw new IllegalStateException("候选图片内容为空");
        if (slot.getMaxFileSize() != null && image.getBytes().length > slot.getMaxFileSize())
            throw new IllegalStateException("候选图片超过插槽文件大小限制");
        if (requiresAlpha && !image.isAlpha()) throw new IllegalStateException("艺术字候选不包含透明通道");
    }
}
