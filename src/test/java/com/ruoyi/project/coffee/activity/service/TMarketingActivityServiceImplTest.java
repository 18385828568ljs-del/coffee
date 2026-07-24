package com.ruoyi.project.coffee.activity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.activity.domain.TMarketingActivity;
import com.ruoyi.project.coffee.activity.domain.TMarketingActivityScope;
import com.ruoyi.project.coffee.activity.mapper.TMarketingActivityMapper;
import com.ruoyi.project.coffee.activity.service.impl.TMarketingActivityServiceImpl;

@SuppressWarnings("unchecked")
class TMarketingActivityServiceImplTest
{
    @Mock
    private TMarketingActivityMapper marketingActivityMapper;

    private TMarketingActivityServiceImpl marketingActivityService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        marketingActivityService = new TMarketingActivityServiceImpl();
        ReflectionTestUtils.setField(marketingActivityService, "tMarketingActivityMapper", marketingActivityMapper);
    }

    @Test
    void selectByIdShouldPopulateDedupedScopeIdsAndText()
    {
        TMarketingActivity activity = activity(10L, 1);
        when(marketingActivityMapper.selectTMarketingActivityByActivityId(10L)).thenReturn(activity);
        when(marketingActivityMapper.selectTMarketingActivityScopesByActivityIds(Collections.singletonList(10L)))
                .thenReturn(Arrays.asList(scope(10L, 100L, 1), scope(10L, 100L, 1), scope(10L, 200L, 1),
                        scope(10L, null, 1)));

        TMarketingActivity result = marketingActivityService.selectTMarketingActivityByActivityId(10L);

        assertEquals(Arrays.asList(100L, 200L), result.getScopeIds());
        assertEquals("100,200", result.getScopeIdsText());
    }

    @Test
    void selectListShouldPopulateScopesForEachActivityAndIgnoreNullActivities()
    {
        TMarketingActivity first = activity(10L, 1);
        TMarketingActivity second = activity(20L, 2);
        when(marketingActivityMapper.selectTMarketingActivityList(any(TMarketingActivity.class)))
                .thenReturn(Arrays.asList(first, null, second));
        when(marketingActivityMapper.selectTMarketingActivityScopesByActivityIds(Arrays.asList(10L, 20L)))
                .thenReturn(Arrays.asList(scope(10L, 101L, 1), scope(20L, 201L, 2), scope(20L, 202L, 2)));

        List<TMarketingActivity> result = marketingActivityService.selectTMarketingActivityList(new TMarketingActivity());

        assertEquals(Arrays.asList(101L), result.get(0).getScopeIds());
        assertEquals("101", result.get(0).getScopeIdsText());
        assertEquals(Arrays.asList(201L, 202L), result.get(2).getScopeIds());
        assertEquals("201,202", result.get(2).getScopeIdsText());
    }

    @Test
    void insertShouldSyncDedupedScopeIdsFromListAfterBaseActivityCreated()
    {
        TMarketingActivity activity = activity(10L, 1);
        activity.setScopeIds(Arrays.asList(3L, 2L, 3L, 0L, null));
        when(marketingActivityMapper.insertTMarketingActivity(activity)).thenReturn(1);

        int rows = marketingActivityService.insertTMarketingActivity(activity);

        assertEquals(1, rows);
        assertNotNull(activity.getCreateTime());
        verify(marketingActivityMapper).deleteTMarketingActivityScopeByActivityId(10L);
        ArgumentCaptor<List<TMarketingActivityScope>> scopesCaptor = ArgumentCaptor.forClass(List.class);
        verify(marketingActivityMapper).batchInsertTMarketingActivityScopes(scopesCaptor.capture());
        List<TMarketingActivityScope> scopes = scopesCaptor.getValue();
        assertEquals(2, scopes.size());
        assertScope(scopes.get(0), 10L, 1, 3L);
        assertScope(scopes.get(1), 10L, 1, 2L);
        assertEquals(Arrays.asList(3L, 2L), activity.getScopeIds());
        assertEquals("3,2", activity.getScopeIdsText());
    }

    @Test
    void updateShouldParseScopeIdsTextWhenScopeListIsEmpty()
    {
        TMarketingActivity activity = activity(10L, 2);
        activity.setScopeIds(Collections.<Long>emptyList());
        activity.setScopeIdsText("8, 9, 8, abc, -1");
        when(marketingActivityMapper.updateTMarketingActivity(activity)).thenReturn(1);

        int rows = marketingActivityService.updateTMarketingActivity(activity);

        assertEquals(1, rows);
        assertNotNull(activity.getUpdateTime());
        verify(marketingActivityMapper).deleteTMarketingActivityScopeByActivityId(10L);
        ArgumentCaptor<List<TMarketingActivityScope>> scopesCaptor = ArgumentCaptor.forClass(List.class);
        verify(marketingActivityMapper).batchInsertTMarketingActivityScopes(scopesCaptor.capture());
        List<TMarketingActivityScope> scopes = scopesCaptor.getValue();
        assertEquals(2, scopes.size());
        assertScope(scopes.get(0), 10L, 2, 8L);
        assertScope(scopes.get(1), 10L, 2, 9L);
        assertEquals(Arrays.asList(8L, 9L), activity.getScopeIds());
        assertEquals("8,9", activity.getScopeIdsText());
    }

    @Test
    void insertShouldClearScopeWhenActivityIsGlobalScope()
    {
        TMarketingActivity activity = activity(10L, 0);
        activity.setScopeIds(Arrays.asList(1L, 2L));
        activity.setScopeIdsText("1,2");
        when(marketingActivityMapper.insertTMarketingActivity(activity)).thenReturn(1);

        int rows = marketingActivityService.insertTMarketingActivity(activity);

        assertEquals(1, rows);
        verify(marketingActivityMapper).deleteTMarketingActivityScopeByActivityId(10L);
        verify(marketingActivityMapper, never()).batchInsertTMarketingActivityScopes(any());
        assertTrue(activity.getScopeIds().isEmpty());
        assertEquals("", activity.getScopeIdsText());
    }

    @Test
    void insertShouldNotSyncScopesWhenActivityIdMissing()
    {
        TMarketingActivity activity = activity(null, 1);
        activity.setScopeIds(Arrays.asList(1L, 2L));
        when(marketingActivityMapper.insertTMarketingActivity(activity)).thenReturn(1);

        int rows = marketingActivityService.insertTMarketingActivity(activity);

        assertEquals(1, rows);
        verify(marketingActivityMapper, never()).deleteTMarketingActivityScopeByActivityId(any());
        verify(marketingActivityMapper, never()).batchInsertTMarketingActivityScopes(any());
    }

    @Test
    void deleteByIdShouldDeleteScopesBeforeActivity()
    {
        when(marketingActivityMapper.deleteTMarketingActivityByActivityId(10L)).thenReturn(1);

        int rows = marketingActivityService.deleteTMarketingActivityByActivityId(10L);

        assertEquals(1, rows);
        verify(marketingActivityMapper).deleteTMarketingActivityScopeByActivityId(10L);
        verify(marketingActivityMapper).deleteTMarketingActivityByActivityId(10L);
    }

    @Test
    void deleteByIdsShouldSplitIdsAndDeleteScopesBeforeActivities()
    {
        when(marketingActivityMapper.deleteTMarketingActivityByActivityIds(any(String[].class))).thenReturn(2);

        int rows = marketingActivityService.deleteTMarketingActivityByActivityIds("10,20");

        assertEquals(2, rows);
        ArgumentCaptor<String[]> idsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(marketingActivityMapper).deleteTMarketingActivityScopeByActivityIds(idsCaptor.capture());
        assertEquals(Arrays.asList("10", "20"), Arrays.asList(idsCaptor.getValue()));
        verify(marketingActivityMapper).deleteTMarketingActivityByActivityIds(eq(idsCaptor.getValue()));
    }

    private static TMarketingActivity activity(Long activityId, Integer scopeType)
    {
        TMarketingActivity activity = new TMarketingActivity();
        activity.setActivityId(activityId);
        activity.setScopeType(scopeType);
        return activity;
    }

    private static TMarketingActivityScope scope(Long activityId, Long scopeTargetId, Integer scopeType)
    {
        TMarketingActivityScope scope = new TMarketingActivityScope();
        scope.setActivityId(activityId);
        scope.setScopeType(scopeType);
        scope.setScopeTargetId(scopeTargetId);
        return scope;
    }

    private static void assertScope(TMarketingActivityScope scope, Long activityId, Integer scopeType,
            Long scopeTargetId)
    {
        assertEquals(activityId, scope.getActivityId());
        assertEquals(scopeType, scope.getScopeType());
        assertEquals(scopeTargetId, scope.getScopeTargetId());
    }
}
