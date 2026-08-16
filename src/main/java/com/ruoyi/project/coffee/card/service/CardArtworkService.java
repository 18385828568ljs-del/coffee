package com.ruoyi.project.coffee.card.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import com.ruoyi.project.coffee.card.domain.AiCard;
import com.ruoyi.project.coffee.image.client.ImageGenerationClient;
import com.ruoyi.project.coffee.image.domain.ImageGenerationRequest;
import com.ruoyi.project.coffee.image.domain.ImageGenerationResult;
import com.ruoyi.project.coffee.image.service.ImageSourceDownloader;
import com.ruoyi.project.common.storage.FileStorageService;

@Service
public class CardArtworkService
{
    private final ImageGenerationClient imageClient;
    private final ImageSourceDownloader downloader;
    private final FileStorageService storage;

    public CardArtworkService(ImageGenerationClient imageClient, ImageSourceDownloader downloader, FileStorageService storage)
    {
        this.imageClient = imageClient;
        this.downloader = downloader;
        this.storage = storage;
    }

    public String generate(AiCard card) throws IOException
    {
        byte[] reference = buildReferenceBoard(card);
        String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(reference);
        ImageGenerationResult result = imageClient.generate(new ImageGenerationRequest(dataUrl, buildPrompt(card)));
        byte[] bytes = Base64.getDecoder().decode(result.getImageBase64());
        validateArtwork(bytes);
        return storage.upload(new CardBytesMultipartFile("card-artwork.png", bytes)).getUrl();
    }

    private byte[] buildReferenceBoard(AiCard card) throws IOException
    {
        BufferedImage board = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = board.createGraphics();
        try
        {
            g.setColor(new Color(246, 226, 118));
            g.fillRect(0, 0, 1024, 1024);
            drawReference(g, card.getLeftProductImage(), 50, 50, 442, 924);
            drawReference(g, card.getRightProductImage(), 532, 50, 442, 924);
        }
        finally { g.dispose(); }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(board, "png", output);
        return output.toByteArray();
    }

    private void drawReference(Graphics2D g, String url, int x, int y, int w, int h)
    {
        g.setColor(new Color(244, 132, 164));
        g.fillRect(x, y, w, h);
        if (url == null || url.trim().isEmpty()) return;
        try
        {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(downloader.download(url)));
            if (image == null) return;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            double scale = Math.min((double) w / image.getWidth(), (double) h / image.getHeight());
            int dw=(int)(image.getWidth()*scale), dh=(int)(image.getHeight()*scale);
            g.drawImage(image, x+(w-dw)/2, y+(h-dh)/2, dw, dh, null);
        }
        catch (Exception ignored) { }
    }

    private String buildPrompt(AiCard card)
    {
        return "Create a square editorial illustration layer for a coffee card. Retro 1950s hand-drawn comic, "
            + "bold black ink outlines, halftone print texture, playful collage colors, expressive anthropomorphic coffee cups. "
            + "Use the reference board only as product inspiration. Left concept: " + safe(card.getLeftProductName())
            + ", " + safe(card.getLeftDescription()) + ". Right concept: " + safe(card.getRightProductName())
            + ", " + safe(card.getRightDescription()) + ". Scene: " + safe(card.getThemePrompt())
            + ". IMPORTANT: image only; no words, no letters, no numbers, no logos, no labels, no borders, no frames. "
            + "Keep the main characters centered with clear space near the top and bottom for later typography.";
    }

    private void validateArtwork(byte[] bytes) throws IOException
    {
        if (bytes == null || bytes.length < 4096 || bytes.length > 20 * 1024 * 1024) throw new IOException("AI插画文件大小异常");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null || image.getWidth() < 512 || image.getHeight() < 512) throw new IOException("AI插画尺寸不足，至少需要512x512");
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }
}
