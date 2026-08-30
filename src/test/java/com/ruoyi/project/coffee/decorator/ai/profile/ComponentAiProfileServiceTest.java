package com.ruoyi.project.coffee.decorator.ai.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.asset.BackgroundSlotService;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;
import com.ruoyi.project.coffee.decorator.mapper.ComponentAiProfileMapper;

class ComponentAiProfileServiceTest
{
    private static final List<String> KEYS = Arrays.asList("homeBanner","actionCard","sectionBanner","aboutImage","productCard","specPanel","emptyCart","cartPanel","checkoutBar","meProfileHeader","memberCard","meOrderCenter","meAddressCard","tabBar");
    private ComponentAiProfileService service;
    private ComponentAiProfileMapper mapper;
    private BackgroundSlotService slotService;

    @BeforeEach
    void setUp()
    {
        service = new ComponentAiProfileService();
        mapper = mock(ComponentAiProfileMapper.class);
        slotService = mock(BackgroundSlotService.class);
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "slotService", slotService);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    }

    @Test
    void exposesAllRegisteredBackgroundProfiles()
    {
        List<ComponentAiProfile> profiles = new ArrayList<ComponentAiProfile>();
        for (String key : KEYS)
        {
            ComponentAiProfile profile = profile(key, "[\"BACKGROUND\"]", "[\"NO_TEXT\"]");
            profiles.add(profile); when(mapper.selectActive(key)).thenReturn(profile);
            BackgroundSlotSpec slot = new BackgroundSlotSpec(); slot.setComponentKey(key);
            when(slotService.require(key)).thenReturn(slot);
        }
        when(mapper.selectActiveList()).thenReturn(profiles);
        assertEquals(KEYS.size(), service.capabilities().size());
    }

    @Test
    void enforcesTextModeWhitelist()
    {
        ComponentAiProfile home = profile("homeBanner", "[\"BACKGROUND\",\"ART_TEXT\",\"BACKGROUND_WITH_TEXT\"]", "[\"NO_TEXT\",\"ART_TEXT_LAYER\",\"EMBEDDED_TEXT\"]");
        ComponentAiProfile product = profile("productCard", "[\"BACKGROUND\"]", "[\"NO_TEXT\"]");
        ComponentAiProfile tab = profile("tabBar", "[\"BACKGROUND\"]", "[\"NO_TEXT\"]");
        ComponentAiProfile section = profile("sectionBanner", "[\"BACKGROUND\",\"ART_TEXT\"]", "[\"NO_TEXT\",\"ART_TEXT_LAYER\"]");
        assertTrue(service.allowsText(home, "EMBEDDED_TEXT"));
        assertFalse(service.allowsText(product, "EMBEDDED_TEXT"));
        assertFalse(service.allowsText(tab, "ART_TEXT_LAYER"));
        assertTrue(service.allowsText(section, "ART_TEXT_LAYER"));
    }

    @Test
    void rejectsDefaultTextModeOutsideAllowedModes()
    {
        ComponentAiProfile invalid = profile("productCard", "[\"BACKGROUND\"]", "[\"NO_TEXT\"]");
        invalid.setDefaultTextMode("EMBEDDED_TEXT"); when(mapper.selectActive("productCard")).thenReturn(invalid);
        assertThrows(ServiceException.class, () -> service.require("productCard"));
    }

    private ComponentAiProfile profile(String key, String modes, String textModes)
    {
        ComponentAiProfile result = new ComponentAiProfile(); result.setSlotKey(key); result.setProfileVersion(1);
        result.setEnabled(true); result.setGenerationModes(modes); result.setAllowedTextModes(textModes);
        result.setDefaultTextMode("NO_TEXT"); result.setRequiredFieldsJson("[\"stylePreset\"]");
        result.setOptionalFieldsJson("[]"); result.setForbiddenElementsJson("[]"); result.setStatus("ACTIVE");
        return result;
    }
}
