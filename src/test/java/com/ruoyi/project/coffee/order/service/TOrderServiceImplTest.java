package com.ruoyi.project.coffee.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import com.ruoyi.project.coffee.item.mapper.TOrderItemMapper;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.order.mapper.TOrderMapper;
import com.ruoyi.project.coffee.order.service.impl.TOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class TOrderServiceImplTest
{
    private TOrderServiceImpl orderService;

    @Mock
    private TOrderMapper orderMapper;

    @Mock
    private TOrderItemMapper orderItemMapper;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        orderService = new TOrderServiceImpl();
        ReflectionTestUtils.setField(orderService, "tOrderMapper", orderMapper);
        ReflectionTestUtils.setField(orderService, "tOrderItemMapper", orderItemMapper);
    }

    @Test
    void insertOrderShouldFillCreateTimeBeforePersisting()
    {
        TOrder order = new TOrder();
        when(orderMapper.insertTOrder(order)).thenReturn(1);

        int rows = orderService.insertTOrder(order);

        assertEquals(1, rows);
        assertNotNull(order.getCreateTime());
        verify(orderMapper).insertTOrder(order);
    }

    @Test
    void shipOrderShouldOnlyMovePaidOrderToShipped()
    {
        Date shipTime = new Date();
        TOrder order = new TOrder();
        order.setOrderId(8L);
        order.setShipTime(shipTime);
        order.setExpressNo("SF1001");
        order.setRemark("冷链发货");
        when(orderMapper.updateOrderStatus(eq(8L), eq(1), eq(2), eq(null), eq(shipTime),
            eq(null), eq(null), eq("SF1001"), eq("冷链发货"))).thenReturn(1);

        int rows = orderService.shipOrder(order);

        assertEquals(1, rows);
        verify(orderMapper).updateOrderStatus(8L, 1, 2, null, shipTime, null, null,
            "SF1001", "冷链发货");
    }

    @Test
    void deleteOrdersShouldDeleteItemsBeforeOrders()
    {
        when(orderMapper.deleteTOrderByOrderIds(any())).thenReturn(2);

        int rows = orderService.deleteTOrderByOrderIds("1,2");

        assertEquals(2, rows);
        verify(orderItemMapper).deleteTOrderItemByOrderIds(new String[] {"1", "2"});
        verify(orderMapper).deleteTOrderByOrderIds(new String[] {"1", "2"});
    }

    @Test
    void deleteSingleOrderShouldDeleteItemsBeforeOrder()
    {
        when(orderMapper.deleteTOrderByOrderId(9L)).thenReturn(1);

        int rows = orderService.deleteTOrderByOrderId(9L);

        assertEquals(1, rows);
        verify(orderItemMapper).deleteTOrderItemByOrderId(9L);
        verify(orderMapper).deleteTOrderByOrderId(9L);
    }
}
