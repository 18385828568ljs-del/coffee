package com.ruoyi.project.coffee.decorator.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.decorator.theme.DecoratorThemeService;
import com.ruoyi.project.coffee.decorator.theme.domain.PublishedStoreTheme;

class CustomerThemeControllerTest
{
    private CustomerThemeController controller;
    private DecoratorThemeService themeService;

    @BeforeEach
    void setUp()
    {
        controller = new CustomerThemeController();
        themeService = mock(DecoratorThemeService.class);
        ReflectionTestUtils.setField(controller, "themeService", themeService);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
    }

    @Test
    void returnsPublishedSkinContract()
    {
        when(themeService.publishedStoreTheme("store-a")).thenReturn(publishedTheme());

        AjaxResult response = controller.skin("store-a");
        Map<?, ?> data = (Map<?, ?>) response.get("data");

        assertEquals(7, ((Number) data.get("version")).intValue());
        assertEquals("store-a", data.get("storeCode"));
        assertEquals("https://cdn.example.com/banner.png",
                ((Map<?, ?>) data.get("assetUrls")).get("11"));
        assertTrue(data.get("config") instanceof JsonNode);
        assertEquals("#F3E4D6", ((JsonNode) data.get("config")).path("page").path("backgroundColor").asText());
    }

    @Test
    void returnsPublishedVersionNumber()
    {
        when(themeService.publishedStoreTheme("store-a")).thenReturn(publishedTheme());

        AjaxResult response = controller.skinVersion("store-a");
        Map<?, ?> data = (Map<?, ?>) response.get("data");

        assertEquals(7, ((Number) data.get("version")).intValue());
    }

    private PublishedStoreTheme publishedTheme()
    {
        PublishedStoreTheme theme = new PublishedStoreTheme();
        theme.setStoreCode("store-a");
        theme.setVersionId(19L);
        theme.setVersionNo(7);
        theme.setConfigJson("{\"schemaVersion\":1,\"page\":{\"backgroundColor\":\"#F3E4D6\"}}");
        theme.setAssetUrls(Collections.singletonMap("11", "https://cdn.example.com/banner.png"));
        return theme;
    }
}
