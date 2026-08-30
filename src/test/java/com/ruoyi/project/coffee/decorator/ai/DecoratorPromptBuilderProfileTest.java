package com.ruoyi.project.coffee.decorator.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.decorator.ai.domain.DecoratorAiTask;
import com.ruoyi.project.coffee.decorator.ai.profile.ComponentAiProfile;
import com.ruoyi.project.coffee.decorator.ai.profile.ComponentAiProfileService;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;

class DecoratorPromptBuilderProfileTest
{
    @Test
    void backgroundPromptContainsProfileSafetyContext()
    {
        ComponentAiProfileService profiles = mock(ComponentAiProfileService.class);
        ComponentAiProfile profile = new ComponentAiProfile(); profile.setForbiddenElementsJson("[\"price\",\"button\"]");
        when(profiles.require("productCard")).thenReturn(profile);
        when(profiles.values(profile.getForbiddenElementsJson())).thenReturn(java.util.Arrays.asList("price", "button"));
        DecoratorPromptBuilder builder = new DecoratorPromptBuilder(); ReflectionTestUtils.setField(builder, "profileService", profiles);
        BackgroundSlotSpec slot = new BackgroundSlotSpec(); slot.setComponentKey("productCard"); slot.setOutputWidth(1372); slot.setOutputHeight(640); slot.setRenderMode("STRETCH");
        DecoratorAiTask task = new DecoratorAiTask(); task.setStylePreset("VINTAGE_COFFEE"); task.setPlacementPreset("RIGHT_CENTER"); task.setTextMode("NO_TEXT"); task.setPromptText("low contrast");
        String prompt = builder.background(task, slot);
        assertTrue(prompt.contains("productCard")); assertTrue(prompt.contains("1372x640")); assertTrue(prompt.contains("STRETCH"));
        assertTrue(prompt.contains("price")); assertTrue(prompt.contains("NO_TEXT")); assertTrue(prompt.contains("low contrast"));
    }

    @Test
    void embeddedTextUsesBackgroundPromptRules()
    {
        ComponentAiProfileService profiles = mock(ComponentAiProfileService.class);
        ComponentAiProfile profile = new ComponentAiProfile(); profile.setForbiddenElementsJson("[\"price\",\"button\"]");
        when(profiles.require("sectionBanner")).thenReturn(profile);
        when(profiles.values(profile.getForbiddenElementsJson())).thenReturn(java.util.Arrays.asList("price", "button"));
        DecoratorPromptBuilder builder = new DecoratorPromptBuilder(); ReflectionTestUtils.setField(builder, "profileService", profiles);
        BackgroundSlotSpec slot = new BackgroundSlotSpec(); slot.setComponentKey("sectionBanner"); slot.setOutputWidth(1372); slot.setOutputHeight(288); slot.setRenderMode("STRETCH");
        DecoratorAiTask task = new DecoratorAiTask(); task.setStylePreset("VINTAGE_COFFEE"); task.setPlacementPreset("CENTER"); task.setSizePreset("LARGE"); task.setTextMode("EMBEDDED_TEXT"); task.setTextContent("welcome");
        String prompt = builder.background(task, slot);
        assertTrue(prompt.contains("Include only the exact text 'welcome'"));
        assertTrue(prompt.contains("Text policy: EMBEDDED_TEXT"));
        assertTrue(prompt.contains("65% to 85% of the canvas width and at least 40% of the canvas height"));
        assertTrue(prompt.contains("Do not render the text as tiny decoration"));
        assertTrue(prompt.contains("Real business UI outside the supplied reference image is rendered by the application"));
    }

    @Test
    void aboutImagePromptPreservesReferenceComposition()
    {
        ComponentAiProfileService profiles = mock(ComponentAiProfileService.class);
        ComponentAiProfile profile = new ComponentAiProfile(); profile.setForbiddenElementsJson("[\"price\",\"button\"]");
        when(profiles.require("aboutImage")).thenReturn(profile);
        when(profiles.values(profile.getForbiddenElementsJson())).thenReturn(java.util.Arrays.asList("price", "button"));
        DecoratorPromptBuilder builder = new DecoratorPromptBuilder(); ReflectionTestUtils.setField(builder, "profileService", profiles);
        BackgroundSlotSpec slot = new BackgroundSlotSpec(); slot.setComponentKey("aboutImage"); slot.setOutputWidth(800); slot.setOutputHeight(1421); slot.setRenderMode("CONTAIN");
        DecoratorAiTask task = new DecoratorAiTask(); task.setStylePreset("BOTANICAL"); task.setPlacementPreset("CENTER"); task.setTextMode("NO_TEXT"); task.setPromptText("古风");

        String prompt = builder.background(task, slot);

        assertTrue(prompt.contains("full-height story poster composition"));
        assertTrue(prompt.contains("existing cafe scene and main subject(s)"));
        assertTrue(prompt.contains("800x1421 pixels exactly"));
        assertTrue(prompt.contains("do not crop, reframe, stretch, rotate, move, remove, or redesign"));
        assertTrue(prompt.contains("preserve every original word, character, number, line, text block, and reading order"));
        assertTrue(prompt.contains("large, crisp, high-contrast"));
        assertFalse(prompt.contains("Do not copy or invent the reference image's characters"));
    }

    @Test
    void sketchPromptTreatsReferenceAsWireframe()
    {
        ComponentAiProfileService profiles = mock(ComponentAiProfileService.class);
        ComponentAiProfile profile = new ComponentAiProfile(); profile.setForbiddenElementsJson("[\"price\",\"button\"]");
        when(profiles.require("homeBanner")).thenReturn(profile);
        when(profiles.values(profile.getForbiddenElementsJson())).thenReturn(java.util.Arrays.asList("price", "button"));
        DecoratorPromptBuilder builder = new DecoratorPromptBuilder(); ReflectionTestUtils.setField(builder, "profileService", profiles);
        BackgroundSlotSpec slot = new BackgroundSlotSpec(); slot.setComponentKey("homeBanner"); slot.setOutputWidth(1500); slot.setOutputHeight(720); slot.setRenderMode("COVER");
        DecoratorAiTask task = new DecoratorAiTask(); task.setStylePreset("WARM_HANDMADE"); task.setTextMode("NO_TEXT");
        task.setPromptText("soft morning light");
        task.setVisualIntentJson("{\"compositionPreset\":\"LEFT_COPY_RIGHT_SUBJECT\"}");

        String prompt = builder.background(task, slot);

        assertTrue(prompt.contains("wireframe layout guide"));
        assertTrue(prompt.contains("Do not render guide boxes"));
        assertTrue(prompt.contains("text-safe area visually quiet"));
        assertTrue(prompt.contains("LEFT_COPY_RIGHT_SUBJECT"));
        assertFalse(prompt.contains("style transfer for one UI component"));
    }
}
