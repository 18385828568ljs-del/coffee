package com.ruoyi.project.coffee.scanOrder.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;
import com.ruoyi.project.coffee.scanOrder.service.IScanOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class ScanOrderTimeoutTaskTest
{
    private ScanOrderTimeoutTask task;

    @Mock
    private IScanOrderService scanOrderService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        task = new ScanOrderTimeoutTask();
        ReflectionTestUtils.setField(task, "scanOrderService", scanOrderService);
    }

    @Test
    void cancelTimeoutOrdersShouldOnlyCancelPendingScanOrdersOlderThanThirtyMinutes()
    {
        ScanOrder freshOrder = order(1L, "SO001", minutesAgo(10));
        ScanOrder missingCreateTime = order(2L, "SO002", null);
        ScanOrder timeoutOrder = order(3L, "SO003", minutesAgo(45));
        when(scanOrderService.selectScanOrderList(any(ScanOrder.class)))
            .thenReturn(Arrays.asList(freshOrder, missingCreateTime, timeoutOrder));
        when(scanOrderService.cancelOrder(3L)).thenReturn(1);

        task.cancelTimeoutOrders();

        verify(scanOrderService).cancelOrder(3L);
        verify(scanOrderService, never()).cancelOrder(1L);
        verify(scanOrderService, never()).cancelOrder(2L);
    }

    @Test
    void cancelTimeoutOrdersShouldReturnQuietlyWhenNoPendingScanOrder()
    {
        when(scanOrderService.selectScanOrderList(any(ScanOrder.class))).thenReturn(Collections.emptyList());

        task.cancelTimeoutOrders();

        verify(scanOrderService, never()).cancelOrder(any());
    }

    @Test
    void cancelTimeoutOrdersShouldContinueWhenOneScanOrderCancelFails()
    {
        ScanOrder failedOrder = order(8L, "SO008", minutesAgo(45));
        ScanOrder nextOrder = order(9L, "SO009", minutesAgo(60));
        when(scanOrderService.selectScanOrderList(any(ScanOrder.class)))
            .thenReturn(Arrays.asList(failedOrder, nextOrder));
        when(scanOrderService.cancelOrder(8L)).thenThrow(new IllegalStateException("状态已变更"));
        when(scanOrderService.cancelOrder(9L)).thenReturn(1);

        task.cancelTimeoutOrders();

        verify(scanOrderService).cancelOrder(8L);
        verify(scanOrderService).cancelOrder(9L);
    }

    private static ScanOrder order(Long orderId, String orderNo, Date createTime)
    {
        ScanOrder order = new ScanOrder();
        order.setOrderId(orderId);
        order.setOrderNo(orderNo);
        order.setCreateTime(createTime);
        return order;
    }

    private static Date minutesAgo(int minutes)
    {
        return new Date(System.currentTimeMillis() - minutes * 60L * 1000L);
    }
}
