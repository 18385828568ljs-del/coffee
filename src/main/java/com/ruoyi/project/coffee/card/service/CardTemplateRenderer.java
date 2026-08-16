package com.ruoyi.project.coffee.card.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import com.ruoyi.project.coffee.card.domain.AiCard;

@Component
public class CardTemplateRenderer
{
    public static final int WIDTH = 900;
    public static final int HEIGHT = 1200;

    public byte[] render(AiCard card, BufferedImage artwork, BufferedImage logo) throws IOException
    {
        if (artwork == null)
        {
            throw new IOException("AI插画无法读取");
        }
        Palette p = Palette.from(card.getPaletteCode());
        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try
        {
            applyQuality(g);
            g.setColor(p.dark);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            drawArtworkPanel(g, artwork, p);
            drawHeader(g, card, p);
            drawInfoBox(g, 55, 790, 375, 220, card.getLeftProductName(), card.getLeftDescription(), p);
            drawInfoBox(g, 470, 790, 375, 220, card.getRightProductName(), card.getRightDescription(), p);
            drawBrand(g, card, logo, p);
        }
        finally
        {
            g.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(canvas, "png", output))
        {
            throw new IOException("卡片PNG编码失败");
        }
        return output.toByteArray();
    }

    private void drawArtworkPanel(Graphics2D g, BufferedImage artwork, Palette p)
    {
        Path2D border = zigzag(38, 30, 824, 730, 14);
        g.setColor(p.accent);
        g.fill(border);
        int x = 54, y = 46, w = 792, h = 710;
        double scale = Math.max((double) w / artwork.getWidth(), (double) h / artwork.getHeight());
        int dw = (int) Math.ceil(artwork.getWidth() * scale);
        int dh = (int) Math.ceil(artwork.getHeight() * scale);
        java.awt.Shape oldClip = g.getClip();
        g.setClip(x, y, w, h);
        g.drawImage(artwork, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, null);
        g.setClip(oldClip);
        g.setColor(p.ink);
        g.setStroke(new BasicStroke(5f));
        g.draw(border);
    }

    private void drawHeader(Graphics2D g, AiCard card, Palette p)
    {
        String english = safe(card.getEnglishTitle(), "COFFEE CARD").toUpperCase();
        drawFittedText(g, english, 70, 72, 500, 74, true, p.ink);
        drawFittedText(g, safe(card.getTitle(), "今日咖啡"), 585, 72, 245, 64, true, p.ink);
    }

    private void drawInfoBox(Graphics2D g, int x, int y, int w, int h, String name, String description, Palette p)
    {
        g.setColor(p.panel);
        g.fillRect(x, y, w, h);
        g.setColor(p.ink);
        g.setStroke(new BasicStroke(5f));
        g.drawRect(x, y, w, h);
        drawFittedText(g, safe(name, "咖啡特调"), x + 22, y + 32, w - 44, 44, true, p.ink);
        drawWrapped(g, safe(description, "今日风味推荐"), x + 24, y + 105, w - 48, 2, 30, p.ink);
    }

    private void drawBrand(Graphics2D g, AiCard card, BufferedImage logo, Palette p)
    {
        g.setColor(p.dark);
        g.fillRect(0, 1035, WIDTH, 165);
        drawWrapped(g, safe(card.getBrandName(), "COFFEE STREET"), 54, 1080, 560, 2, 46, Color.WHITE);
        if (logo != null)
        {
            int size = 105;
            g.drawImage(logo, 730, 1060, size, size, null);
        }
        else
        {
            g.setColor(p.panel);
            g.fillOval(732, 1062, 100, 100);
            g.setColor(p.ink);
            g.setStroke(new BasicStroke(4f));
            g.drawOval(732, 1062, 100, 100);
            drawFittedText(g, "COFFEE", 746, 1095, 72, 24, true, p.ink);
        }
    }

