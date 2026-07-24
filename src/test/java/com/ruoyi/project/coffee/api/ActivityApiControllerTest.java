package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.coffee.activity.domain.MarketingPreviewResult;
import com.ruoyi.project.coffee.activity.domain.TMarketingActivity;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import com.ruoyi.project.coffee.order.domain.TOrder;

class ActivityApiControllerTest
{
    @Mock
    private MarketingActivityEngine marketingActivityEngine;

    @Captor
    private ArgumentCaptor<List<TOrderItem>> itemsCaptor;

    private ActivityApiController controller;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new ActivityApiController();
        ReflectionTestUtils.setField(controller, "marketingActivityEngine", marketingActivityEngine);
    }

    @AfterEach
    void tearDown()
    {
        WxUserAuthContext.clear();
    }

    @Test
    void getActivityListShouldReturnActiveActivities()
    {
        List<TMarketingActivity> activities = Collections.singletonList(new TMarketingActivity());
        when(marketingActivityEngine.getActiveActivities()).thenReturn(activities);

        AjaxResult result = controller.getActivityList();

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(activities, result.get(AjaxResult.DATA_TAG));
        verify(marketingActivityEngine).getActiveActivities();
    }

    @Test
    void previewShouldUseEmptyItemsWhenOrderMissing()
    {
        bindUser(21L);
        MarketingPreviewResult preview = new MarketingPreviewResult();
        when(marketingActivityEngine.previewOrder(org.mockito.ArgumentMatchers.eq(21L), anyList())).thenReturn(preview);

        AjaxResult result = controller.preview(null);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(preview, result.get(AjaxResult.DATA_TAG));
        verify(marketingActivityEngine).previewOrder(org.mockito.ArgumentMatchers.eq(21L), itemsCaptor.capture());
        assertEquals(Collections.emptyList(), itemsCaptor.getValue());
    }

    @Test
    void previewShouldUseCurrentUserAndOrderItems()
    {
        bindUser(22L);
        TOrder order = new TOrder();
        List<TOrderItem> items = Arrays.asList(new TOrderItem(), new TOrderItem());
        order.setOrderItems(items);
        MarketingPreviewResult preview = new MarketingPreviewResult();
        when(marketingActivityEngine.previewOrder(22L, items)).thenReturn(preview);

        AjaxResult result = controller.preview(order);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(preview, result.get(AjaxResult.DATA_TAG));
        verify(marketingActivityEngine).previewOrder(22L, items);
    }

    @Test
    void previewShouldReturnEngineValidationError()
    {
        bindUser(23L);
        when(marketingActivityEngine.previewOrder(org.mockito.ArgumentMatchers.eq(23L), anyList()))
                .thenThrow(new IllegalArgumentException("订单商品不能为空"));

        AjaxResult result = controller.preview(null);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("订单商品不能为空", result.get(AjaxResult.MSG_TAG));
    }

    private static void bindUser(Long userId)
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(userId);
        user.setOpenid("openid-" + userId);
        WxUserAuthContext.setCurrentUser(user);
    }
}
