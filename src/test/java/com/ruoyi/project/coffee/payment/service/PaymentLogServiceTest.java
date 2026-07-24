package com.ruoyi.project.coffee.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import com.ruoyi.project.coffee.payment.domain.PaymentLog;
import com.ruoyi.project.coffee.payment.mapper.PaymentLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentLogServiceTest
{
    private PaymentLogService paymentLogService;

    @Mock
    private PaymentLogMapper paymentLogMapper;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        paymentLogService = new PaymentLogService();
        ReflectionTestUtils.setField(paymentLogService, "paymentLogMapper", paymentLogMapper);
    }

    @Test
    void recordBalancePaidShouldInsertBalanceAuditLog()
    {
        paymentLogService.recordBalancePaid("SCAN_ORDER", "SO001", 7L, new BigDecimal("9.50"));

        ArgumentCaptor<PaymentLog> captor = ArgumentCaptor.forClass(PaymentLog.class);
        verify(paymentLogMapper).insertPaymentLog(captor.capture());
        PaymentLog log = captor.getValue();
        assertEquals("balance", log.getPayType());
        assertEquals("balance", log.getPayChannel());
        assertEquals("PAID", log.getStatus());
    }

    @Test
    void recordRefundShouldInsertRefundAuditLog()
    {
        paymentLogService.recordRefund("SCAN_ORDER", "SO001", 7L, "balance", "balance",
            new BigDecimal("9.50"), "SUCCESS");

        ArgumentCaptor<PaymentLog> captor = ArgumentCaptor.forClass(PaymentLog.class);
        verify(paymentLogMapper).insertPaymentLog(captor.capture());
        assertEquals("REFUND_SUCCESS", captor.getValue().getStatus());
    }

    @Test
    void selectPaymentLogListShouldDelegateToMapper()
    {
        PaymentLog query = new PaymentLog();
        query.setBusinessType("SCAN_ORDER");
        when(paymentLogMapper.selectPaymentLogList(query)).thenReturn(Collections.singletonList(query));

        List<PaymentLog> rows = paymentLogService.selectPaymentLogList(query);

        assertEquals(1, rows.size());
        verify(paymentLogMapper).selectPaymentLogList(query);
    }
}