    private void drawFittedText(Graphics2D g, String text, int x, int y, int maxWidth, int size, boolean bold, Color color)
    {
        int current = size;
        Font font;
        do
        {
            font = font(current, bold);
            current -= 2;
        }
        while (current >= 12 && g.getFontMetrics(font).stringWidth(text) > maxWidth);
        g.setFont(font);
        g.setColor(color);
        java.awt.Shape oldClip = g.getClip();
        g.clipRect(x, y, maxWidth, size + 12);
        g.drawString(text, x, y + g.getFontMetrics().getAscent());
        g.setClip(oldClip);
    }

    private void drawWrapped(Graphics2D g, String text, int x, int y, int maxWidth, int maxLines, int size, Color color)
    {
        Font font = font(size, true);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = wrap(text, fm, maxWidth, maxLines);
        g.setColor(color);
        for (int i = 0; i < lines.size(); i++)
        {
            g.drawString(lines.get(i), x, y + fm.getAscent() + i * (fm.getHeight() + 3));
        }
    }

    private List<String> wrap(String text, FontMetrics fm, int maxWidth, int maxLines)
    {
        List<String> lines = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (c == '\n' || (line.length() > 0 && fm.stringWidth(line.toString() + c) > maxWidth))
            {
                if (lines.size() == maxLines - 1)
                {
                    lines.add(ellipsize(line.toString(), fm, maxWidth));
                    line.setLength(0);
                    break;
                }
                lines.add(line.toString());
                line.setLength(0);
                if (c == '\n') continue;
            }
            line.append(c);
        }
        if (line.length() > 0 && lines.size() < maxLines) lines.add(line.toString());
        if (lines.size() > 1)
        {
            int last = lines.size() - 1;
            String tail = lines.get(last);
            String previous = lines.get(last - 1);
            if (tail.length() == 1 && previous.length() > 1 && !tail.endsWith("\u2026"))
            {
                lines.set(last - 1, previous.substring(0, previous.length() - 1));
                lines.set(last, previous.substring(previous.length() - 1) + tail);
            }
        }
        return lines;
    }

    private String ellipsize(String text, FontMetrics fm, int maxWidth)
    {
        String suffix = "\u2026";
        String value = text;
        while (value.length() > 0 && fm.stringWidth(value + suffix) > maxWidth)
        {
            value = value.substring(0, value.length() - 1);
        }
        return value + suffix;
    }

    private Font font(int size, boolean bold)
    {
        String[] candidates = { "Noto Sans CJK SC", "Microsoft YaHei", "PingFang SC", "SansSerif" };
        java.util.Set<String> available = new java.util.HashSet<String>();
        for (String name : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) available.add(name);
        for (String candidate : candidates)
        {
            if (available.contains(candidate)) return new Font(candidate, bold ? Font.BOLD : Font.PLAIN, size);
        }
        return new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size);
    }

    private Path2D zigzag(int x, int y, int w, int h, int step)
    {
        Path2D path = new Path2D.Double();
        path.moveTo(x, y);
        for (int px = x; px <= x + w; px += step) path.lineTo(px, y + (((px - x) / step) % 2 == 0 ? 0 : 8));
        for (int py = y; py <= y + h; py += step) path.lineTo(x + w - (((py - y) / step) % 2 == 0 ? 0 : 8), py);
        for (int px = x + w; px >= x; px -= step) path.lineTo(px, y + h - (((x + w - px) / step) % 2 == 0 ? 0 : 8));
        for (int py = y + h; py >= y; py -= step) path.lineTo(x + (((y + h - py) / step) % 2 == 0 ? 0 : 8), py);
        path.closePath();
        return path;
    }

    private void applyQuality(Graphics2D g)
    {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private String safe(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value.trim(); }

    private static class Palette
    {
        private final Color dark, ink, accent, panel;
        private Palette(Color dark, Color ink, Color accent, Color panel) { this.dark=dark; this.ink=ink; this.accent=accent; this.panel=panel; }
        private static Palette from(String code)
        {
            if ("mint".equals(code)) return new Palette(new Color(37,65,57),new Color(27,31,29),new Color(94,198,176),new Color(251,231,98));
            if ("mocha".equals(code)) return new Palette(new Color(83,38,20),new Color(42,25,19),new Color(227,116,65),new Color(245,220,155));
            return new Palette(new Color(92,42,18),new Color(31,27,24),new Color(15,139,172),new Color(255,222,75));
        }
    }
}
