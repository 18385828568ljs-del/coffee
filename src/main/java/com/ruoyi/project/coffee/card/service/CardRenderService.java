package com.ruoyi.project.coffee.card.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import com.ruoyi.project.coffee.card.domain.AiCard;
import com.ruoyi.project.coffee.image.service.ImageSourceDownloader;
import com.ruoyi.project.common.storage.FileStorageService;

@Service
public class CardRenderService
{
    private final CardTemplateRenderer renderer;
    private final ImageSourceDownloader downloader;
    private final FileStorageService storage;

    public CardRenderService(CardTemplateRenderer renderer, ImageSourceDownloader downloader, FileStorageService storage)
    {
        this.renderer = renderer;
        this.downloader = downloader;
        this.storage = storage;
    }

    public String renderAndUpload(AiCard card, String artworkUrl) throws IOException
    {
        BufferedImage artwork = read(artworkUrl, true);
        BufferedImage logo = read(card.getLogoUrl(), false);
        byte[] bytes = renderer.render(card, artwork, logo);
        return storage.upload(new CardBytesMultipartFile("ai-card-v" + card.getVersion() + ".png", bytes)).getUrl();
    }

    private BufferedImage read(String url, boolean required) throws IOException
    {
        if (url == null || url.trim().isEmpty())
        {
            if (required) throw new IOException("插画地址不能为空");
            return null;
        }
        try
        {
            return ImageIO.read(new ByteArrayInputStream(downloader.download(url)));
        }
        catch (IOException e)
        {
            if (required) throw e;
            return null;
        }
    }
}
