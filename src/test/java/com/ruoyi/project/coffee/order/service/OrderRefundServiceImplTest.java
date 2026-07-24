package com.ruoyi.project.coffee.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.member.service.MemberService;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.order.mapper.TOrderMapper;
import com.ruoyi.project.coffee.order.service.impl.OrderRefundServiceImpl;
import com.ruoyi.project.coffee.payment.service.PaymentLogService;
import com.ruoyi.project.coffee.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class OrderRefundServiceImplTest
{
    private OrderRefundServiceImpl refundService;

    @Mock
    private TOrderMapper orderMapper;

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
        refundService = new OrderRefundServiceImpl();
        ReflectionTestUtils.setField(refundService, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(refundService, "walletService", walletService);
        ReflectionTestUtils.setField(refundService, "memberService", memberService);
        ReflectionTestUtils.setField(refundService, "paymentLogService", paymentLogService);
    }

    @Test
    void applyRefundShouldMarkPaidMallOrderAsPendingReview()
    {
        TOrder order = paidOrder();
        when(orderMapper.selectTOrderByOrderId(19L)).thenReturn(order);
        when(orderMapper.updateTOrder(order)).thenReturn(1);

        boolean success = refundService.applyRefund(19L, 7L, "不想要了");

        assertTrue(success);
        assertEquals(Integer.valueOf(1), order.getRefundStatus());
        assertEquals("不想要了", order.getRefundReason());
        ArgumentCaptor<TOrder> captor = ArgumentCaptor.forClass(TOrder.class);
        verify(orderMapper).updateTOrder(captor.capture());
        assertEquals(order, captor.getValue());
    }

    @Test
    void applyRefundShouldRejectOtherUsersAndUnpaidOrders()
    {
        TOrder order = paidOrder();
        when(orderMapper.selectTOrderByOrderId(19L)).thenReturn(order);

        ServiceException forbidden = assertThrows(ServiceException.class,
            () -> refundService.applyRefund(19L, 8L, "不想要了"));
        assertEquals("无权操作此订单", forbidden.getMessage());

        order.setUserId(8L);
        order.setStatus(0);
        ServiceException unpaid = assertThrows(ServiceException.class,
            () -> refundService.applyRefund(19L, 8L, "不想要了"));
        assertEquals("订单状态不支持退款", unpaid.getMessage());
        verify(orderMapper, never()).updateTOrder(order);
    }

    @Test
    void reviewRefundShouldRejectPendingRequest()
    {
        TOrder order = paidOrder();
        order.setRefundStatus(1);
        when(orderMapper.selectTOrderByOrderId(19L)).thenReturn(order);
        when(orderMapper.updateTOrder(order)).thenReturn(1);

        boolean success = refundService.reviewRefund(19L, false, "已发货无法退款");

        assertTrue(success);
        assertEquals(Integer.valueOf(3), order.getRefundStatus());
        assertEquals("已发货无法退款", order.getRefundRejectReason());
        verify(walletService, never()).refund(eq(7L), eq(new BigDecimal("26.60")), eq("MALL_ORDER_REFUND"),
            eq("CF001"), eq("商城订单退款"));
    }

    @Test
    void reviewRefundShouldExecuteBalanceRefundWhenApproved()
    {
        TOrder order = paidOrder();
        order.setRefundStatus(1);
        when(orderMapper.selectTOrderByOrderId(19L)).thenReturn(order);
        when(orderMapper.updateTOrder(order)).thenReturn(1);

        boolean success = refundService.reviewRefund(19L, true, null);

        assertTrue(success);
        verify(walletService).refund(7L, new BigDecimal("26.60"), "MALL_ORDER_REFUND", "CF001", "商城订单退款");
        verify(memberService).subtractSpending(7L, new BigDecimal("26.60"));
        verify(paymentLogService).recordRefund("MALL_ORDER", "CF001", 7L, "balance", "balance",
            new BigDecimal("26.60"), "SUCCESS");
        assertEquals(Integer.valueOf(2), order.getRefundStatus());
        assertEquals(new BigDecimal("26.60"), order.getRefundAmount());
    }

    @Test
    void executeRefundShouldBeIdempotentForRefundedOrder()
    {
        TOrder order = paidOrder();
        order.setRefundStatus(2);
        when(orderMapper.selectTOrderByOrderId(19L)).thenReturn(order);

        assertTrue(refundService.executeRefund(19L));

        verify(walletService, never()).refund(eq(7L), eq(new BigDecimal("26.60")), eq("MALL_ORDER_REFUND"),
            eq("CF001"), eq("商城订单退款"));
    }

    private TOrder paidOrder()
    {
        TOrder order = new TOrder();
        order.setOrderId(19L);
        order.setOrderNo("CF001");
        order.setUserId(7L);
        order.setStatus(1);
        order.setPayType("balance");
        order.setPayAmount(new BigDecimal("26.60"));
        order.setRefundStatus(0);
        return order;
    }
}
