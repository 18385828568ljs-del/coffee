package com.ruoyi.project.coffee.card.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import com.ruoyi.project.coffee.card.domain.AiCard;
import javax.imageio.ImageIO;

class CardTemplateRendererTest
{
    @Test
    void rendersDynamicCardAsStablePng() throws Exception
    {
        BufferedImage artwork=new BufferedImage(768,768,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=artwork.createGraphics(); g.setColor(new Color(30,150,180)); g.fillRect(0,0,768,768); g.dispose();
        AiCard card=new AiCard();
        card.setTitle("游野组合"); card.setEnglishTitle("COMBO"); card.setLeftProductName("美式咖啡");
        card.setLeftDescription("柑橘 / 榛果 / 热带水果"); card.setRightProductName("奶咖");
        card.setRightDescription("黄油曲奇 / 伯爵茶"); card.setBrandName("COFFEE STREET"); card.setPaletteCode("candy");

        byte[] png=new CardTemplateRenderer().render(card,artwork,null);
        BufferedImage result=ImageIO.read(new ByteArrayInputStream(png));

        assertNotNull(result);
        assertEquals(CardTemplateRenderer.WIDTH,result.getWidth());
        assertEquals(CardTemplateRenderer.HEIGHT,result.getHeight());
        assertTrue(png.length>10000);
    }
}
