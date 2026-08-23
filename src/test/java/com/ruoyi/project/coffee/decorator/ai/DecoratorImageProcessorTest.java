package com.ruoyi.project.coffee.decorator.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;

class DecoratorImageProcessorTest
{
    private final DecoratorImageProcessor processor = new DecoratorImageProcessor();

    @Test
    void backgroundKeepsExactTargetSize() throws Exception
    {
        DecoratorImageProcessor.ProcessedImage result = processor.background(
                png(new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB)), 100, 100);

        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
        assertFalse(result.isAlpha());
    }

    @Test
    void wideTargetUsesCenterCropWithoutVirtualEdgePixels() throws Exception
    {
        BufferedImage source = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++)
            for (int x = 0; x < source.getWidth(); x++)
                source.setRGB(x, y, x < 50 ? 0xFFFF0000 : 0xFF0000FF);
        DecoratorImageProcessor.ProcessedImage result = processor.background(png(source), 300, 100);
        BufferedImage output = ImageIO.read(new java.io.ByteArrayInputStream(result.getBytes()));

        assertEquals(300, result.getWidth());
        assertEquals(100, result.getHeight());
        assertEquals(0xFFFF0000, output.getRGB(20, 50));
        assertEquals(0xFF0000FF, output.getRGB(280, 50));
    }

    @Test
    void artTextRequiresActualTransparentPixels() throws Exception
    {
        BufferedImage opaqueArgb = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < opaqueArgb.getHeight(); y++)
            for (int x = 0; x < opaqueArgb.getWidth(); x++) opaqueArgb.setRGB(x, y, 0xFFFFFFFF);
        assertFalse(processor.artText(png(opaqueArgb), 20, 10).isAlpha());

        opaqueArgb.setRGB(0, 0, 0x00FFFFFF);
        assertTrue(processor.artText(png(opaqueArgb), 20, 10).isAlpha());
    }

    @Test
    void containKeepsExactCanvasSize() throws Exception
    {
        BackgroundSlotSpec slot = new BackgroundSlotSpec(); slot.setOutputWidth(100); slot.setOutputHeight(100); slot.setRenderMode("CONTAIN");
        DecoratorImageProcessor.ProcessedImage result = processor.background(png(new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB)), slot);
        assertEquals(100, result.getWidth()); assertEquals(100, result.getHeight()); assertTrue(result.isAlpha());
    }

    private byte[] png(BufferedImage image) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
