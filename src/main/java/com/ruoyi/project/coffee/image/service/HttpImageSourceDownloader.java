package com.ruoyi.project.coffee.image.service;

import java.io.IOException;
import org.springframework.stereotype.Component;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

@Component
public class HttpImageSourceDownloader implements ImageSourceDownloader
{
    @Override
    public byte[] download(String imageUrl) throws IOException
    {
        HttpResponse response = HttpRequest.get(imageUrl)
            .timeout(30000)
            .execute();
        if (!response.isOk())
        {
            throw new IOException("原图下载失败，HTTP状态：" + response.getStatus());
        }
        return response.bodyBytes();
    }
}
