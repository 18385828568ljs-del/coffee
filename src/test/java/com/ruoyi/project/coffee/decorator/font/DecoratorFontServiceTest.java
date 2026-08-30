package com.ruoyi.project.coffee.decorator.font;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.font.domain.FontResource;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorFontMapper;

class DecoratorFontServiceTest
{
    @Mock private DecoratorFontMapper fontMapper;
    private DecoratorFontService service;
    private JsonNode config;

    @BeforeEach
    void setUp() throws Exception
    {
        MockitoAnnotations.openMocks(this);
        service = new DecoratorFontService();
        ReflectionTestUtils.setField(service, "fontMapper", fontMapper);
        config = new ObjectMapper().readTree("{\"typography\":{\"bannerTitle\":{\"fontId\":12},\"bodyText\":{\"fontId\":12}}}");
    }

    @Test
    void validatesDeduplicatedTenantFontReferences()
    {
        FontResource font = new FontResource();
        font.setId(12L);
        when(fontMapper.selectUsableFonts(eq(9L), anyList())).thenReturn(Collections.singletonList(font));
        assertDoesNotThrow(() -> service.validateReferences(9L, config));
    }

    @Test
    void rejectsUnavailableFontReference()
    {
        when(fontMapper.selectUsableFonts(eq(9L), anyList())).thenReturn(Collections.<FontResource>emptyList());
        assertThrows(ServiceException.class, () -> service.validateReferences(9L, config));
    }
}
