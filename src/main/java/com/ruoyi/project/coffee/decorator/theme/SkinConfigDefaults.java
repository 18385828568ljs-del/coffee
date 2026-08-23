package com.ruoyi.project.coffee.decorator.theme;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class SkinConfigDefaults
{
    private static final String DEFAULT_CONFIG = "{\"schemaVersion\":1,\"themeVersion\":1,"
            + "\"page\":{\"backgroundColor\":\"#FFFFFF\",\"textColor\":\"#332C28\",\"secondaryTextColor\":\"#8A7D74\"},"
            + "\"slots\":{\"shopHeader\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#FFFFFF\",\"backgroundImage\":null,\"fit\":\"fill\",\"radius\":0},\"heroBanner\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#6F4E37\",\"backgroundImage\":null,\"fit\":\"cover\"},"
            + "\"orderCard\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#FFFFFF\",\"backgroundImage\":null,\"iconColor\":\"#745848\",\"textColor\":\"#302720\",\"secondaryTextColor\":\"#C28B62\",\"radius\":20,\"shadow\":\"light\"},"
            + "\"shopCard\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#FFFFFF\",\"backgroundImage\":null,\"iconColor\":\"#332C28\",\"textColor\":\"#302720\",\"secondaryTextColor\":\"#8A7D74\",\"radius\":20,\"shadow\":\"light\"},"
            + "\"welcomeBanner\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#E8D4C3\",\"backgroundImage\":null,\"fit\":\"cover\",\"radius\":8},"
            + "\"aboutSection\":{\"backgroundColor\":\"#FFFFFF\",\"titleColor\":\"#332C28\",\"image\":\"\"},"
            + "\"specPanel\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#FFFFFF\",\"backgroundImage\":null,\"fit\":\"fill\",\"radius\":28,\"background\":{\"type\":\"solid\",\"color\":\"#FFFFFF\"}},"
            + "\"meProfileHeader\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#6F4E37\",\"backgroundImage\":null,\"fit\":\"fill\",\"radius\":24,\"background\":{\"type\":\"solid\",\"color\":\"#6F4E37\"}},"
            + "\"memberCard\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#7B5138\",\"backgroundImage\":null,\"fit\":\"fill\",\"radius\":24,\"background\":{\"type\":\"solid\",\"color\":\"#7B5138\"}},"
            + "\"meOrderCenter\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#FFFFFF\",\"backgroundImage\":null,\"fit\":\"fill\",\"radius\":24,\"background\":{\"type\":\"solid\",\"color\":\"#FFFFFF\"}},"
            + "\"meAddressCard\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#FFFFFF\",\"backgroundImage\":null,\"fit\":\"fill\",\"radius\":24,\"background\":{\"type\":\"solid\",\"color\":\"#FFFFFF\"}},"
            + "\"tabBar\":{\"backgroundColor\":\"#FFFFFF\",\"textColor\":\"#777777\",\"activeTextColor\":\"#44352C\",\"iconColor\":\"#999999\",\"activeIconColor\":\"#44352C\",\"activeBackgroundColor\":\"#F3E4D6\"}}}";

    private final ObjectMapper objectMapper;

    @Autowired
    public SkinConfigDefaults(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public JsonNode create()
    {
        try
        {
            ObjectNode config = (ObjectNode) objectMapper.readTree(DEFAULT_CONFIG);
            ObjectNode page = (ObjectNode) config.path("page");
            ObjectNode pageBackground = page.putObject("background");
            pageBackground.put("type", "solid");
            pageBackground.put("color", "#FFFFFF");
            config.set("colors", objectMapper.readTree("{\"primary\":\"#6F4E37\",\"pageBackground\":\"#FFFFFF\",\"cardBackground\":\"#FFFFFF\",\"textPrimary\":\"#332C28\",\"textSecondary\":\"#8A7D74\"}"));

            ObjectNode assets = objectMapper.createObjectNode();
            for (String key : ThemeConfigValidator.SKIN_COMPONENT_KEYS)
            {
                if ("homeBanner".equals(key)) assets.putArray(key);
                else assets.putNull(key);
            }
            config.set("assets", assets);
            config.set("productImages", objectMapper.createObjectNode());
            config.set("decorations", objectMapper.createObjectNode());

            ObjectNode homeBannerContent = config.putObject("content").putObject("homeBanner");
            homeBannerContent.put("visible", true);
            homeBannerContent.put("title", "一杯好咖啡，从这里开始");
            homeBannerContent.put("subtitle", "现点现做，认真对待每一杯");
            ObjectNode shopHeaderContent = (ObjectNode) config.with("content").putObject("shopHeader");
            shopHeaderContent.put("title", "一杯好咖啡");
            shopHeaderContent.put("subtitle", "从这里开始");
            shopHeaderContent.putNull("logoAssetId");

            ObjectNode homeBannerLayout = config.putObject("layout").putObject("homeBanner");
            homeBannerLayout.put("contentPreset", "LEFT_CENTER");
            homeBannerLayout.put("safeAreaPreset", "LEFT_CENTER_LARGE");

            ObjectNode typography = objectMapper.createObjectNode();
            addTypography(typography, "pageTitle", "#332C28", 18, 700, 1.3);
            addTypography(typography, "sectionTitle", "#332C28", 16, 700, 1.35);
            addTypography(typography, "bannerTitle", "#332C28", 26, 800, 1.2);
            addTypography(typography, "bannerSubtitle", "#88776A", 12, 500, 1.5);
            addTypography(typography, "actionTitle", "#38271F", 15, 700, 1.3);
            addTypography(typography, "actionSubtitle", "#88776A", 10, 500, 1.4);
            addTypography(typography, "productTitle", "#38271F", 18, 700, 1.3);
            addTypography(typography, "price", "#875629", 22, 800, 1.2);
            addTypography(typography, "metaText", "#88776A", 11, 400, 1.4);
            addTypography(typography, "bodyText", "#57483E", 13, 400, 1.55);
            addTypography(typography, "panelTitle", "#332C28", 17, 700, 1.35);
            addTypography(typography, "optionTitle", "#38271F", 14, 700, 1.35);
            addTypography(typography, "optionText", "#57483E", 13, 500, 1.35);
            addTypography(typography, "buttonPrimary", "#FFFFFF", 15, 700, 1.2);
            addTypography(typography, "buttonSecondary", "#6F4E37", 14, 600, 1.2);
            addTypography(typography, "memberTitle", "#FFF9EF", 16, 700, 1.3);
            addTypography(typography, "memberValue", "#FFFFFF", 22, 800, 1.2);
            addTypography(typography, "emptyTitle", "#38271F", 17, 700, 1.35);
            addTypography(typography, "emptyDescription", "#88776A", 12, 400, 1.55);
            addTypography(typography, "tabText", "#8A7B70", 11, 400, 1.2);
            addTypography(typography, "tabTextActive", "#875629", 11, 700, 1.2);
            config.set("typography", typography);
            return config;
        }
        catch (JsonProcessingException e) { throw new IllegalStateException("Default SkinConfig is invalid", e); }
    }

    private void addTypography(ObjectNode typography, String role, String color,
            int fontSize, int fontWeight, double lineHeight)
    {
        ObjectNode token = typography.putObject(role);
        token.put("color", color);
        token.put("fontSize", fontSize);
        token.put("fontWeight", fontWeight);
        token.put("fontStyle", "normal");
        token.put("lineHeight", lineHeight);
    }
}
