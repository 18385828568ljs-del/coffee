package com.ruoyi.project.coffee.behavior.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruoyi.project.coffee.behavior.domain.UserBehaviorEvent;
import com.ruoyi.project.coffee.behavior.mapper.UserBehaviorEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

class UserBehaviorEventServiceTest
{
    @Mock
    private UserBehaviorEventMapper userBehaviorEventMapper;

    private UserBehaviorEventService service;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new UserBehaviorEventService();
        ReflectionTestUtils.setField(service, "userBehaviorEventMapper", userBehaviorEventMapper);
    }

    @Test
    void recordProductViewWritesDailyDedupKeyAndSource()
    {
        when(userBehaviorEventMapper.insertUserBehaviorEvent(any(UserBehaviorEvent.class))).thenReturn(1);

        assertTrue(service.recordProductView(7L, UserBehaviorEventService.SCENE_MALL, 100L, 3L,
            UserBehaviorEventService.SOURCE_PERSONALIZED_LIST));

        ArgumentCaptor<UserBehaviorEvent> captor = ArgumentCaptor.forClass(UserBehaviorEvent.class);
        verify(userBehaviorEventMapper).insertUserBehaviorEvent(captor.capture());
        UserBehaviorEvent event = captor.getValue();
        assertTrue(event.getEventTime() != null);
        assertEquals(7L, event.getUserId());
        assertEquals(UserBehaviorEventService.EVENT_PRODUCT_VIEW, event.getEventType());
        assertEquals(UserBehaviorEventService.SCENE_MALL, event.getScene());
        assertEquals(100L, event.getProductId());
        assertEquals(3L, event.getCategoryId());
        assertEquals(UserBehaviorEventService.SOURCE_PERSONALIZED_LIST, event.getSource());
        assertTrue(event.getDedupKey().startsWith("PRODUCT_VIEW:MALL:7:100:"));
    }

    @Test
    void recordFirstCartAddUsesCartRowAsDedupKey()
    {
        when(userBehaviorEventMapper.insertUserBehaviorEvent(any(UserBehaviorEvent.class))).thenReturn(1);

        String specJson = "[{\"specName\":\"温度\",\"optionNames\":[\"冰\"]}]";
        assertTrue(service.recordFirstCartAdd(7L, UserBehaviorEventService.SCENE_SCAN, 100L, 3L, 55L,
            UserBehaviorEventService.SOURCE_CATEGORY, specJson));

        ArgumentCaptor<UserBehaviorEvent> captor = ArgumentCaptor.forClass(UserBehaviorEvent.class);
        verify(userBehaviorEventMapper).insertUserBehaviorEvent(captor.capture());
        assertTrue(captor.getValue().getDedupKey().contains(":55"));
        assertEquals(specJson, captor.getValue().getSpecJson());
    }

    @Test
    void recordCartRemoveUsesCartRowAsDedupKey()
    {
        when(userBehaviorEventMapper.insertUserBehaviorEvent(any(UserBehaviorEvent.class))).thenReturn(1);

        assertTrue(service.recordCartRemove(7L, UserBehaviorEventService.SCENE_SCAN, 100L, 3L, 55L));

        ArgumentCaptor<UserBehaviorEvent> captor = ArgumentCaptor.forClass(UserBehaviorEvent.class);
        verify(userBehaviorEventMapper).insertUserBehaviorEvent(captor.capture());
        assertEquals("CART_REMOVE:SCAN:55", captor.getValue().getDedupKey());
    }

    @Test
    void duplicateEvidenceDoesNotEscapeTheCollectionPath()
    {
        when(userBehaviorEventMapper.insertUserBehaviorEvent(any(UserBehaviorEvent.class)))
            .thenThrow(new DuplicateKeyException("duplicate"));

        assertFalse(service.recordFirstCartAdd(7L, UserBehaviorEventService.SCENE_MALL, 100L, null, 55L));
    }

    @Test
    void mapperFailureDoesNotEscapeTheCollectionPath()
    {
        when(userBehaviorEventMapper.insertUserBehaviorEvent(any(UserBehaviorEvent.class)))
            .thenThrow(new IllegalStateException("storage unavailable"));

        assertFalse(service.recordProductView(7L, UserBehaviorEventService.SCENE_MALL, 100L, null));
    }

    @Test
    void invalidEvidenceDoesNotWrite()
    {
        assertFalse(service.recordProductView(null, UserBehaviorEventService.SCENE_MALL, 100L, null));

        verify(userBehaviorEventMapper, never()).insertUserBehaviorEvent(any(UserBehaviorEvent.class));
    }
}
