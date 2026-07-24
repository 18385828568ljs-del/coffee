package com.ruoyi.project.coffee.scanOrder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.activity.domain.MarketingPreviewResult;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.payment.service.PaymentLogService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderItem;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderStatus;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanCartMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanOrderItemMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanOrderMapper;
import com.ruoyi.project.coffee.scanOrder.service.impl.ScanOrderServiceImpl;
import com.ruoyi.project.coffee.scanOrder.wx.ScanOrderSubscribeMessageService;
import com.ruoyi.project.coffee.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class ScanOrderServiceImplTest
{
    private ScanOrderServiceImpl orderService;

    @Mock
    private ScanOrderMapper scanOrderMapper;

    @Mock
    private ScanOrderItemMapper scanOrderItemMapper;

    @Mock
    private ScanCartMapper scanCartMapper;

    @Mock
    private WalletService walletService;

    @Mock
    private MarketingActivityEngine marketingActivityEngine;

    @Mock
    private IScanTableQrcodeService scanTableQrcodeService;

    @Mock
    private ScanOrderSubscribeMessageService subscribeMessageService;

    @Mock
    private PaymentLogService paymentLogService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        orderService = new ScanOrderServiceImpl();
        ReflectionTestUtils.setField(orderService, "scanOrderMapper", scanOrderMapper);
        ReflectionTestUtils.setField(orderService, "scanOrderItemMapper", scanOrderItemMapper);
        ReflectionTestUtils.setField(orderService, "scanCartMapper", scanCartMapper);
        ReflectionTestUtils.setField(orderService, "walletService", walletService);
        ReflectionTestUtils.setField(orderService, "marketingActivityEngine", marketingActivityEngine);
        ReflectionTestUtils.setField(orderService, "scanTableQrcodeService", scanTableQrcodeService);
        ReflectionTestUtils.setField(orderService, "subscribeMessageService", subscribeMessageService);
        ReflectionTestUtils.setField(orderService, "paymentLogService", paymentLogService);
    }

    @Test
    void createOrderFromCartShouldValidateTableAndSettleOnlySelectedActiveItems()
    {
        ScanTableQrcode table = table(1);
        ScanCart selected = buildCart(101L, 1001L, "招牌拿铁", "热 / 少糖", new BigDecimal("18.00"), 2, 1, 1, 0);
        ScanCart unselected = buildCart(102L, 1002L, "美式咖啡", "冰", new BigDecimal("12.00"), 1, 0, 1, 0);
        ScanCart deleted = buildCart(103L, 1003L, "已删商品", "热", new BigDecimal("15.00"), 1, 1, 1, 1);
        ScanCart inactive = buildCart(104L, 1004L, "失效商品", "热", new BigDecimal("16.00"), 1, 1, 0, 0);
        when(scanTableQrcodeService.selectByShopAndTable(1L, " A01 ")).thenReturn(table);
        when(scanCartMapper.selectScanCartList(any(ScanCart.class)))
            .thenReturn(Arrays.asList(selected, unselected, deleted, inactive));

        ScanOrder result = orderService.createOrderFromCart(7L, " openid-001 ", 1L,
            " A01 ", "少放糖", " balance ");

        assertEquals(Long.valueOf(7L), result.getUserId());
        assertEquals("openid-001", result.getOpenid());
        assertEquals("A01", result.getTableNo());
        assertEquals("dine_in", result.getScene());
        assertEquals(Integer.valueOf(ScanOrderStatus.PENDING_PAY), result.getStatus());
        assertEquals("balance", result.getPayType());
        assertEquals("少放糖", result.getRemark());
        assertNotNull(result.getOrderNo());
        assertEquals(1, result.getItems().size());
        assertEquals(Long.valueOf(1001L), result.getItems().get(0).getProductId());
        assertEquals("招牌拿铁", result.getItems().get(0).getProductName());
        assertEquals("热 / 少糖", result.getItems().get(0).getSpec());
        assertEquals(new BigDecimal("18.00"), result.getItems().get(0).getPrice());
        assertEquals(Integer.valueOf(2), result.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("36.00"), result.getItems().get(0).getTotalPrice());

        ArgumentCaptor<ScanCart> queryCaptor = ArgumentCaptor.forClass(ScanCart.class);
        verify(scanCartMapper).selectScanCartList(queryCaptor.capture());
        assertEquals(Long.valueOf(7L), queryCaptor.getValue().getUserId());
        assertEquals(Long.valueOf(1L), queryCaptor.getValue().getShopId());
        assertEquals("A01", queryCaptor.getValue().getTableNo());
        assertEquals(Integer.valueOf(1), queryCaptor.getValue().getStatus());

        ArgumentCaptor<ScanOrder> orderCaptor = ArgumentCaptor.forClass(ScanOrder.class);
        verify(marketingActivityEngine).applyScanOrderMarketing(eq(result), eq(Arrays.asList(selected)));
        verify(scanOrderMapper).insertScanOrder(orderCaptor.capture());
        assertEquals(result, orderCaptor.getValue());
        verify(scanOrderItemMapper).batchInsertScanOrderItem(result.getItems());
        verify(scanCartMapper).logicDeleteById(101L);
        verify(scanCartMapper, never()).logicDeleteById(102L);
        verify(scanCartMapper, never()).logicDeleteById(103L);
        verify(scanCartMapper, never()).logicDeleteById(104L);
    }

    @Test
    void createOrderFromCartShouldRejectMissingOrDisabledTable()
    {
        when(scanTableQrcodeService.selectByShopAndTable(1L, "A01")).thenReturn(null);

        ServiceException missing = assertThrows(ServiceException.class,
            () -> orderService.createOrderFromCart(7L, "openid", 1L, "A01", null, "balance"));

        assertEquals("桌台不存在,请重新扫码", missing.getMessage());

        ScanTableQrcode disabled = table(0);
        when(scanTableQrcodeService.selectByShopAndTable(1L, "A01")).thenReturn(disabled);
        ServiceException stopped = assertThrows(ServiceException.class,
            () -> orderService.createOrderFromCart(7L, "openid", 1L, "A01", null, "balance"));

        assertEquals("桌台已停用,请联系工作人员", stopped.getMessage());
        verify(scanCartMapper, never()).selectScanCartList(any());
        verify(scanOrderMapper, never()).insertScanOrder(any());
    }

    @Test
    void createOrderFromCartShouldRejectEmptySelectedCart()
    {
        ScanCart unselected = buildCart(102L, 1002L, "美式咖啡", "冰", new BigDecimal("12.00"), 1, 0, 1, 0);
        when(scanCartMapper.selectScanCartList(any(ScanCart.class))).thenReturn(Arrays.asList(unselected));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> orderService.createOrderFromCart(7L, "openid", 1L, null, null, "balance"));

        assertEquals("购物车为空", exception.getMessage());
        verify(marketingActivityEngine, never()).applyScanOrderMarketing(any(), any());
        verify(scanOrderMapper, never()).insertScanOrder(any());
    }

    @Test
    void createOrderFromCartShouldConvertMarketingExceptionToServiceExceptionBeforePersisting()
    {
        ScanCart selected = buildCart(101L, 1001L, "招牌拿铁", "热", new BigDecimal("18.00"), 1, 1, 1, 0);
        when(scanCartMapper.selectScanCartList(any(ScanCart.class))).thenReturn(Arrays.asList(selected));
        when(marketingActivityEngine.applyScanOrderMarketing(any(ScanOrder.class), eq(Arrays.asList(selected))))
            .thenThrow(new IllegalArgumentException("点单商品不存在或已下架"));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> orderService.createOrderFromCart(7L, "openid", 1L, null, null, "balance"));

        assertEquals("点单商品不存在或已下架", exception.getMessage());
        verify(scanOrderMapper, never()).insertScanOrder(any());
        verify(scanOrderItemMapper, never()).batchInsertScanOrderItem(any());
        verify(scanCartMapper, never()).logicDeleteById(any());
    }

    @Test
    void previewOrderFromCartShouldReturnEmptyPreviewForAnonymousEmptyOrInvalidCart()
    {
        MarketingPreviewResult anonymous = orderService.previewOrderFromCart(null, "openid", 1L, "A01");
        assertEquals(BigDecimal.ZERO, anonymous.getPayAmount());
        verify(scanCartMapper, never()).selectScanCartList(any());

        when(scanCartMapper.selectScanCartList(any(ScanCart.class))).thenReturn(Arrays.asList());
        MarketingPreviewResult empty = orderService.previewOrderFromCart(7L, "openid", 1L, "A01");
        assertEquals(BigDecimal.ZERO, empty.getPayAmount());
        verify(marketingActivityEngine, never()).previewScanOrder(any(), any());

        ScanCart selected = buildCart(101L, 1001L, "招牌拿铁", "热", new BigDecimal("18.00"), 1, 1, 1, 0);
        when(scanCartMapper.selectScanCartList(any(ScanCart.class))).thenReturn(Arrays.asList(selected));
        when(marketingActivityEngine.previewScanOrder(7L, Arrays.asList(selected)))
            .thenThrow(new IllegalArgumentException("点单商品不存在或已下架"));
        MarketingPreviewResult invalid = orderService.previewOrderFromCart(7L, "openid", 1L, "A01");

        assertEquals(BigDecimal.ZERO, invalid.getPayAmount());
    }

    @Test
    void previewOrderFromCartShouldPassSelectedActiveItemsToMarketingEngine()
    {
        ScanCart selected = buildCart(101L, 1001L, "招牌拿铁", "热", new BigDecimal("18.00"), 2, 1, 1, 0);
        ScanCart defaultSelected = buildCart(102L, 1002L, "美式咖啡", "冰", new BigDecimal("12.00"), 1, null, 1, 0);
        ScanCart inactive = buildCart(103L, 1003L, "失效商品", "热", new BigDecimal("15.00"), 1, 1, 0, 0);
        MarketingPreviewResult preview = new MarketingPreviewResult();
        preview.setTotalAmount(new BigDecimal("48.00"));
        preview.setDiscountAmount(new BigDecimal("8.00"));
        preview.setPayAmount(new BigDecimal("40.00"));
        when(scanCartMapper.selectScanCartList(any(ScanCart.class)))
            .thenReturn(Arrays.asList(selected, defaultSelected, inactive));
        when(marketingActivityEngine.previewScanOrder(7L, Arrays.asList(selected, defaultSelected))).thenReturn(preview);

        MarketingPreviewResult result = orderService.previewOrderFromCart(7L, " openid ", 1L, " A01 ");

        assertEquals(new BigDecimal("48.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("8.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("40.00"), result.getPayAmount());
        ArgumentCaptor<ScanCart> queryCaptor = ArgumentCaptor.forClass(ScanCart.class);
        verify(scanCartMapper).selectScanCartList(queryCaptor.capture());
        assertEquals(Long.valueOf(7L), queryCaptor.getValue().getUserId());
        assertEquals("A01", queryCaptor.getValue().getTableNo());
        verify(marketingActivityEngine).previewScanOrder(7L, Arrays.asList(selected, defaultSelected));
    }

    @Test
    void payOrderShouldDeductWalletAfterStatusLockedForBalancePay()
    {
        ScanOrder order = pendingOrder();
        order.setPayType("balance");
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);
        when(scanOrderMapper.selectMaxPickupNoToday(1L)).thenReturn(8);
        when(scanOrderMapper.countUnfinishedPaidOrders(1L)).thenReturn(2);
        when(scanOrderMapper.updateScanOrderStatus(eq(21L), eq(ScanOrderStatus.PENDING_PAY),
            eq(ScanOrderStatus.MAKING), any(Date.class), any(Date.class), any(Date.class),
            eq(null), eq(null), eq(null), eq("009"), eq(15), eq("balance"))).thenReturn(1);

        int rows = orderService.payOrder(21L, null);

        assertEquals(1, rows);
        verify(walletService).deductForOrder(7L, new BigDecimal("24.00"), "SO001");
    }

    @Test
    void payOrderShouldRejectUnsupportedPayTypeBeforeStatusUpdate()
    {
        ScanOrder order = pendingOrder();
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.payOrder(21L, "cash"));

        assertEquals("不支持的支付方式: cash", exception.getMessage());
        verify(scanOrderMapper, never()).updateScanOrderStatus(any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any(), any());
        verify(walletService, never()).deductForOrder(any(), any(), any());
    }

    @Test
    void payOrderShouldRejectInvalidAmountAfterStatusLockWithoutWalletDeduction()
    {
        ScanOrder order = pendingOrder();
        order.setPayType("balance");
        order.setPayAmount(BigDecimal.ZERO);
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);
        when(scanOrderMapper.selectMaxPickupNoToday(1L)).thenReturn(1);
        when(scanOrderMapper.countUnfinishedPaidOrders(1L)).thenReturn(0);
        when(scanOrderMapper.updateScanOrderStatus(eq(21L), eq(ScanOrderStatus.PENDING_PAY),
            eq(ScanOrderStatus.MAKING), any(Date.class), any(Date.class), any(Date.class),
            eq(null), eq(null), eq(null), eq("002"), eq(5), eq("balance"))).thenReturn(1);

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.payOrder(21L, null));

        assertEquals("订单金额异常,无法支付", exception.getMessage());
        verify(walletService, never()).deductForOrder(any(), any(), any());
    }

    @Test
    void payOrderShouldNotDeductWhenStatusLockFails()
    {
        ScanOrder order = pendingOrder();
        order.setPayType("balance");
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);
        when(scanOrderMapper.selectMaxPickupNoToday(1L)).thenReturn(null);
        when(scanOrderMapper.countUnfinishedPaidOrders(1L)).thenReturn(0);
        when(scanOrderMapper.updateScanOrderStatus(eq(21L), eq(ScanOrderStatus.PENDING_PAY),
            eq(ScanOrderStatus.MAKING), any(Date.class), any(Date.class), any(Date.class),
            eq(null), eq(null), eq(null), eq("001"), eq(5), eq("balance"))).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.payOrder(21L, null));

        assertEquals("订单状态已变更,支付失败,请刷新后重试", exception.getMessage());
        verify(walletService, never()).deductForOrder(any(), any(), any());
    }

    @Test
    void payOrderShouldRejectUnsupportedPayType()
    {
        ScanOrder order = pendingOrder();
        order.setPayType("wechat");
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.payOrder(21L, "wechat"));

        assertEquals("不支持的支付方式: wechat", exception.getMessage());
        verify(scanOrderMapper, never()).updateScanOrderStatus(any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any(), any());
        verify(walletService, never()).deductForOrder(any(), any(), any());
    }

    @Test
    void urgeOrderShouldRejectOtherUsersOrder()
    {
        ScanOrder order = pendingOrder();
        order.setUserId(7L);
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.urgeOrder(21L, 8L));

        assertEquals("订单不存在", exception.getMessage());
        verify(scanOrderMapper, never()).markUrged(any(), any());
    }

    @Test
    void urgeOrderShouldRejectTooFrequentRequest()
    {
        ScanOrder order = pendingOrder();
        order.setStatus(ScanOrderStatus.MAKING);
        order.setLastUrgeTime(new Date(System.currentTimeMillis() - 1000L));
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.urgeOrder(21L, 7L));

        assertEquals("催单太频繁，请稍后再试", exception.getMessage());
        verify(scanOrderMapper, never()).markUrged(any(), any());
    }

    @Test
    void urgeOrderShouldRejectRefundingOrder()
    {
        ScanOrder order = pendingOrder();
        order.setStatus(ScanOrderStatus.MAKING);
        order.setRefundStatus(1);
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.urgeOrder(21L, 7L));

        assertEquals("退款处理中，不能催单", exception.getMessage());
        verify(scanOrderMapper, never()).markUrged(any(), any());
    }

    @Test
    void urgeOrderShouldRejectRefundedOrder()
    {
        ScanOrder order = pendingOrder();
        order.setStatus(ScanOrderStatus.WAIT_PICKUP);
        order.setRefundStatus(2);
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> orderService.urgeOrder(21L, 7L));

        assertEquals("订单已退款，不能催单", exception.getMessage());
        verify(scanOrderMapper, never()).markUrged(any(), any());
    }

    @Test
    void urgeOrderShouldMarkUnfinishedPaidOrder()
    {
        ScanOrder order = pendingOrder();
        order.setStatus(ScanOrderStatus.WAIT_PICKUP);
        order.setLastUrgeTime(new Date(System.currentTimeMillis() - 120000L));
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);
        when(scanOrderMapper.markUrged(eq(21L), any(Date.class))).thenReturn(1);

        int rows = orderService.urgeOrder(21L, 7L);

        assertEquals(1, rows);
        verify(scanOrderMapper).markUrged(eq(21L), any(Date.class));
    }

    @Test
    void selectScanOrderWithItemsShouldReturnNullWhenOrderMissing()
    {
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(null);

        ScanOrder order = orderService.selectScanOrderWithItems(21L);

        assertNull(order);
        verify(scanOrderItemMapper, never()).selectItemsByOrderId(any());
    }

    @Test
    void selectScanOrderWithItemsShouldAttachEmptyListWhenMapperReturnsNull()
    {
        ScanOrder order = pendingOrder();
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);
        when(scanOrderItemMapper.selectItemsByOrderId(21L)).thenReturn(null);

        ScanOrder result = orderService.selectScanOrderWithItems(21L);

        assertNotNull(result.getItems());
        assertEquals(0, result.getItems().size());
    }

    @Test
    void selectScanOrderWithItemsShouldAttachItemsWhenPresent()
    {
        ScanOrder order = pendingOrder();
        List<ScanOrderItem> items = Arrays.asList(buildItem(1L, "招牌奶茶"), buildItem(2L, "美式咖啡"));
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);
        when(scanOrderItemMapper.selectItemsByOrderId(21L)).thenReturn(items);

        ScanOrder result = orderService.selectScanOrderWithItems(21L);

        assertEquals(2, result.getItems().size());
        assertEquals("招牌奶茶", result.getItems().get(0).getProductName());
        assertEquals("美式咖啡", result.getItems().get(1).getProductName());
    }

    @Test
    void selectScanOrderListShouldAttachItemsForListCards()
    {
        ScanOrder first = pendingOrder();
        first.setOrderId(21L);
        ScanOrder second = pendingOrder();
        second.setOrderId(22L);
        ScanOrderItem firstItem = buildItem(1L, "招牌奶茶");
        firstItem.setOrderId(21L);
        firstItem.setProductImage("milk-tea.png");
        ScanOrderItem secondItem = buildItem(2L, "美式咖啡");
        secondItem.setOrderId(22L);
        secondItem.setProductImage("coffee.png");
        when(scanOrderMapper.selectScanOrderList(any(ScanOrder.class))).thenReturn(Arrays.asList(first, second));
        when(scanOrderItemMapper.selectItemsByOrderIds(Arrays.asList(21L, 22L)))
            .thenReturn(Arrays.asList(firstItem, secondItem));

        List<ScanOrder> result = orderService.selectScanOrderList(new ScanOrder());

        assertEquals(1, result.get(0).getItems().size());
        assertEquals("milk-tea.png", result.get(0).getItems().get(0).getProductImage());
        assertEquals(1, result.get(1).getItems().size());
        assertEquals("coffee.png", result.get(1).getItems().get(0).getProductImage());
        verify(scanOrderItemMapper).selectItemsByOrderIds(Arrays.asList(21L, 22L));
    }

    @Test
    void selectMyOrderListShouldReturnEmptyForAnonymousUser()
    {
        List<ScanOrder> orders = orderService.selectMyOrderList(null);

        assertNotNull(orders);
        assertEquals(0, orders.size());
        verify(scanOrderMapper, never()).selectScanOrderList(any());
    }

    @Test
    void selectMyOrderListShouldQueryCurrentUserOnly()
    {
        List<ScanOrder> orders = Arrays.asList(pendingOrder());
        when(scanOrderMapper.selectScanOrderList(any(ScanOrder.class))).thenReturn(orders);

        List<ScanOrder> result = orderService.selectMyOrderList(7L);

        assertEquals(1, result.size());
        assertEquals("SO001", result.get(0).getOrderNo());
        ArgumentCaptor<ScanOrder> captor = ArgumentCaptor.forClass(ScanOrder.class);
        verify(scanOrderMapper).selectScanOrderList(captor.capture());
        assertEquals(Long.valueOf(7L), captor.getValue().getUserId());
    }

    @Test
    void cancelOrderShouldMovePendingPaymentToCancelled()
    {
        when(scanOrderMapper.updateScanOrderStatus(eq(21L), eq(ScanOrderStatus.PENDING_PAY),
            eq(ScanOrderStatus.CANCELLED), isNull(), isNull(), isNull(), isNull(), isNull(),
            any(Date.class), isNull(), isNull(), isNull())).thenReturn(1);

        int rows = orderService.cancelOrder(21L);

        assertEquals(1, rows);
    }

    @Test
    void callOrderShouldNotifyAfterStatusChanged()
    {
        ScanOrder order = pendingOrder();
        order.setStatus(ScanOrderStatus.WAIT_PICKUP);
        when(scanOrderMapper.updateScanOrderStatus(eq(21L), eq(ScanOrderStatus.MAKING),
            eq(ScanOrderStatus.WAIT_PICKUP), isNull(), isNull(), isNull(), any(Date.class),
            isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(1);
        when(scanOrderMapper.selectScanOrderById(21L)).thenReturn(order);

        int rows = orderService.callOrder(21L);

        assertEquals(1, rows);
        verify(subscribeMessageService).sendPickupNoticeAsync(order);
    }

    @Test
    void callOrderShouldNotNotifyWhenStatusDoesNotChange()
    {
        when(scanOrderMapper.updateScanOrderStatus(eq(21L), eq(ScanOrderStatus.MAKING),
            eq(ScanOrderStatus.WAIT_PICKUP), isNull(), isNull(), isNull(), any(Date.class),
            isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(0);

        int rows = orderService.callOrder(21L);

        assertEquals(0, rows);
        verify(scanOrderMapper, never()).selectScanOrderById(21L);
        verify(subscribeMessageService, never()).sendPickupNoticeAsync(any());
    }

    @Test
    void completeOrderShouldMoveWaitPickupToCompleted()
    {
        when(scanOrderMapper.updateScanOrderStatus(eq(21L), eq(ScanOrderStatus.WAIT_PICKUP),
            eq(ScanOrderStatus.COMPLETED), isNull(), isNull(), isNull(), isNull(),
            any(Date.class), isNull(), isNull(), isNull(), isNull())).thenReturn(1);

        int rows = orderService.completeOrder(21L);

        assertEquals(1, rows);
    }

    @Test
    void cancelPaidOrderShouldMoveMakingToCancelled()
    {
        when(scanOrderMapper.updateScanOrderStatus(eq(21L), eq(ScanOrderStatus.MAKING),
            eq(ScanOrderStatus.CANCELLED), isNull(), isNull(), isNull(), isNull(), isNull(),
            any(Date.class), isNull(), isNull(), isNull())).thenReturn(1);

        int rows = orderService.cancelPaidOrder(21L);

        assertEquals(1, rows);
    }

    private ScanOrder pendingOrder()
    {
        ScanOrder order = new ScanOrder();
        order.setOrderId(21L);
        order.setOrderNo("SO001");
        order.setUserId(7L);
        order.setShopId(1L);
        order.setStatus(ScanOrderStatus.PENDING_PAY);
        order.setPayAmount(new BigDecimal("24.00"));
        return order;
    }

    private ScanCart buildCart(Long id, Long productId, String productName, String spec,
        BigDecimal price, Integer quantity, Integer selected, Integer status, Integer delFlag)
    {
        ScanCart cart = new ScanCart();
        cart.setId(id);
        cart.setUserId(7L);
        cart.setOpenid("openid-001");
        cart.setShopId(1L);
        cart.setTableNo("A01");
        cart.setProductId(productId);
        cart.setProductName(productName);
        cart.setProductImage(productId + ".png");
        cart.setSpecText(spec);
        cart.setPrice(price);
        cart.setQuantity(quantity);
        cart.setSelected(selected);
        cart.setStatus(status);
        cart.setDelFlag(delFlag);
        return cart;
    }

    private ScanTableQrcode table(Integer status)
    {
        ScanTableQrcode table = new ScanTableQrcode();
        table.setTableId(88L);
        table.setShopId(1L);
        table.setTableNo("A01");
        table.setStatus(status);
        return table;
    }

    private ScanOrderItem buildItem(Long productId, String productName)
    {
        ScanOrderItem item = new ScanOrderItem();
        item.setOrderId(21L);
        item.setProductId(productId);
        item.setProductName(productName);
        item.setPrice(new BigDecimal("12.00"));
        item.setQuantity(1);
        item.setTotalPrice(new BigDecimal("12.00"));
        return item;
    }
}
