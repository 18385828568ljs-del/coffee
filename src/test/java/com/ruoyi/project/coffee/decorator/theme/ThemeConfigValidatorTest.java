package com.ruoyi.project.coffee.decorator.theme;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

class ThemeConfigValidatorTest
{
    private ThemeConfigValidator validator;

    @BeforeEach
    void setUp()
    {
        validator = new ThemeConfigValidator(new ObjectMapper());
    }

    @Test
    void acceptsValidV1Config()
    {
        assertDoesNotThrow(() -> validator.validate(validConfig()));
    }

    @Test
    void acceptsSkinConfigV1()
    {
        assertDoesNotThrow(() -> validator.validate(validSkinConfig()));
    }

    @Test
    void rejectsUnknownSkinSlot()
    {
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(validSkinConfig().replace("\"tabBar\":", "\"freeLayout\":{},\"tabBar\":")));
    }

    @Test
    void rejectsExecutableFieldsInSkinConfig()
    {
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(validSkinConfig().replaceFirst("\\}$", ",\"javascript\":\"alert(1)\"}")));
    }

    @Test
    void rejectsUnsupportedSkinRadius()
    {
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(validSkinConfig().replace("\"radius\":20", "\"radius\":19")));
    }

    @Test
    void acceptsCompleteCanonicalSkinConfig()
    {
        ObjectNode config = canonicalSkinConfig();
        ((ObjectNode) config.path("assets")).put("aboutImage", "201");
        ObjectNode productImages = (ObjectNode) config.path("productImages");
        productImages.put("1", "202");
        productImages.putNull("2");
        assertDoesNotThrow(() -> validator.validate(config.toString()));
    }

    @Test
    void acceptsStructuredGradientBackground()
    {
        ObjectNode config = canonicalSkinConfig();
        ObjectNode page = (ObjectNode) config.path("page");
        ObjectNode gradient = page.putObject("background").put("type", "gradient")
                .putObject("gradient");
        gradient.put("type", "linear").put("direction", 135);
        ArrayNode colors = gradient.putArray("colors");
        colors.addObject().put("color", "#FFF3E8").put("position", 0);
        colors.addObject().put("color", "#8A5A44").put("position", 100);
        assertDoesNotThrow(() -> validator.validate(config.toString()));
    }

    @Test
    void rejectsUnsortedGradientStops()
    {
        ObjectNode config = canonicalSkinConfig();
        ObjectNode page = (ObjectNode) config.path("page");
        ObjectNode gradient = page.putObject("background").put("type", "gradient")
                .putObject("gradient");
        gradient.put("type", "linear").put("direction", 135);
        ArrayNode colors = gradient.putArray("colors");
        colors.addObject().put("color", "#FFF3E8").put("position", 80);
        colors.addObject().put("color", "#8A5A44").put("position", 20);
        assertThrows(ThemeConfigValidationException.class, () -> validator.validate(config.toString()));
    }

    @Test
    void acceptsHomeBannerCarouselAndLegacySingleAsset()
    {
        ObjectNode carousel = canonicalSkinConfig();
        ((ObjectNode) carousel.path("assets")).putArray("homeBanner")
                .add("201").add(202).add("/static/banner.jpg");
        assertDoesNotThrow(() -> validator.validate(carousel.toString()));

        ObjectNode legacy = canonicalSkinConfig();
        ((ObjectNode) legacy.path("assets")).put("homeBanner", "201");
        assertDoesNotThrow(() -> validator.validate(legacy.toString()));
    }

    @Test
    void rejectsInvalidOrOversizedHomeBannerCarousel()
    {
        ObjectNode invalid = canonicalSkinConfig();
        ((ObjectNode) invalid.path("assets")).putArray("homeBanner").addNull();
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(invalid.toString()));

        ObjectNode oversized = canonicalSkinConfig();
        for (int index = 0; index <= ThemeConfigValidator.MAX_HOME_BANNERS; index++)
        {
            ((ObjectNode) oversized.path("assets")).withArray("homeBanner")
                    .add(String.valueOf(300 + index));
        }
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(oversized.toString()));
    }

    @Test
    void rejectsInvalidProductImageMappings()
    {
        ObjectNode invalidProductId = canonicalSkinConfig();
        ((ObjectNode) invalidProductId.path("productImages")).put("coffee", "201");
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(invalidProductId.toString()));

        ObjectNode invalidAsset = canonicalSkinConfig();
        ((ObjectNode) invalidAsset.path("productImages")).putObject("1").put("assetId", 201);
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(invalidAsset.toString()));

        ObjectNode tooManyProducts = canonicalSkinConfig();
        ObjectNode productImages = (ObjectNode) tooManyProducts.path("productImages");
        for (int productId = 1; productId <= 201; productId++) productImages.put(String.valueOf(productId), "201");
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(tooManyProducts.toString()));
    }

    @Test
    void validatesEditableHomeBannerContent()
    {
        ObjectNode config = canonicalSkinConfig();
        ObjectNode homeBanner = (ObjectNode) config.path("content").path("homeBanner");
        homeBanner.put("visible", false);
        homeBanner.put("title", "今日咖啡");
        homeBanner.put("subtitle", "");
        assertDoesNotThrow(() -> validator.validate(config.toString()));

        homeBanner.put("title", "12345678901234567890123456789012345678901");
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(config.toString()));
    }

    @Test
    void rejectsUnknownCanonicalComponentAndTextRole()
    {
        ObjectNode unknownComponent = canonicalSkinConfig();
        ((ObjectNode) unknownComponent.path("assets")).putNull("freeLayout");
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(unknownComponent.toString()));

        ObjectNode unknownRole = canonicalSkinConfig();
        ((ObjectNode) unknownRole.path("typography")).set("floatingCaption",
                unknownRole.path("typography").path("bodyText").deepCopy());
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(unknownRole.toString()));
    }

    @Test
    void rejectsLayoutCoordinatesAtAnyDepth()
    {
        ObjectNode config = canonicalSkinConfig();
        ((ObjectNode) config.path("typography").path("productTitle")).put("left", 12);
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(config.toString()));
    }

    @Test
    void rejectsUnsupportedSchemaVersion()
    {
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(validConfig().replace("1.0.0", "2.0.0")));
    }

    @Test
    void rejectsUnknownVariant()
    {
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(validConfig().replace("\"centered\"", "\"free-layout\"")));
    }

    @Test
    void rejectsMismatchedBackgroundSlot()
    {
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(validConfig().replace("shopHeader.background", "productCard.background")));
    }

    @Test
    void rejectsOpacityOutsideRange()
    {
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(validConfig().replace("\"opacity\":1", "\"opacity\":1.2")));
    }

    @Test
    void rejectsOversizedConfig()
    {
        StringBuilder value = new StringBuilder(validConfig());
        while (value.length() <= ThemeConfigValidator.MAX_CONFIG_BYTES)
        {
            value.append(' ');
        }
        assertThrows(ThemeConfigValidationException.class, () -> validator.validate(value.toString()));
    }

    @Test
    void rejectsAssetIdOutsideLongRange()
    {
        assertThrows(ThemeConfigValidationException.class,
                () -> validator.validate(validConfig().replace("\"501\"", "\"999999999999999999999999\"")));
    }

    private static String validConfig()
    {
        return "{\"schemaVersion\":\"1.0.0\","
                + "\"tokens\":{\"colors\":{\"primary\":\"#6F4E37\"},"
                + "\"radius\":{\"card\":12},\"shadow\":{\"card\":\"soft\"}},"
                + "\"brand\":{\"logoAssetId\":\"501\"},"
                + "\"components\":{"
                + "\"shopHeader\":{\"variant\":\"centered\",\"background\":{"
                + "\"assetId\":\"601\",\"slotKey\":\"shopHeader.background\","
                + "\"slotSpecVersion\":1,\"renderMode\":\"cover\",\"opacity\":1}},"
                + "\"activityBanner\":{\"visible\":true,\"variant\":\"single\"},"
                + "\"categoryNav\":{\"variant\":\"icon-grid\"},"
                + "\"productCard\":{\"variant\":\"vertical\"},"
                + "\"tabBar\":{\"variant\":\"standard\"},"
                + "\"profileHeader\":{\"variant\":\"brand\"}}}";
    }

    private static String validSkinConfig()
    {
        return "{\"schemaVersion\":1,\"themeVersion\":1,"
                + "\"page\":{\"backgroundColor\":\"#F7F2EC\",\"textColor\":\"#332C28\",\"secondaryTextColor\":\"#8A7D74\"},"
                + "\"slots\":{"
                + "\"heroBanner\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#6F4E37\",\"fit\":\"cover\"},"
                + "\"orderCard\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#FFFFFF\",\"iconColor\":\"#745848\",\"textColor\":\"#302720\",\"secondaryTextColor\":\"#C28B62\",\"radius\":20,\"shadow\":\"light\"},"
                + "\"shopCard\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#FFFFFF\",\"iconColor\":\"#332C28\",\"textColor\":\"#302720\",\"secondaryTextColor\":\"#8A7D74\",\"radius\":20,\"shadow\":\"light\"},"
                + "\"welcomeBanner\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#E8D4C3\",\"fit\":\"cover\",\"radius\":8},"
                + "\"aboutSection\":{\"backgroundColor\":\"#F7F2EC\",\"titleColor\":\"#332C28\",\"image\":\"/static/about.jpg\"},"
                + "\"tabBar\":{\"backgroundColor\":\"#FFFFFF\",\"textColor\":\"#777777\",\"activeTextColor\":\"#44352C\",\"iconColor\":\"#999999\",\"activeIconColor\":\"#44352C\",\"activeBackgroundColor\":\"#F3E4D6\"}}}";
    }

    private ObjectNode canonicalSkinConfig()
    {
        return (ObjectNode) new SkinConfigDefaults(new ObjectMapper()).create();
    }
}
