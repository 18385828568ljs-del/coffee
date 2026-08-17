package com.ruoyi.project.coffee.decorator.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.context.TenantContext;
import com.ruoyi.project.coffee.decorator.context.TenantContextService;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorThemeMapper;
import com.ruoyi.project.coffee.decorator.theme.domain.DecoratorTheme;
import com.ruoyi.project.coffee.decorator.theme.domain.PreviewTheme;
import com.ruoyi.project.coffee.decorator.theme.domain.StoreThemeSummary;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemeDraft;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemePreviewSession;
import com.ruoyi.project.coffee.scanOrder.wx.WxaCodeService;

class DecoratorPreviewServiceTest
{
    @Mock
    private DecoratorThemeMapper themeMapper;

    @Mock
    private TenantContextService contextService;

    @Mock
    private WxaCodeService wxaCodeService;

    private DecoratorPreviewService service;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new DecoratorPreviewService();
        ReflectionTestUtils.setField(service, "themeMapper", themeMapper);
        ReflectionTestUtils.setField(service, "contextService", contextService);
        ReflectionTestUtils.setField(service, "configValidator", new ThemeConfigValidator(new ObjectMapper()));
        ReflectionTestUtils.setField(service, "wxaCodeService", wxaCodeService);
    }

    @Test
    void createShouldPersistHashAndSavedDraftSnapshot()
    {
        TenantContext context = context();
        DecoratorTheme theme = new DecoratorTheme();
        theme.setId(11L);
        theme.setMerchantId(7L);
        theme.setScopeType("MERCHANT");
        StoreThemeSummary store = new StoreThemeSummary();
        store.setStoreId(21L);
        store.setStoreCode("STORE_21");
        store.setStoreStatus("ACTIVE");
        ThemeDraft draft = draft(31L, 4);
        when(themeMapper.selectTheme(7L, 11L)).thenReturn(theme);
        when(themeMapper.selectStore(7L, 21L)).thenReturn(store);
        when(themeMapper.selectDraft(7L, 11L)).thenReturn(draft);
        when(themeMapper.insertPreviewSession(any(ThemePreviewSession.class))).thenAnswer(invocation -> {
            invocation.<ThemePreviewSession>getArgument(0).setId(41L);
            return 1;
        });
        when(wxaCodeService.generateWxaCode(anyString(), anyString(), anyString()))
                .thenReturn("/profile/scanQrcode/decorator_preview_41.jpg");

        Map<String, Object> result = service.create(context, 11L, 21L, 4);

        ArgumentCaptor<ThemePreviewSession> captor = ArgumentCaptor.forClass(ThemePreviewSession.class);
        verify(themeMapper).insertPreviewSession(captor.capture());
        ThemePreviewSession saved = captor.getValue();
        assertEquals(draft.getConfigJson(), saved.getConfigJson());
        assertEquals(Integer.valueOf(4), saved.getDraftRevision());
        assertEquals(64, saved.getTokenHash().length());
        assertNotEquals(result.get("previewToken"), saved.getTokenHash());
        assertEquals(22, String.valueOf(result.get("previewToken")).length());
        assertEquals("/profile/scanQrcode/decorator_preview_41.jpg", result.get("qrCodeUrl"));
        assertTrue(String.valueOf(result.get("previewPath")).contains("previewToken="));
    }

    @Test
    void createShouldRejectStaleRevision()
    {
        DecoratorTheme theme = new DecoratorTheme();
        theme.setScopeType("MERCHANT");
        StoreThemeSummary store = new StoreThemeSummary();
        store.setStoreStatus("ACTIVE");
        when(themeMapper.selectTheme(7L, 11L)).thenReturn(theme);
        when(themeMapper.selectStore(7L, 21L)).thenReturn(store);
        when(themeMapper.selectDraft(7L, 11L)).thenReturn(draft(31L, 5));

        assertThrows(ThemeConflictException.class, () -> service.create(context(), 11L, 21L, 4));
    }

    @Test
    void createShouldKeepPreviewPathWhenWxaCodeIsUnavailable()
    {
        DecoratorTheme theme = new DecoratorTheme();
        theme.setScopeType("MERCHANT");
        StoreThemeSummary store = new StoreThemeSummary();
        store.setStoreCode("STORE_21");
        store.setStoreStatus("ACTIVE");
        when(themeMapper.selectTheme(7L, 11L)).thenReturn(theme);
        when(themeMapper.selectStore(7L, 21L)).thenReturn(store);
        when(themeMapper.selectDraft(7L, 11L)).thenReturn(draft(31L, 4));
        when(themeMapper.insertPreviewSession(any(ThemePreviewSession.class))).thenAnswer(invocation -> {
            invocation.<ThemePreviewSession>getArgument(0).setId(42L);
            return 1;
        });
        when(wxaCodeService.generateWxaCode(anyString(), anyString(), anyString()))
                .thenThrow(new ServiceException("微信配置不可用"));

        Map<String, Object> result = service.create(context(), 11L, 21L, 4);

        assertTrue(String.valueOf(result.get("previewPath")).contains("previewToken="));
        assertEquals("微信配置不可用", result.get("qrUnavailableReason"));
    }

    @Test
    void resolveShouldRejectInvalidOrMissingToken()
    {
        assertThrows(ServiceException.class, () -> service.resolve("guess"));
        when(themeMapper.selectPreviewTheme(anyString())).thenReturn(null);
        assertThrows(ServiceException.class,
                () -> service.resolve("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
    }

    @Test
    void resolveShouldAcceptSceneLengthToken()
    {
        PreviewTheme preview = new PreviewTheme();
        preview.setConfigJson(validConfig());
        when(themeMapper.selectPreviewTheme(anyString())).thenReturn(preview);

        assertEquals(preview, service.resolve("AAAAAAAAAAAAAAAAAAAAAA"));
    }

    @Test
    void resolveShouldReturnValidatedSnapshot()
    {
        PreviewTheme preview = new PreviewTheme();
        preview.setConfigJson(validConfig());
        when(themeMapper.selectPreviewTheme(anyString())).thenReturn(preview);

        assertEquals(preview, service.resolve("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
    }

    private static TenantContext context()
    {
        return new TenantContext(9L, 8L, 7L, "DESIGNER", "SELECTED", Collections.singleton(21L));
    }

    private static ThemeDraft draft(Long id, int revision)
    {
        ThemeDraft draft = new ThemeDraft();
        draft.setId(id);
        draft.setMerchantId(7L);
        draft.setThemeId(11L);
        draft.setRevision(revision);
        draft.setSchemaVersion("1.0.0");
        draft.setConfigJson(validConfig());
        return draft;
    }

    private static String validConfig()
    {
        return "{\"schemaVersion\":\"1.0.0\","
                + "\"tokens\":{\"colors\":{\"primary\":\"#6F4E37\"},"
                + "\"radius\":{\"card\":12},\"shadow\":{\"card\":\"soft\"}},"
                + "\"brand\":{},\"components\":{"
                + "\"shopHeader\":{\"variant\":\"centered\"},"
                + "\"activityBanner\":{\"visible\":true,\"variant\":\"single\"},"
                + "\"categoryNav\":{\"variant\":\"icon-grid\"},"
                + "\"productCard\":{\"variant\":\"vertical\"},"
                + "\"tabBar\":{\"variant\":\"standard\"},"
                + "\"profileHeader\":{\"variant\":\"brand\"}}}";
    }
}
