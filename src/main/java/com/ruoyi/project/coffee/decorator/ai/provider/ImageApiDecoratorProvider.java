package com.ruoyi.project.coffee.decorator.ai.provider;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.project.coffee.image.client.ImageGenerationClient;
import com.ruoyi.project.coffee.image.domain.ImageGenerationRequest;
import com.ruoyi.project.coffee.image.domain.ImageGenerationResult;
import com.ruoyi.project.coffee.image.service.ImageSourceDownloader;

@Component
public class ImageApiDecoratorProvider implements DecoratorAssetGenerationProvider
{
    @Autowired private ImageGenerationClient client;
    @Autowired private ImageSourceDownloader downloader;

    @Override
    public byte[] generate(String prompt, String referenceUrl, boolean transparentBackground)
    {
        return generate(prompt, referenceUrl, transparentBackground, 0, 0);
    }

    @Override
    public byte[] generate(String prompt, String referenceUrl, boolean transparentBackground, int targetWidth, int targetHeight)
    {
        String source = null;
        if (referenceUrl != null && !referenceUrl.isEmpty())
        {
            try
            {
                byte[] reference = downloader.download(referenceUrl);
                String mimeType = detectMimeType(reference);
                source = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(reference);
            }
            catch (Exception e)
            {
                throw new IllegalStateException("参考素材下载失败: " + rootMessage(e), e);
            }
        }
        try
        {
            ImageGenerationResult result = client.generate(new ImageGenerationRequest(source, prompt,
                    transparentBackground ? "transparent" : "opaque", targetWidth, targetHeight));
            return Base64.getDecoder().decode(result.getImageBase64());
        }
        catch (Exception e)
        {
            throw new IllegalStateException("AI图片服务请求失败: " + rootMessage(e), e);
        }
    }

    private String rootMessage(Throwable throwable)
    {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) current = current.getCause();
        return current == null || current.getMessage() == null ? "未知错误" : current.getMessage();
    }

    private String detectMimeType(byte[] bytes)
    {
        if (bytes == null || bytes.length < 4) throw new IllegalStateException("参考素材内容为空或格式无法识别");
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47)
            return "image/png";
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff)
            return "image/jpeg";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P')
            return "image/webp";
        throw new IllegalStateException("参考素材格式不支持，AI 编辑接口仅支持 PNG、JPEG 或 WebP");
    }
}
