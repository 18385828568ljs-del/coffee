package com.ruoyi.project.coffee.decorator.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.ai.domain.DecoratorAiTask;
import com.ruoyi.project.coffee.decorator.ai.domain.DecoratorAiResult;
import com.ruoyi.project.coffee.decorator.api.DecoratorAiBatchApplyRequest;
import com.ruoyi.project.coffee.decorator.api.DecoratorAiApplyRequest;
import com.ruoyi.project.coffee.decorator.api.DecoratorAiTaskRequest;
import com.ruoyi.project.coffee.decorator.asset.domain.DecoratorAsset;
import com.ruoyi.project.coffee.decorator.context.TenantContext;
import com.ruoyi.project.coffee.decorator.context.TenantContextService;
import com.ruoyi.project.coffee.decorator.asset.BackgroundSlotService;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorAiMapper;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorAssetMapper;
import com.ruoyi.project.coffee.decorator.theme.DecoratorThemeService;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemeDraft;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class DecoratorAiServiceTest
{
    private DecoratorAiService service;
    private TenantContext context;
    private DecoratorAiMapper aiMapper;
    private DecoratorAssetMapper assetMapper;
    private BackgroundSlotService slotService;
    private DecoratorPromptBuilder promptBuilder;
    private DecoratorThemeService themeService;
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp()
    {
        service = new DecoratorAiService();
        ReflectionTestUtils.setField(service, "contextService", mock(TenantContextService.class));
        aiMapper = mock(DecoratorAiMapper.class);
        assetMapper = mock(DecoratorAssetMapper.class);
        slotService = mock(BackgroundSlotService.class);
        promptBuilder = mock(DecoratorPromptBuilder.class);
        themeService = mock(DecoratorThemeService.class);
        executor = mock(ThreadPoolTaskExecutor.class);
        ReflectionTestUtils.setField(service, "aiMapper", aiMapper);
        ReflectionTestUtils.setField(service, "assetMapper", assetMapper);
        ReflectionTestUtils.setField(service, "slotService", slotService);
        ReflectionTestUtils.setField(service, "promptBuilder", promptBuilder);
        ReflectionTestUtils.setField(service, "executor", executor);
        ReflectionTestUtils.setField(service, "themeService", themeService);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        context = new TenantContext(1L, 1L, 1L, "OWNER", "ALL", Collections.<Long>emptySet());
    }

    @Test
    void createsTasksWithImageApiProvider()
    {
        BackgroundSlotSpec slot = new BackgroundSlotSpec();
        slot.setId(10L);
        slot.setComponentKey("homeBanner");
        slot.setSpecVersion(1);
        slot.setAiEnabled(true);
        when(slotService.require("homeBanner")).thenReturn(slot);
        when(promptBuilder.background(any(DecoratorAiTask.class), eq(slot))).thenReturn("generated prompt");

        DecoratorAiTask task = service.createBackground(context, request(null));

        assertEquals("openai-images", task.getProvider());
        verify(aiMapper).insertTask(task);
    }

    @Test
    void storesValidatedHomeBannerVisualIntent() throws Exception
    {
        BackgroundSlotSpec slot = new BackgroundSlotSpec();
        slot.setId(10L); slot.setComponentKey("homeBanner"); slot.setSpecVersion(1); slot.setAiEnabled(true);
        when(slotService.require("homeBanner")).thenReturn(slot);
        when(promptBuilder.background(any(DecoratorAiTask.class), eq(slot))).thenReturn("generated prompt");
        DecoratorAiTaskRequest request = request(null);
        request.setVisualIntent(new ObjectMapper().readTree("{\"schemaVersion\":1,\"slotKey\":\"homeBanner\","
                + "\"compositionPreset\":\"LEFT_COPY_RIGHT_SUBJECT\",\"subjects\":[{\"box\":{\"x\":0.55,\"y\":0.1,\"width\":0.35,\"height\":0.8}}],"
                + "\"textSafeAreas\":[{\"x\":0.05,\"y\":0.15,\"width\":0.4,\"height\":0.65}]}"));
        byte[] pngHeader = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        request.setGuideImageDataUrl("data:image/png;base64," + Base64.getEncoder().encodeToString(pngHeader));

        DecoratorAiTask task = service.createBackground(context, request);

        Assertions.assertTrue(task.getVisualIntentJson().contains("LEFT_COPY_RIGHT_SUBJECT"));
        verify(aiMapper).insertTask(task);
    }

    @Test
    void storesValidatedVisualIntentForAnyEnabledBackgroundComponent() throws Exception
    {
        BackgroundSlotSpec slot = new BackgroundSlotSpec();
        slot.setId(11L); slot.setComponentKey("meOrderCenter"); slot.setSpecVersion(1); slot.setAiEnabled(true);
        when(slotService.require("meOrderCenter")).thenReturn(slot);
        when(promptBuilder.background(any(DecoratorAiTask.class), eq(slot))).thenReturn("generated prompt");
        DecoratorAiTaskRequest request = request(null);
        request.setSlotKey("meOrderCenter");
        request.setVisualIntent(new ObjectMapper().readTree("{\"schemaVersion\":1,\"slotKey\":\"meOrderCenter\","
                + "\"compositionPreset\":\"SCENE_MOOD\",\"subjects\":[],\"scene\":{\"type\":\"CAFE\"},"
                + "\"textSafeAreas\":[{\"x\":0.05,\"y\":0.1,\"width\":0.4,\"height\":0.7}]}"));
        byte[] pngHeader = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        request.setGuideImageDataUrl("data:image/png;base64," + Base64.getEncoder().encodeToString(pngHeader));

        DecoratorAiTask task = service.createBackground(context, request);

        assertEquals("meOrderCenter", task.getSlotKey());
        Assertions.assertTrue(task.getVisualIntentJson().contains("meOrderCenter"));
        verify(aiMapper).insertTask(task);
    }

    @Test
    void rejectsVisualIntentWithoutGuideImage() throws Exception
    {
        BackgroundSlotSpec slot = new BackgroundSlotSpec();
        slot.setId(10L); slot.setComponentKey("homeBanner"); slot.setSpecVersion(1); slot.setAiEnabled(true);
        when(slotService.require("homeBanner")).thenReturn(slot);
        DecoratorAiTaskRequest request = request(null);
        request.setVisualIntent(new ObjectMapper().readTree("{\"schemaVersion\":1,\"slotKey\":\"homeBanner\","
                + "\"compositionPreset\":\"SCENE_MOOD\",\"subjects\":[],\"scene\":{\"type\":\"CAFE\"},"
                + "\"textSafeAreas\":[{\"x\":0.05,\"y\":0.1,\"width\":0.4,\"height\":0.7}]}"));

        ServiceException error = assertThrows(ServiceException.class, () -> service.createBackground(context, request));

        Assertions.assertTrue(error.getMessage().contains("PNG"));
        verify(aiMapper, never()).insertTask(any(DecoratorAiTask.class));
    }

    @Test
    void rejectsArtTextLongerThanTwentyCharacters()
    {
        DecoratorAiTaskRequest request = request("123456789012345678901");
        assertThrows(ServiceException.class, () -> service.createArtText(context, request));
    }

    @Test
    void rejectsUnsupportedArtTextCharacters()
    {
        DecoratorAiTaskRequest request = request("Coffee\uD83D\uDE00");
        assertThrows(ServiceException.class, () -> service.createArtText(context, request));
    }

    @Test
    void batchApplyAppendsSelectedCandidatesToHomeBanner() throws Exception
    {
        DecoratorAiTask task = new DecoratorAiTask();
        task.setId(21L); task.setMerchantId(1L); task.setSlotKey("homeBanner");
        task.setGenerationType("BACKGROUND"); task.setStatus("SUCCEEDED");
        when(aiMapper.selectTask(1L, 21L)).thenReturn(task);
        when(aiMapper.selectResult(eq(1L), eq(101L))).thenReturn(result(101L, 21L));
        when(aiMapper.selectResult(eq(1L), eq(102L))).thenReturn(result(102L, 21L));

        AtomicLong assetIds = new AtomicLong(30L);
        when(assetMapper.insertAsset(any(DecoratorAsset.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, DecoratorAsset.class).setId(assetIds.incrementAndGet());
            return 1;
        });
        when(aiMapper.acceptResult(eq(1L), any(Long.class), any(Long.class))).thenReturn(1);
        when(assetMapper.selectAsset(eq(1L), any(Long.class))).thenAnswer(invocation -> {
            DecoratorAsset asset = new DecoratorAsset(); asset.setId(invocation.getArgument(1)); return asset;
        });

        ThemeDraft draft = new ThemeDraft();
        draft.setRevision(4); draft.setConfigJson("{\"assets\":{\"homeBanner\":[9]}}");
        when(themeService.draft(context, 7L)).thenReturn(draft);
        when(themeService.saveDraft(eq(context), eq(7L), eq(4), any(String.class))).thenReturn(draft);

        DecoratorAiBatchApplyRequest request = new DecoratorAiBatchApplyRequest();
        request.setThemeId(7L); request.setRevision(4); request.setResultIds(Arrays.asList(101L, 102L));
        service.applyBatch(context, request);

        ArgumentCaptor<String> config = ArgumentCaptor.forClass(String.class);
        verify(themeService).saveDraft(eq(context), eq(7L), eq(4), config.capture());
        JsonNode banners = new ObjectMapper().readTree(config.getValue()).path("assets").path("homeBanner");
        Assertions.assertEquals(3, banners.size());
        Assertions.assertEquals(9L, banners.get(0).asLong());
        Assertions.assertEquals(31L, banners.get(1).asLong());
        Assertions.assertEquals(32L, banners.get(2).asLong());
    }

    @Test
    void applySpecPanelBackgroundWritesTheComponentAssetSlot() throws Exception
    {
        DecoratorAiTask task = new DecoratorAiTask();
        task.setId(135L); task.setMerchantId(1L); task.setSlotKey("specPanel");
        task.setSlotSpecVersion(1); task.setGenerationType("BACKGROUND"); task.setStatus("SUCCEEDED");
        DecoratorAiResult result = result(62L, 135L);
        result.setWidth(1372); result.setHeight(1440);
        when(aiMapper.selectResult(1L, 62L)).thenReturn(result);
        when(aiMapper.selectTask(1L, 135L)).thenReturn(task);
        when(assetMapper.insertAsset(any(DecoratorAsset.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, DecoratorAsset.class).setId(99L);
            return 1;
        });
        when(aiMapper.acceptResult(1L, 62L, 99L)).thenReturn(1);
        when(assetMapper.selectAsset(1L, 99L)).thenAnswer(invocation -> {
            DecoratorAsset asset = new DecoratorAsset(); asset.setId(99L); return asset;
        });

        ThemeDraft draft = new ThemeDraft();
        draft.setRevision(4); draft.setConfigJson("{\"assets\":{\"specPanel\":null}}");
        when(themeService.draft(context, 7L)).thenReturn(draft);
        when(themeService.saveDraft(eq(context), eq(7L), eq(4), any(String.class))).thenReturn(draft);

        DecoratorAiApplyRequest request = new DecoratorAiApplyRequest();
        request.setThemeId(7L); request.setRevision(4);
        service.apply(context, 62L, request);

        ArgumentCaptor<String> config = ArgumentCaptor.forClass(String.class);
        verify(themeService).saveDraft(eq(context), eq(7L), eq(4), config.capture());
        JsonNode saved = new ObjectMapper().readTree(config.getValue());
        Assertions.assertEquals(99L, saved.path("assets").path("specPanel").asLong());
        Assertions.assertTrue(saved.path("productImages").isMissingNode());
    }

    @Test
    void batchApplyRejectsMoreThanTenHomeBanners()
    {
        DecoratorAiTask task = new DecoratorAiTask();
        task.setId(21L); task.setMerchantId(1L); task.setSlotKey("homeBanner");
        task.setGenerationType("BACKGROUND"); task.setStatus("SUCCEEDED");
        when(aiMapper.selectTask(1L, 21L)).thenReturn(task);
        DecoratorAiResult first = result(101L, 21L); first.setAcceptedAssetId(31L);
        DecoratorAiResult second = result(102L, 21L); second.setAcceptedAssetId(32L);
        when(aiMapper.selectResult(eq(1L), eq(101L))).thenReturn(first);
        when(aiMapper.selectResult(eq(1L), eq(102L))).thenReturn(second);
        when(assetMapper.selectAsset(eq(1L), any(Long.class))).thenAnswer(invocation -> {
            DecoratorAsset asset = new DecoratorAsset(); asset.setId(invocation.getArgument(1)); return asset;
        });

        ThemeDraft draft = new ThemeDraft();
        draft.setRevision(4);
        draft.setConfigJson("{\"assets\":{\"homeBanner\":[1,2,3,4,5,6,7,8,9]}}");
        when(themeService.draft(context, 7L)).thenReturn(draft);

        DecoratorAiBatchApplyRequest request = new DecoratorAiBatchApplyRequest();
        request.setThemeId(7L); request.setRevision(4); request.setResultIds(Arrays.asList(101L, 102L));

        ServiceException error = assertThrows(ServiceException.class, () -> service.applyBatch(context, request));
        Assertions.assertTrue(error.getMessage().contains("最多 10 张"));
        verify(themeService, never()).saveDraft(any(TenantContext.class), any(Long.class), any(Integer.class), any(String.class));
    }

    private DecoratorAiResult result(Long id, Long taskId)
    {
        DecoratorAiResult result = new DecoratorAiResult();
        result.setId(id); result.setTaskId(taskId); result.setStorageKey("ai/" + id + ".png");
        result.setMimeType("image/png"); result.setWidth(1500); result.setHeight(720);
        return result;
    }

    private DecoratorAiTaskRequest request(String text)
    {
        DecoratorAiTaskRequest request = new DecoratorAiTaskRequest();
        request.setSlotKey("homeBanner");
        request.setTextContent(text);
        request.setPlacementPreset("CENTER");
        request.setSizePreset("MEDIUM");
        request.setStylePreset("MINIMAL");
        return request;
    }
}
