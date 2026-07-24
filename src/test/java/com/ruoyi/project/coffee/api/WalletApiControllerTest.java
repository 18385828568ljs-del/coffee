package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.wallet.domain.TWallet;
import com.ruoyi.project.coffee.wallet.domain.TWalletLog;
import com.ruoyi.project.coffee.wallet.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class WalletApiControllerTest
{
    private WalletApiController controller;

    @Mock
    private WalletService walletService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new WalletApiController();
        ReflectionTestUtils.setField(controller, "walletService", walletService);
    }

    @AfterEach
    void tearDown()
    {
        WxUserAuthContext.clear();
    }

    @Test
    void infoRequiresLogin()
    {
        AjaxResult result = controller.info();

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("未登录", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void infoReturnsWallet()
    {
        bindUser(6L);
        TWallet wallet = new TWallet();
        wallet.setUserId(6L);
        wallet.setBalance(new BigDecimal("88.00"));
        when(walletService.getOrCreateWallet(6L)).thenReturn(wallet);

        AjaxResult result = controller.info();

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(wallet, result.get(AjaxResult.DATA_TAG));
    }

    @Test
    void logPaginatesInMemoryRows()
    {
        bindUser(6L);
        TWalletLog first = log(1L);
        TWalletLog second = log(2L);
        TWalletLog third = log(3L);
        when(walletService.getWalletLogs(6L)).thenReturn(Arrays.asList(first, second, third));

        AjaxResult result = controller.log(2, 2);

        List<?> data = (List<?>) result.get(AjaxResult.DATA_TAG);
        assertEquals(1, data.size());
        assertSame(third, data.get(0));
    }

    @Test
    void rechargeRequiresLogin()
    {
        AjaxResult result = controller.recharge();

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("未登录", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void rechargeIsNotAvailable()
    {
        bindUser(6L);

        AjaxResult result = controller.recharge();

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("在线充值暂未开放", result.get(AjaxResult.MSG_TAG));
    }

    private TWalletLog log(Long id)
    {
        TWalletLog log = new TWalletLog();
        log.setLogId(id);
        return log;
    }

    private void bindUser(Long userId)
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(userId);
        user.setOpenid("openid-" + userId);
        WxUserAuthContext.setCurrentUser(user);
    }
}
