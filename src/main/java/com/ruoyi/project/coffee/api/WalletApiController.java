package com.ruoyi.project.coffee.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

    /**
     * 创建充值订单
     *
     * 充值金额必须来自后台维护的充值模板,不接受前端任意指定 payAmount。
     * 这样可以避免前端伪造金额导致 t_recharge_record 数据失真。
     */
    @PostMapping("/recharge")
    public AjaxResult recharge(@RequestBody Map<String, Object> params)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("未登录");
        }

        if (params == null || params.get("templateId") == null)
        {
            return AjaxResult.error("请选择充值套餐");
        }

        Long templateId;
        try
        {
            templateId = Long.valueOf(params.get("templateId").toString());
        }
        catch (NumberFormatException e)
        {
            return AjaxResult.error("充值套餐参数错误");
        }

        try
        {
            // payAmount 在 service 内会被模板金额覆盖,这里传 ZERO 即可
            TRechargeRecord record = walletService.createRecharge(userId, templateId, BigDecimal.ZERO);
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("rechargeNo", record.getRechargeNo());
            result.put("payAmount", record.getPayAmount());
            return AjaxResult.success(result);
        }
        catch (Exception e)
        {
            return AjaxResult.error("创建充值订单失败：" + e.getMessage());
        }
    }

    /**
     * 充值回调确认（仅供开发测试用，生产环境必须禁用）
     *
     * 警告：此接口可被任意登录用户调用实现无限刷钱，仅用于开发阶段测试充值流程。
     * 生产环境必须：
     * 1. 删除此接口或用 @Profile("dev") 严格隔离
     * 2. 真实支付由微信支付异步回调(带签名验签)驱动入账
     */
    @PostMapping("/recharge/callback")
    public AjaxResult rechargeCallback(@RequestBody Map<String, Object> params)
    {
        // 开发阶段警告提示
        logger.warn("充值回调接口被调用(开发测试用),生产环境必须禁用此接口");

        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("未登录");
        }
        String rechargeNo = params != null && params.get("rechargeNo") != null
            ? params.get("rechargeNo").toString() : null;
        if (rechargeNo == null || rechargeNo.isEmpty())
        {
            return AjaxResult.error("充值单号不能为空");
        }

        try
        {
            walletService.confirmRecharge(rechargeNo, userId);
            return AjaxResult.success("充值入账成功(测试模式)");
        }
        catch (Exception e)
        {
            return AjaxResult.error("充值入账失败：" + e.getMessage());
        }
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
