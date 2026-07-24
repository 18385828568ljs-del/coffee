package com.ruoyi.project.coffee.order.task;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import com.ruoyi.project.coffee.item.service.ITOrderItemService;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.order.service.ITOrderService;
import com.ruoyi.project.coffee.product.service.ITProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class OrderTimeoutTaskTest
{
    private OrderTimeoutTask task;

    @Mock
    private ITOrderService orderService;

    @Mock
    private ITOrderItemService orderItemService;

    @Mock
    private ITProductService productService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        task = new OrderTimeoutTask();
        ReflectionTestUtils.setField(task, "orderService", orderService);
        ReflectionTestUtils.setField(task, "orderItemService", orderItemService);
        ReflectionTestUtils.setField(task, "productService", productService);
    }

    @Test
    void cancelTimeoutOrdersShouldOnlyCancelPendingOrdersOlderThanThirtyMinutes()
    {
        TOrder freshOrder = order(1L, "CF001", minutesAgo(5));
        TOrder missingCreateTime = order(2L, "CF002", null);
        TOrder timeoutOrder = order(3L, "CF003", minutesAgo(45));
        when(orderService.selectTOrderList(any(TOrder.class)))
            .thenReturn(Arrays.asList(freshOrder, missingCreateTime, timeoutOrder));
        when(orderService.changeOrderStatus(eq(3L), eq(0), eq(4), isNull(), isNull(), isNull(),
            any(Date.class), isNull(), eq("订单超时自动取消"))).thenReturn(1);
        TOrderItem item = item(1001L, 2L);
        when(orderItemService.selectTOrderItemList(any(TOrderItem.class))).thenReturn(Collections.singletonList(item));
        when(productService.increaseStock(1001L, 2L)).thenReturn(1);

        task.cancelTimeoutOrders();

        verify(orderService).changeOrderStatus(eq(3L), eq(0), eq(4), isNull(), isNull(), isNull(),
            any(Date.class), isNull(), eq("订单超时自动取消"));
        verify(productService).increaseStock(1001L, 2L);
        verify(orderService, never()).changeOrderStatus(eq(1L), any(), any(), any(), any(), any(), any(), any(), any());
        verify(orderService, never()).changeOrderStatus(eq(2L), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelTimeoutOrdersShouldReturnQuietlyWhenNoPendingOrder()
    {
        when(orderService.selectTOrderList(any(TOrder.class))).thenReturn(Collections.emptyList());

        task.cancelTimeoutOrders();

        verify(orderItemService, never()).selectTOrderItemList(any());
        verify(productService, never()).increaseStock(any(), any());
    }

    @Test
    void cancelOrderShouldNotRollbackStockWhenStatusWasChangedByAnotherFlow() throws Exception
    {
        TOrder order = order(9L, "CF009", minutesAgo(60));
        when(orderService.changeOrderStatus(eq(9L), eq(0), eq(4), isNull(), isNull(), isNull(),
            any(Date.class), isNull(), eq("订单超时自动取消"))).thenReturn(0);

        assertThrows(Exception.class, () -> task.cancelOrder(order));

        verify(orderItemService, never()).selectTOrderItemList(any());
        verify(productService, never()).increaseStock(any(), any());
    }

    @Test
    void cancelOrderShouldContinueWhenOneProductStockRollbackFails() throws Exception
    {
        TOrder order = order(10L, "CF010", minutesAgo(60));
        when(orderService.changeOrderStatus(eq(10L), eq(0), eq(4), isNull(), isNull(), isNull(),
            any(Date.class), isNull(), eq("订单超时自动取消"))).thenReturn(1);
        when(orderItemService.selectTOrderItemList(any(TOrderItem.class)))
            .thenReturn(Arrays.asList(item(1001L, 1L), item(1002L, 3L)));
        when(productService.increaseStock(1001L, 1L)).thenReturn(0);
        when(productService.increaseStock(1002L, 3L)).thenReturn(1);

        task.cancelOrder(order);

        verify(productService).increaseStock(1001L, 1L);
        verify(productService).increaseStock(1002L, 3L);
    }

    private static TOrder order(Long orderId, String orderNo, Date createTime)
    {
        TOrder order = new TOrder();
        order.setOrderId(orderId);
        order.setOrderNo(orderNo);
        order.setCreateTime(createTime);
        return order;
    }

    private static TOrderItem item(Long productId, Long quantity)
    {
        TOrderItem item = new TOrderItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private static Date minutesAgo(int minutes)
    {
        return new Date(System.currentTimeMillis() - minutes * 60L * 1000L);
    }
}
