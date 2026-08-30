package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.coffee.activity.domain.MarketingPreviewResult;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.member.service.MemberService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderStatus;
import com.ruoyi.project.coffee.scanOrder.service.IScanOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class ScanOrderApiControllerTest
{
    private ScanOrderApiController controller;

    @Mock
    private IScanOrderService scanOrderService;

    @Mock
    private MemberService memberService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new ScanOrderApiController();
        ReflectionTestUtils.setField(controller, "scanOrderService", scanOrderService);
        ReflectionTestUtils.setField(controller, "memberService", memberService);
        ReflectionTestUtils.setField(controller, "pickupTemplateId", "template-1");
    }

    @AfterEach
    void tearDown()
    {
        WxUserAuthContext.clear();
    }

    @Test
    void createOrderRequiresLogin()
    {
        AjaxResult result = controller.createOrder(new HashMap<String, Object>());

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("请先登录后再下单", result.get(AjaxResult.MSG_TAG));
        verify(scanOrderService, never()).createOrderFromCart(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createOrderUsesCurrentUserAndTable()
    {
        bindUser(11L, "openid-11");
        ScanOrder order = new ScanOrder();
        order.setOrderId(21L);
        when(scanOrderService.createOrderFromCart(11L, "openid-11", "A01", "少冰", "balance")).thenReturn(order);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("tableNo", " A01 ");
        body.put("remark", "少冰");
        body.put("payType", "balance");

        AjaxResult result = controller.createOrder(body);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals(order, result.get(AjaxResult.DATA_TAG));
    }

    @Test
    void previewOrderReturnsAmountBreakdown()
    {
        bindUser(11L, "openid-11");
        MarketingPreviewResult preview = new MarketingPreviewResult();
        preview.setTotalAmount(new BigDecimal("30.00"));
        preview.setPayAmount(new BigDecimal("24.00"));
        preview.setDiscountAmount(new BigDecimal("4.00"));
        preview.setMemberDiscount(new BigDecimal("2.00"));
        preview.setActivitySummary("满减活动");
        when(scanOrderService.previewOrderFromCart(11L, "openid-11", "A01")).thenReturn(preview);

        AjaxResult result = controller.previewOrder("A01", null);

        Map<?, ?> data = (Map<?, ?>) result.get(AjaxResult.DATA_TAG);
        assertEquals(new BigDecimal("30.00"), data.get("totalAmount"));
        assertEquals(new BigDecimal("24.00"), data.get("payAmount"));
        assertEquals(new BigDecimal("4.00"), data.get("discountAmount"));
        assertEquals(new BigDecimal("2.00"), data.get("memberDiscount"));
        assertEquals("满减活动", data.get("activitySummary"));
    }

    @Test
    void cancelOrderRejectsPaidOrder()
    {
        bindUser(11L, "openid-11");
        ScanOrder order = new ScanOrder();
        order.setOrderId(7L);
        order.setUserId(11L);
        order.setStatus(ScanOrderStatus.MAKING);
        when(scanOrderService.selectScanOrderById(7L)).thenReturn(order);

        AjaxResult result = controller.cancelOrder(7L);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("仅未支付订单可以取消", result.get(AjaxResult.MSG_TAG));
        verify(scanOrderService, never()).cancelOrder(7L);
    }

    @Test
    void urgeOrderDelegatesToServiceForCurrentUser()
    {
        bindUser(11L, "openid-11");
        when(scanOrderService.urgeOrder(7L, 11L)).thenReturn(1);

        AjaxResult result = controller.urgeOrder(7L);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals("已催单，商家会尽快处理", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void subscribeConfigReturnsTemplateId()
    {
        AjaxResult result = controller.subscribeConfig();

        Map<?, ?> data = (Map<?, ?>) result.get(AjaxResult.DATA_TAG);
        assertEquals("template-1", data.get("pickupTemplateId"));
    }

    private void bindUser(Long userId, String openid)
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(userId);
        user.setOpenid(openid);
        WxUserAuthContext.setCurrentUser(user);
    }
}

