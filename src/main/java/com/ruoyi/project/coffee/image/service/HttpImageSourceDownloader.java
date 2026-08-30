package com.ruoyi.project.coffee.image.service;

import java.io.IOException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Component;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.framework.config.RuoYiConfig;

@Component
public class HttpImageSourceDownloader implements ImageSourceDownloader
{
    @Override
    public byte[] download(String imageUrl) throws IOException
    {
        if (imageUrl == null || imageUrl.trim().isEmpty())
        {
            throw new IOException("参考素材地址为空");
        }
        String normalizedUrl = imageUrl.trim();
        if (isLocalResource(normalizedUrl))
        {
            return readLocalResource(normalizedUrl);
        }
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://"))
        {
            throw new IOException("参考素材地址必须是 http(s) URL 或本地 /profile 资源路径");
        }
        HttpResponse response = HttpRequest.get(imageUrl)
            .setProxy(Proxy.NO_PROXY)
            .timeout(30000)
            .execute();
        if (!response.isOk())
        {
            throw new IOException("原图下载失败，HTTP状态：" + response.getStatus());
        }
        return response.bodyBytes();
    }

    private boolean isLocalResource(String imageUrl)
    {
        return imageUrl.equals(Constants.RESOURCE_PREFIX)
                || imageUrl.startsWith(Constants.RESOURCE_PREFIX + "/");
    }

    private byte[] readLocalResource(String resourceUrl) throws IOException
    {
        String profile = RuoYiConfig.getProfile();
        if (profile == null || profile.trim().isEmpty())
        {
            throw new IOException("本地参考素材目录未配置");
        }
        String relative = resourceUrl.substring(Constants.RESOURCE_PREFIX.length())
                .replace('/', java.io.File.separatorChar)
                .replace('\\', java.io.File.separatorChar);
        while (relative.startsWith(java.io.File.separator)) relative = relative.substring(1);
        Path root = Paths.get(profile).toAbsolutePath().normalize();
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root))
        {
            throw new IOException("参考素材路径非法");
        }
        if (!Files.isRegularFile(file))
        {
            throw new IOException("参考素材文件不存在: " + resourceUrl);
        }
        return Files.readAllBytes(file);
    }
}
