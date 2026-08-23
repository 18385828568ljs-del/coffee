package com.ruoyi.project.coffee.decorator.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.project.coffee.decorator.context.TenantContext;
import com.ruoyi.project.coffee.decorator.context.TenantContextService;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorThemeMapper;
import com.ruoyi.project.coffee.decorator.theme.domain.DecoratorTheme;
import com.ruoyi.project.coffee.decorator.theme.domain.StoreThemeSummary;
import com.ruoyi.project.coffee.decorator.theme.domain.SystemThemeTemplate;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemeDraft;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemeVersion;

class DecoratorThemeServiceTest
{
    @Mock
    private DecoratorThemeMapper themeMapper;

    @Mock
    private TenantContextService contextService;

    private DecoratorThemeService service;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        ObjectMapper objectMapper = new ObjectMapper();
        service = new DecoratorThemeService();
        ReflectionTestUtils.setField(service, "themeMapper", themeMapper);
        ReflectionTestUtils.setField(service, "contextService", contextService);
        ReflectionTestUtils.setField(service, "configValidator", new ThemeConfigValidator(objectMapper));
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(service, "skinConfigDefaults", new SkinConfigDefaults(objectMapper));
        when(themeMapper.selectTheme(7L, 11L)).thenReturn(masterTheme());
    }

    @Test
    void saveDraftRejectsStaleRevision()
    {
        when(themeMapper.updateDraft(7L, 11L, 3, validConfig(), "1.0.0", 9L, null)).thenReturn(0);

        assertThrows(ThemeConflictException.class,
                () -> service.saveDraft(context(), 11L, 3, validConfig()));
    }

    @Test
    void draftFallsBackToPublishedConfigWithoutWriting()
    {
        ThemeVersion published = version(41L, 2);
        published.setSchemaVersion("1");
        published.setConfigJson(validSkinConfig());
        when(themeMapper.selectDraft(7L, 11L)).thenReturn(null);
        when(themeMapper.selectLatestVersion(7L, 11L)).thenReturn(published);

        ThemeDraft draft = service.draft(context(), 11L);

        assertEquals(Integer.valueOf(0), draft.getRevision());
        assertEquals(Long.valueOf(41L), draft.getBasedOnVersionId());
        verify(themeMapper, never()).insertDraftIfAbsent(any(ThemeDraft.class));
    }

    @Test
    void draftsPassesRegularListToMapperForOgnlCollectionExpressions()
    {
        TenantContext contextWithStore = new TenantContext(9L, 8L, 7L, "OWNER", "LIMITED",
                Collections.singleton(22L));
        when(themeMapper.selectDraftThemes(eq(7L), any())).thenReturn(Collections.<DecoratorTheme>emptyList());

        service.drafts(contextWithStore);

        ArgumentCaptor<List<Long>> storeIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(themeMapper).selectDraftThemes(eq(7L), storeIdsCaptor.capture());
        assertEquals(Arrays.asList(22L), storeIdsCaptor.getValue());
        assertTrue(storeIdsCaptor.getValue().getClass().equals(java.util.ArrayList.class));
    }

    @Test
    void publishReturnsExistingVersionForRepeatedIdempotencyKey()
    {
        ThemeVersion existing = version(41L, 2);
        when(themeMapper.selectVersionByIdempotencyKey(7L, 11L, "same-request")).thenReturn(existing);

        Map<String, Object> result = service.publish(context(), 11L, 4, " same-request ", "ignored");

        assertEquals("41", result.get("versionId"));
        assertEquals(Boolean.TRUE, result.get("idempotent"));
        verify(themeMapper, never()).selectDraftForUpdate(7L, 11L);
    }

    @Test
    void publishCreatesVersionAndUpdatesFollowingStores()
    {
        ThemeDraft draft = new ThemeDraft();
        draft.setRevision(4);
        draft.setConfigJson(validConfig());
        when(themeMapper.selectDraftForUpdate(7L, 11L)).thenReturn(draft);
        when(themeMapper.selectNextVersionNo(7L, 11L)).thenReturn(1);
        when(themeMapper.bindFollowingStores(7L, 11L, 51L, 9L)).thenReturn(3);
        when(themeMapper.insertVersion(any(ThemeVersion.class))).thenAnswer(invocation -> {
            invocation.<ThemeVersion>getArgument(0).setId(51L);
            return 1;
        });

        Map<String, Object> result = service.publish(context(), 11L, 4, "publish-1", " 首次发布 ");

        assertEquals("51", result.get("versionId"));
        assertEquals(1, result.get("versionNo"));
        assertEquals(3, result.get("affectedStores"));
        assertEquals(Boolean.FALSE, result.get("idempotent"));
        assertTrue(String.valueOf(result.get("configHash")).matches("[0-9a-f]{64}"));
    }

    @Test
    void createThemeCreatesNamedDraftWithoutChangingActiveBinding()
    {
        ThemeVersion current = version(41L, 3);
        current.setSchemaVersion("1");
        current.setConfigJson(validSkinConfig());
        when(themeMapper.selectActiveTheme(7L, "MERCHANT", null)).thenReturn(masterTheme());
        when(themeMapper.selectLatestVersion(7L, 11L)).thenReturn(current);
        when(themeMapper.insertTheme(any(DecoratorTheme.class))).thenAnswer(invocation -> {
            invocation.<DecoratorTheme>getArgument(0).setId(90L);
            return 1;
        });

        DecoratorTheme created = service.createTheme(context(), "MERCHANT", 7L,
                "圣诞限定", "ACTIVE_VERSION", null, null);

        assertEquals(Long.valueOf(90L), created.getId());
        assertEquals("圣诞限定", created.getName());
        assertEquals("MERCHANT", created.getScopeType());
        assertEquals(Long.valueOf(11L), created.getClonedFromThemeId());
        assertEquals(Long.valueOf(41L), created.getClonedFromVersionId());
        ArgumentCaptor<ThemeDraft> draftCaptor = ArgumentCaptor.forClass(ThemeDraft.class);
        verify(themeMapper).insertDraft(draftCaptor.capture());
        assertEquals(Long.valueOf(41L), draftCaptor.getValue().getBasedOnVersionId());
        verify(themeMapper, never()).bindFollowingStores(any(Long.class), any(Long.class),
                any(Long.class), any(Long.class));
    }

    @Test
    void createThemeFromSystemTemplateCreatesEditableDraft()
    {
        SystemThemeTemplate template = new SystemThemeTemplate();
        template.setId(3L);
        template.setName("深林鼠尾草");
        template.setConfigJson(validConfig());
        when(themeMapper.selectTemplate(3L)).thenReturn(template);
        when(themeMapper.insertTheme(any(DecoratorTheme.class))).thenAnswer(invocation -> {
            invocation.<DecoratorTheme>getArgument(0).setId(91L);
            return 1;
        });

        DecoratorTheme created = service.createTheme(context(), "MERCHANT", 7L,
                "主题 1", "TEMPLATE", null, 3L);

        assertEquals(Long.valueOf(91L), created.getId());
        assertEquals(Long.valueOf(3L), created.getSourceTemplateId());
        ArgumentCaptor<ThemeDraft> draftCaptor = ArgumentCaptor.forClass(ThemeDraft.class);
        verify(themeMapper).insertDraft(draftCaptor.capture());
        assertEquals(Integer.valueOf(1), draftCaptor.getValue().getRevision());
        assertEquals(validConfig(), draftCaptor.getValue().getConfigJson());
    }

    @Test
    void createIndependentThemeFreezesCurrentPublishedSnapshotUntilFirstPublish()
    {
        StoreThemeSummary following = store("FOLLOW_MERCHANT", 11L, 41L);
        ThemeVersion current = version(41L, 4);
        current.setSchemaVersion("1");
        current.setConfigJson(validSkinConfig());
        when(themeMapper.selectStore(7L, 22L)).thenReturn(following);
        when(themeMapper.selectVersion(7L, 11L, 41L)).thenReturn(current);
        when(themeMapper.insertTheme(any(DecoratorTheme.class))).thenAnswer(invocation -> {
            invocation.<DecoratorTheme>getArgument(0).setId(90L);
            return 1;
        });

        DecoratorTheme created = service.createIndependentTheme(context(), 22L);

        assertEquals(Long.valueOf(90L), created.getId());
        assertEquals("STORE", created.getScopeType());
        assertEquals(Long.valueOf(22L), created.getOwnerStoreId());
        ArgumentCaptor<ThemeDraft> draftCaptor = ArgumentCaptor.forClass(ThemeDraft.class);
        verify(themeMapper).insertDraft(draftCaptor.capture());
        assertEquals(Long.valueOf(41L), draftCaptor.getValue().getBasedOnVersionId());
        assertEquals(validSkinConfig(), draftCaptor.getValue().getConfigJson());
        verify(themeMapper).bindIndependentStore(7L, 22L, 11L, 41L, 9L);
    }

    @Test
    void createIndependentThemeIsIdempotentBeforeFirstStorePublish()
    {
        DecoratorTheme existing = storeTheme(90L, 22L);
        when(themeMapper.selectStore(7L, 22L)).thenReturn(store("INDEPENDENT", 11L, 41L));
        when(themeMapper.selectThemes(7L, "STORE", 22L)).thenReturn(Collections.singletonList(existing));

        DecoratorTheme result = service.createIndependentTheme(context(), 22L);

        assertEquals(Long.valueOf(90L), result.getId());
        verify(themeMapper, never()).insertTheme(any(DecoratorTheme.class));
        verify(themeMapper, never()).bindIndependentStore(any(Long.class), any(Long.class),
                any(Long.class), any(Long.class), any(Long.class));
    }

    @Test
    void followMasterBindsCurrentMerchantVersionAndKeepsStoreThemes()
    {
        StoreThemeSummary independent = store("INDEPENDENT", 90L, 61L);
        StoreThemeSummary following = store("FOLLOW_MERCHANT", 11L, 41L);
        ThemeVersion current = version(41L, 4);
        when(themeMapper.selectStore(7L, 22L)).thenReturn(independent, following);
        when(themeMapper.selectActiveTheme(7L, "MERCHANT", 7L)).thenReturn(masterTheme());
        when(themeMapper.selectLatestVersion(7L, 11L)).thenReturn(current);

        StoreThemeSummary result = service.followMaster(context(), 22L);

        assertEquals("FOLLOW_MERCHANT", result.getBindingMode());
        assertEquals(Long.valueOf(11L), result.getThemeId());
        assertEquals(Long.valueOf(41L), result.getPublishedVersionId());
        verify(themeMapper).bindStoreToMaster(7L, 22L, 11L, 41L, 9L);
        verify(themeMapper, never()).insertTheme(any(DecoratorTheme.class));
    }

    private static TenantContext context()
    {
        return new TenantContext(9L, 8L, 7L, "OWNER", "ALL", Collections.<Long>emptySet());
    }

    private static DecoratorTheme masterTheme()
    {
        DecoratorTheme theme = new DecoratorTheme();
        theme.setId(11L);
        theme.setMerchantId(7L);
        theme.setScopeType("MERCHANT");
        theme.setStatus("ACTIVE");
        return theme;
    }

    private static DecoratorTheme storeTheme(Long themeId, Long storeId)
    {
        DecoratorTheme theme = new DecoratorTheme();
        theme.setId(themeId);
        theme.setMerchantId(7L);
        theme.setScopeType("STORE");
        theme.setOwnerStoreId(storeId);
        theme.setStatus("ACTIVE");
        return theme;
    }

    private static StoreThemeSummary store(String mode, Long themeId, Long versionId)
    {
        StoreThemeSummary store = new StoreThemeSummary();
        store.setStoreId(22L);
        store.setStoreName("咖啡商城滨江店");
        store.setStoreStatus("ACTIVE");
        store.setBindingMode(mode);
        store.setThemeId(themeId);
        store.setPublishedVersionId(versionId);
        return store;
    }

    private static ThemeVersion version(Long id, int number)
    {
        ThemeVersion version = new ThemeVersion();
        version.setId(id);
        version.setVersionNo(number);
        version.setConfigHash("hash");
        return version;
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

    private static String validSkinConfig()
    {
        return "{\"schemaVersion\":1,\"themeVersion\":1,\"page\":{\"backgroundColor\":\"#FFFFFF\",\"textColor\":\"#332C28\",\"secondaryTextColor\":\"#8A7D74\"},\"slots\":{\"heroBanner\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#6F4E37\",\"fit\":\"cover\"},\"orderCard\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#FFFFFF\",\"iconColor\":\"#745848\",\"textColor\":\"#302720\",\"secondaryTextColor\":\"#C28B62\",\"radius\":20,\"shadow\":\"light\"},\"shopCard\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#FFFFFF\",\"iconColor\":\"#332C28\",\"textColor\":\"#302720\",\"secondaryTextColor\":\"#8A7D74\",\"radius\":20,\"shadow\":\"light\"},\"welcomeBanner\":{\"backgroundType\":\"color\",\"backgroundColor\":\"#E8D4C3\",\"fit\":\"cover\",\"radius\":8},\"aboutSection\":{\"backgroundColor\":\"#FFFFFF\",\"titleColor\":\"#332C28\",\"image\":\"\"},\"tabBar\":{\"backgroundColor\":\"#FFFFFF\",\"textColor\":\"#777777\",\"activeTextColor\":\"#44352C\",\"iconColor\":\"#999999\",\"activeIconColor\":\"#44352C\",\"activeBackgroundColor\":\"#F3E4D6\"}}}";
    }
}
