package com.ruoyi.project.coffee.decorator.ai.image;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import com.ruoyi.project.coffee.decorator.ai.DecoratorImageProcessor.ProcessedImage;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;

class CandidateValidatorTest
{
    private final CandidateValidator validator = new CandidateValidator();

    @Test
    void requiresExactSlotSize()
    {
        BackgroundSlotSpec slot = slot();
        assertThrows(IllegalStateException.class, () -> validator.validate(new ProcessedImage(new byte[] {1}, 99, 100, false), slot, false));
        assertDoesNotThrow(() -> validator.validate(new ProcessedImage(new byte[] {1}, 100, 100, false), slot, false));
    }

    @Test
    void requiresAlphaForArtText()
    {
        assertThrows(IllegalStateException.class, () -> validator.validate(new ProcessedImage(new byte[] {1}, 100, 100, false), slot(), true));
    }

    private BackgroundSlotSpec slot()
    {
        BackgroundSlotSpec slot = new BackgroundSlotSpec(); slot.setOutputWidth(100); slot.setOutputHeight(100); slot.setMaxFileSize(1024L); return slot;
    }
}
