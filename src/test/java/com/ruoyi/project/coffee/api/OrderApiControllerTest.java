package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.coffee.activity.domain.MarketingPreviewResult;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.cart.domain.TCart;
import com.ruoyi.project.coffee.cart.service.ITCartService;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import com.ruoyi.project.coffee.item.service.ITOrderItemService;
import com.ruoyi.project.coffee.member.service.MemberService;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.order.service.ITOrderService;
import com.ruoyi.project.coffee.payment.service.PaymentLogService;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.wallet.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class OrderApiControllerTest
{
    private OrderApiController controller;

    @Mock
    private ITOrderService orderService;

    @Mock
    private ITOrderItemService orderItemService;

    @Mock
    private ITProductService productService;

    @Mock
    private ITCartService cartService;

    @Mock
    private MarketingActivityEngine marketingActivityEngine;

    @Mock
    private WalletService walletService;

    @Mock
    private MemberService memberService;

    @Mock
    private PaymentLogService paymentLogService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new OrderApiController();
        ReflectionTestUtils.setField(controller, "orderService", orderService);
        ReflectionTestUtils.setField(controller, "orderItemService", orderItemService);
        ReflectionTestUtils.setField(controller, "productService", productService);
        ReflectionTestUtils.setField(controller, "cartService", cartService);
        ReflectionTestUtils.setField(controller, "marketingActivityEngine", marketingActivityEngine);
        ReflectionTestUtils.setField(controller, "walletService", walletService);
        ReflectionTestUtils.setField(controller, "memberService", memberService);
        ReflectionTestUtils.setField(controller, "paymentLogService", paymentLogService);
        bindUser(10L);
    }

    @AfterEach
    void tearDown()
    {
        WxUserAuthContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createOrderShouldRejectMissingLogin()
    {
        WxUserAuthContext.clear();
        TOrder order = orderWithItem();

        AjaxResult result = controller.createOrder(order);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("用户信息缺失", result.get(AjaxResult.MSG_TAG));
        verify(orderService, never()).insertTOrder(any(TOrder.class));
    }

    @Test
    void createOrderShouldRejectOtherUsersCartBeforePersisting()
    {
        TOrder order = orderWithItem();
        order.setCartIds(Collections.singletonList(88L));
        TCart cart = new TCart();
        cart.setCartId(88L);
        cart.setUserId(99L);
        cart.setProductId(1001L);
        cart.setQuantity(1L);
        when(cartService.selectTCartByCartId(88L)).thenReturn(cart);

        AjaxResult result = controller.createOrder(order);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("购物车商品已失效，请刷新后重试", result.get(AjaxResult.MSG_TAG));
        verify(productService, never()).decreaseStock(any(), any());
        verify(orderService, never()).insertTOrder(any(TOrder.class));
    }

    @Test
    void createOrderShouldOverwriteClientAmountsAndPersistServerCalculatedSnapshot()
    {
        TOrder order = orderWithItem();
        order.setTotalAmount(new BigDecimal("999.00"));
        order.setPayAmount(new BigDecimal("1.00"));
        order.setDiscountAmount(new BigDecimal("998.00"));
        order.setMemberDiscount(new BigDecimal("998.00"));
        order.setFreightAmount(new BigDecimal("998.00"));
        order.setActivitySummary("客户端伪造优惠");
        TProduct product = product(1001L, "阿布咖啡豆", new BigDecimal("18.00"), 10L);
        when(productService.selectTProductByProductId(1001L)).thenReturn(product);
        when(marketingActivityEngine.applyOrderMarketing(eq(order), any())).thenAnswer(invocation -> {
            TOrder target = invocation.getArgument(0);
            MarketingPreviewResult result = new MarketingPreviewResult();
            result.setTotalAmount(new BigDecimal("36.00"));
            result.setPayAmount(new BigDecimal("31.00"));
            result.setDiscountAmount(new BigDecimal("5.00"));
            result.setMemberDiscount(BigDecimal.ZERO);
            result.setFreightAmount(BigDecimal.ZERO);
            result.setActivitySummary("满 30 减 5");
            target.setTotalAmount(result.getTotalAmount());
            target.setPayAmount(result.getPayAmount());
            target.setDiscountAmount(result.getDiscountAmount());
            target.setMemberDiscount(result.getMemberDiscount());
            target.setFreightAmount(result.getFreightAmount());
            target.setActivitySummary(result.getActivitySummary());
            return result;
        });
        when(orderService.selectTOrderList(any(TOrder.class))).thenReturn(Collections.emptyList());
        when(productService.decreaseStock(1001L, 2L)).thenReturn(1);
        when(orderService.insertTOrder(order)).thenAnswer(invocation -> {
            order.setOrderId(3001L);
            return 1;
        });

        AjaxResult result = controller.createOrder(order);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals("订单创建成功", result.get(AjaxResult.MSG_TAG));
        assertEquals(new BigDecimal("36.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("31.00"), order.getPayAmount());
        assertEquals(new BigDecimal("5.00"), order.getDiscountAmount());
        assertEquals("满 30 减 5", order.getActivitySummary());
        assertEquals("阿布咖啡豆", order.getOrderItems().get(0).getProductName());
        assertEquals("bean.png", order.getOrderItems().get(0).getProductImage());
        assertEquals(new BigDecimal("18.00"), order.getOrderItems().get(0).getPrice());
        assertEquals(new BigDecimal("36.00"), order.getOrderItems().get(0).getTotalPrice());
        verify(orderItemService).insertTOrderItem(order.getOrderItems().get(0));
        verify(productService).decreaseStock(1001L, 2L);
    }

    @Test
    void createOrderShouldReturnExistingPendingOrderForDuplicateSubmission()
    {
        TOrder order = orderWithItem();
        TProduct product = product(1001L, "阿布咖啡豆", new BigDecimal("18.00"), 10L);
        TOrder pending = orderWithItem();
        pending.setOrderId(7001L);
        pending.setUserId(10L);
        pending.setStatus(0);
        pending.setCreateTime(new Date());
        TOrderItem pendingItem = orderItem(1001L, 2L);
        pendingItem.setPrice(new BigDecimal("18.00"));
        pendingItem.setSpec("默认规格");
        when(productService.selectTProductByProductId(1001L)).thenReturn(product);
        when(marketingActivityEngine.applyOrderMarketing(eq(order), any())).thenReturn(new MarketingPreviewResult());
        when(orderService.selectTOrderList(any(TOrder.class))).thenReturn(Collections.singletonList(pending));
        when(orderItemService.selectTOrderItemList(any(TOrderItem.class))).thenReturn(Collections.singletonList(pendingItem));

        AjaxResult result = controller.createOrder(order);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals("订单已创建，请勿重复提交", result.get(AjaxResult.MSG_TAG));
        assertEquals(7001L, result.get(AjaxResult.DATA_TAG));
        verify(orderService, never()).insertTOrder(any(TOrder.class));
        verify(productService, never()).decreaseStock(any(), any());
    }

    @Test
    void payOrderShouldRejectOtherUsersOrderWithoutDeductingWallet()
    {
        TOrder order = orderWithItem();
        order.setOrderId(12L);
        order.setUserId(99L);
        order.setStatus(0);
        order.setPayType("balance");
        when(orderService.selectTOrderByOrderId(12L)).thenReturn(order);

        AjaxResult result = controller.payOrder(12L);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("订单不存在", result.get(AjaxResult.MSG_TAG));
        verify(walletService, never()).deductForOrder(any(), any(), any());
        verify(orderService, never()).changeOrderStatus(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void payOrderShouldDeductWalletThenMovePendingOrderToPaid()
    {
        TOrder order = orderWithItem();
        order.setOrderId(12L);
        order.setOrderNo("CF001");
        order.setUserId(10L);
        order.setStatus(0);
        order.setPayType("balance");
        order.setPayAmount(new BigDecimal("31.00"));
        when(orderService.selectTOrderByOrderId(12L)).thenReturn(order);
        when(orderService.changeOrderStatus(eq(12L), eq(0), eq(1), any(Date.class),
            isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(1);

        AjaxResult result = controller.payOrder(12L);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals("支付成功", result.get(AjaxResult.MSG_TAG));
        verify(walletService).deductForOrder(10L, new BigDecimal("31.00"), "CF001");
        verify(memberService).addSpending(10L, new BigDecimal("31.00"));
    }

    @Test
    void payOrderShouldRejectNonPendingOrderWithoutDeductingWallet()
    {
        TOrder order = orderWithItem();
        order.setOrderId(12L);
        order.setUserId(10L);
        order.setStatus(1);
        order.setPayType("balance");
        when(orderService.selectTOrderByOrderId(12L)).thenReturn(order);

        AjaxResult result = controller.payOrder(12L);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("当前订单状态不可支付", result.get(AjaxResult.MSG_TAG));
        verify(walletService, never()).deductForOrder(any(), any(), any());
    }

    @Test
    void cancelOrderShouldRestoreStockAndMovePendingOrderToCancelled()
    {
        TOrder order = orderWithItem();
        order.setOrderId(12L);
        order.setOrderNo("CF001");
        order.setUserId(10L);
        order.setStatus(0);
        List<TOrderItem> items = Arrays.asList(orderItem(1001L, 2L), orderItem(1002L, 1L));
        when(orderService.selectTOrderByOrderId(12L)).thenReturn(order);
        when(orderItemService.selectTOrderItemList(any(TOrderItem.class))).thenReturn(items);
        when(productService.increaseStock(1001L, 2L)).thenReturn(1);
        when(productService.increaseStock(1002L, 1L)).thenReturn(1);
        when(orderService.changeOrderStatus(eq(12L), eq(0), eq(4), isNull(), isNull(),
            isNull(), any(Date.class), isNull(), isNull())).thenReturn(1);

        AjaxResult result = controller.cancelOrder(12L);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals("订单已取消", result.get(AjaxResult.MSG_TAG));
        verify(productService).increaseStock(1001L, 2L);
        verify(productService).increaseStock(1002L, 1L);
    }

    @Test
    void getOrderDetailShouldRejectOtherUsersOrder()
    {
        TOrder order = orderWithItem();
        order.setOrderId(12L);
        order.setUserId(99L);
        when(orderService.selectTOrderByOrderId(12L)).thenReturn(order);

        AjaxResult result = controller.getOrderDetail(12L);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("订单不存在", result.get(AjaxResult.MSG_TAG));
        verify(orderItemService, never()).selectTOrderItemList(any(TOrderItem.class));
    }

    @Test
    void getOrderDetailShouldLoadItemsForCurrentUsersOrder()
    {
        TOrder order = orderWithItem();
        order.setOrderId(12L);
        order.setUserId(10L);
        when(orderService.selectTOrderByOrderId(12L)).thenReturn(order);
        when(orderItemService.selectTOrderItemList(any(TOrderItem.class))).thenReturn(Collections.singletonList(orderItem(1001L, 2L)));

        AjaxResult result = controller.getOrderDetail(12L);

        TOrder data = (TOrder) result.get(AjaxResult.DATA_TAG);
        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertNotNull(data.getOrderItems());
        assertEquals(1, data.getOrderItems().size());
    }

    @Test
    void getOrderListShouldReturnEmptyForInvalidStatus()
    {
        assertEquals(0, controller.getOrderList("abc").getRows().size());
        verify(orderService, never()).selectTOrderList(any(TOrder.class));
    }

    @Test
    void getOrderListShouldQueryCurrentUserAndLoadItems()
    {
        bindRequest();
        TOrder order = orderWithItem();
        order.setOrderId(12L);
        order.setUserId(10L);
        when(orderService.selectTOrderList(any(TOrder.class))).thenReturn(Collections.singletonList(order));
        TOrderItem item = orderItem(1001L, 2L);
        item.setOrderId(12L);
        when(orderItemService.selectTOrderItemListByOrderIds(Collections.singletonList(12L))).thenReturn(Collections.singletonList(item));

        List<?> rows = controller.getOrderList("1").getRows();

        ArgumentCaptor<TOrder> captor = ArgumentCaptor.forClass(TOrder.class);
        verify(orderService).selectTOrderList(captor.capture());
        assertEquals(Long.valueOf(10L), captor.getValue().getUserId());
        assertEquals(Integer.valueOf(1), captor.getValue().getStatus());
        assertEquals(1, rows.size());
        assertEquals(1, ((TOrder) rows.get(0)).getOrderItems().size());
    }

    private void bindRequest()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("pageNum", "1");
        request.addParameter("pageSize", "10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private TOrder orderWithItem()
    {
        TOrder order = new TOrder();
        order.setUserId(10L);
        order.setReceiverName("测试用户");
        order.setReceiverPhone("13800000000");
        order.setReceiverAddress("广东省广州市测试路 1 号");
        order.setPayType("balance");
        order.setOrderItems(Collections.singletonList(orderItem(1001L, 2L)));
        return order;
    }

    private TOrderItem orderItem(Long productId, Long quantity)
    {
        TOrderItem item = new TOrderItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setSpec("");
        item.setPrice(new BigDecimal("0.01"));
        item.setTotalPrice(new BigDecimal("0.02"));
        return item;
    }

    private TProduct product(Long productId, String productName, BigDecimal price, Long stock)
    {
        TProduct product = new TProduct();
        product.setProductId(productId);
        product.setProductName(productName);
        product.setImageUrl("bean.png");
        product.setPrice(price);
        product.setStock(stock);
        product.setStatus(1);
        product.setRemark("默认规格");
        return product;
    }

    private void bindUser(Long userId)
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(userId);
        user.setOpenid("openid-" + userId);
        WxUserAuthContext.setCurrentUser(user);
    }
}
