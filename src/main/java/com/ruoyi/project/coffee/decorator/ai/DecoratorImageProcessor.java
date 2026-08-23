package com.ruoyi.project.coffee.decorator.ai;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;

@Component
public class DecoratorImageProcessor
{
    public int[] sourceSize(byte[] source)
    {
        try
        {
            BufferedImage input = ImageIO.read(new ByteArrayInputStream(source));
            if (input == null) throw new IllegalStateException("模型返回的图片无法识别");
            return new int[] { input.getWidth(), input.getHeight() };
        }
        catch (Exception e) { throw new IllegalStateException("模型返回的图片无法识别", e); }
    }

    public ProcessedImage background(byte[] source, BackgroundSlotSpec slot)
    {
        String mode = slot.getRenderMode() == null ? "COVER" : slot.getRenderMode().toUpperCase();
        if ("CONTAIN".equals(mode)) return contain(source, slot.getOutputWidth(), slot.getOutputHeight());
        if ("STRETCH".equals(mode) || "FILL".equals(mode)) return stretch(source, slot.getOutputWidth(), slot.getOutputHeight());
        return background(source, slot.getOutputWidth(), slot.getOutputHeight());
    }

    public ProcessedImage background(byte[] source, int targetWidth, int targetHeight)
    {
        try
        {
            BufferedImage input = ImageIO.read(new ByteArrayInputStream(source));
            if (input == null) throw new IllegalStateException("模型返回的图片无法识别");
            double scale = Math.max((double) targetWidth / input.getWidth(), (double) targetHeight / input.getHeight());
            int cropWidth = Math.max(1, (int) Math.round(targetWidth / scale));
            int cropHeight = Math.max(1, (int) Math.round(targetHeight / scale));
            int cropX = Math.max(0, (input.getWidth() - cropWidth) / 2);
            int cropY = Math.max(0, (input.getHeight() - cropHeight) / 2);
            return render(input.getSubimage(cropX, cropY, cropWidth, cropHeight), targetWidth, targetHeight, false);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("AI 图片后处理失败: " + e.getMessage(), e);
        }
    }

    private ProcessedImage preserveFullFrame(BufferedImage input, int targetWidth, int targetHeight) throws Exception
    {
        return preserveFullFrame(input, targetWidth, targetHeight, hasTransparency(input));
    }

    private ProcessedImage preserveFullFrame(BufferedImage input, int targetWidth, int targetHeight, boolean alpha) throws Exception
    {
        double scale = Math.min((double) targetWidth / input.getWidth(), (double) targetHeight / input.getHeight());
        int width = Math.max(1, (int) Math.round(input.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(input.getHeight() * scale));
        int left = (targetWidth - width) / 2;
        int top = (targetHeight - height) / 2;
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        // Extend the source edges into the unused area so the complete subject remains visible.
        if (left > 0)
        {
            graphics.drawImage(input, 0, top, left, top + height, 0, 0, 1, input.getHeight(), null);
            graphics.drawImage(input, left + width, top, targetWidth, top + height, input.getWidth() - 1, 0, input.getWidth(), input.getHeight(), null);
        }
        if (top > 0)
        {
            graphics.drawImage(input, left, 0, left + width, top, 0, 0, input.getWidth(), 1, null);
            graphics.drawImage(input, left, top + height, left + width, targetHeight, 0, input.getHeight() - 1, input.getWidth(), input.getHeight(), null);
        }
        graphics.drawImage(input, left, top, left + width, top + height, 0, 0, input.getWidth(), input.getHeight(), null);
        graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(output, "png", bytes);
        return new ProcessedImage(bytes.toByteArray(), targetWidth, targetHeight, alpha);
    }

    private ProcessedImage stretch(byte[] source, int targetWidth, int targetHeight)
    {
        try
        {
            BufferedImage input = ImageIO.read(new ByteArrayInputStream(source));
            if (input == null) throw new IllegalStateException("模型返回的图片无法识别");
            return render(input, targetWidth, targetHeight, hasTransparency(input));
        }
        catch (Exception e) { throw new IllegalStateException("AI 图片后处理失败: " + e.getMessage(), e); }
    }

    private ProcessedImage contain(byte[] source, int targetWidth, int targetHeight)
    {
        try
        {
            BufferedImage input = ImageIO.read(new ByteArrayInputStream(source));
            if (input == null) throw new IllegalStateException("模型返回的图片无法识别");
            return preserveFullFrame(input, targetWidth, targetHeight, true);
        }
        catch (Exception e) { throw new IllegalStateException("AI 图片后处理失败: " + e.getMessage(), e); }
    }

    public ProcessedImage artText(byte[] source, int maxWidth, int maxHeight)
    {
        try
        {
            BufferedImage input = ImageIO.read(new ByteArrayInputStream(source));
            if (input == null) throw new IllegalStateException("模型返回的图片无法识别");
            double scale = Math.min(1d, Math.min((double) maxWidth / input.getWidth(), (double) maxHeight / input.getHeight()));
            int width = Math.max(1, (int) Math.round(input.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(input.getHeight() * scale));
            BufferedImage output = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = output.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(input, (maxWidth - width) / 2, (maxHeight - height) / 2, width, height, null);
            graphics.dispose();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(output, "png", bytes);
            return new ProcessedImage(bytes.toByteArray(), maxWidth, maxHeight, hasTransparency(input));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("艺术字后处理失败: " + e.getMessage(), e);
        }
    }

    private boolean hasTransparency(BufferedImage image)
    {
        if (!image.getColorModel().hasAlpha()) return false;
        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = 0; x < image.getWidth(); x++)
            {
                if ((image.getRGB(x, y) >>> 24) < 255) return true;
            }
        }
        return false;
    }

    private ProcessedImage render(BufferedImage input, int width, int height, boolean alpha) throws Exception
    {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(input, 0, 0, width, height, null);
        graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(output, "png", bytes);
        return new ProcessedImage(bytes.toByteArray(), width, height, alpha);
    }

    public static class ProcessedImage
    {
        private final byte[] bytes;
        private final int width;
        private final int height;
        private final boolean alpha;
        public ProcessedImage(byte[] bytes, int width, int height, boolean alpha)
        { this.bytes = bytes; this.width = width; this.height = height; this.alpha = alpha; }
        public byte[] getBytes() { return bytes; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public boolean isAlpha() { return alpha; }
    }
}
