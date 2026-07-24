package com.ruoyi.project.coffee.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.wallet.domain.TWallet;
import com.ruoyi.project.coffee.wallet.domain.TWalletLog;
import com.ruoyi.project.coffee.wallet.domain.TRechargeRecord;
import com.ruoyi.project.coffee.wallet.domain.TRechargeTemplate;
import com.ruoyi.project.coffee.wallet.service.WalletService;

/**
 * 小程序钱包/充值接口
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletApiController extends BaseController
{
    @Autowired
    private WalletService walletService;

    /**
     * 获取钱包信息
     */
    @GetMapping("/info")
    public AjaxResult info()
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("未登录");
        }
        TWallet wallet = walletService.getOrCreateWallet(userId);
        return AjaxResult.success(wallet);
    }

    /**
     * 获取余额流水
     */
    @GetMapping("/log")
    public AjaxResult log(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "20") int pageSize)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("未登录");
        }
        // 简单分页：全量查询后截取（数据量不大时足够）
        List<TWalletLog> all = walletService.getWalletLogs(userId);
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        List<TWalletLog> rows = start < all.size() ? all.subList(start, end) : java.util.Collections.emptyList();
        return AjaxResult.success(rows);
    }

    /**
     * 获取充值模板
     */
    @GetMapping("/recharge/templates")
    public AjaxResult rechargeTemplates()
    {
        List<TRechargeTemplate> list = walletService.getActiveTemplates();
        return AjaxResult.success(list);
    }

    /** 在线充值暂未开放。 */
    @PostMapping("/recharge")
    public AjaxResult recharge()
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("未登录");
        }

        return AjaxResult.error("在线充值暂未开放");
    }

    /**
     * 充值记录列表
     */
    @GetMapping("/recharge/records")
    public AjaxResult rechargeRecords()
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("未登录");
        }
        List<TRechargeRecord> list = walletService.getRechargeRecords(userId);
        return AjaxResult.success(list);
    }

}
