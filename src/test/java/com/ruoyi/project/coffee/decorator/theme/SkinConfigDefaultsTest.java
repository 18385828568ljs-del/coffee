package com.ruoyi.project.coffee.decorator.theme;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class SkinConfigDefaultsTest
{
    @Test
    void defaultSkinIsValidAndStartsWithWhitePage()
    {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode config = new SkinConfigDefaults(objectMapper).create();

        assertEquals(1, config.path("schemaVersion").asInt());
        assertEquals("#FFFFFF", config.path("page").path("backgroundColor").asText());
        assertEquals(12, config.path("assets").size());
        assertEquals(true, config.path("assets").path("homeBanner").isArray());
        assertEquals(0, config.path("assets").path("homeBanner").size());
        assertEquals(true, config.path("assets").has("aboutImage"));
        assertEquals(true, config.path("assets").path("aboutImage").isNull());
        assertEquals(true, config.path("productImages").isObject());
        assertEquals(0, config.path("productImages").size());
        assertEquals(24, config.path("slots").path("meProfileHeader").path("radius").asInt());
        assertEquals(24, config.path("slots").path("meOrderCenter").path("radius").asInt());
        assertEquals(24, config.path("slots").path("meAddressCard").path("radius").asInt());
        assertEquals(21, config.path("typography").size());
        assertEquals(true, config.path("content").path("homeBanner").path("visible").asBoolean());
        assertEquals("一杯好咖啡，从这里开始", config.path("content").path("homeBanner").path("title").asText());
        assertDoesNotThrow(() -> new ThemeConfigValidator(objectMapper).validate(config.toString()));
    }
}
