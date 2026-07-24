package com.ruoyi.project.coffee.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.wallet.domain.TWallet;
import com.ruoyi.project.coffee.wallet.domain.TWalletLog;
import com.ruoyi.project.coffee.wallet.mapper.TWalletMapper;

class WalletServiceTest
{
    @Mock
    private TWalletMapper walletMapper;

    private WalletService walletService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        walletService = new WalletService();
        ReflectionTestUtils.setField(walletService, "walletMapper", walletMapper);
    }

    @Test
    void getOrCreateWalletShouldInitializeZeroWalletWhenMissing()
    {
        when(walletMapper.selectWalletByUserId(7L)).thenReturn(null);

        TWallet wallet = walletService.getOrCreateWallet(7L);

        assertEquals(7L, wallet.getUserId());
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getTotalRecharge()));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getTotalGift()));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getTotalConsumed()));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getFrozenAmount()));
        verify(walletMapper).insertWallet(wallet);
    }

    @Test
    void getOrCreateWalletShouldReturnExistingWalletWithoutInsert()
    {
        TWallet existing = wallet(7L, "18.50", "100.00", "10.00");
        when(walletMapper.selectWalletByUserId(7L)).thenReturn(existing);

        TWallet wallet = walletService.getOrCreateWallet(7L);

        assertSame(existing, wallet);
        verify(walletMapper, never()).insertWallet(any(TWallet.class));
    }

    @Test
    void deductForOrderShouldRejectInsufficientBalanceBeforeAtomicDeduct()
    {
        when(walletMapper.selectWalletByUserId(7L)).thenReturn(wallet(7L, "5.00", "0.00", "0.00"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> walletService.deductForOrder(7L, bd("10.00"), "SO001"));

        assertEquals("余额不足", exception.getMessage());
        verify(walletMapper, never()).deductBalance(any(Long.class), any(BigDecimal.class));
        verify(walletMapper, never()).insertWalletLog(any(TWalletLog.class));
    }

    @Test
    void deductForOrderShouldRejectConcurrentDeductFailureWithoutLog()
    {
        when(walletMapper.selectWalletByUserId(7L)).thenReturn(wallet(7L, "50.00", "0.00", "0.00"));
        when(walletMapper.deductBalance(7L, bd("12.50"))).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> walletService.deductForOrder(7L, bd("12.50"), "SO001"));

        assertEquals("余额不足（并发扣款失败）", exception.getMessage());
        verify(walletMapper, never()).insertWalletLog(any(TWalletLog.class));
    }

    @Test
    void deductForOrderShouldWriteNegativeConsumptionLogAfterAtomicDeduct()
    {
        when(walletMapper.selectWalletByUserId(7L)).thenReturn(wallet(7L, "50.00", "0.00", "0.00"));
        when(walletMapper.deductBalance(7L, bd("12.50"))).thenReturn(1);

        walletService.deductForOrder(7L, bd("12.50"), "SO001");

        verify(walletMapper).deductBalance(7L, bd("12.50"));
        ArgumentCaptor<TWalletLog> logCaptor = ArgumentCaptor.forClass(TWalletLog.class);
        verify(walletMapper).insertWalletLog(logCaptor.capture());
        assertWalletLog(logCaptor.getValue(), 7L, 2, "-12.50", "50.00", "37.50", "SO001", "订单支付");
    }

    private static TWallet wallet(Long userId, String balance, String totalRecharge, String totalGift)
    {
        TWallet wallet = new TWallet();
        wallet.setUserId(userId);
        wallet.setBalance(bd(balance));
        wallet.setTotalRecharge(bd(totalRecharge));
        wallet.setTotalGift(bd(totalGift));
        wallet.setTotalConsumed(BigDecimal.ZERO);
        wallet.setFrozenAmount(BigDecimal.ZERO);
        return wallet;
    }

    private static void assertWalletLog(TWalletLog log, Long userId, Integer type, String amount, String before,
            String after, String relatedOrderNo, String remark)
    {
        assertEquals(userId, log.getUserId());
        assertEquals(type, log.getType());
        assertEquals(0, bd(amount).compareTo(log.getAmount()));
        assertEquals(0, bd(before).compareTo(log.getBalanceBefore()));
        assertEquals(0, bd(after).compareTo(log.getBalanceAfter()));
        assertEquals(relatedOrderNo, log.getRelatedOrderNo());
        assertEquals(remark, log.getRemark());
    }

    private static BigDecimal bd(String value)
    {
        return new BigDecimal(value);
    }
}
