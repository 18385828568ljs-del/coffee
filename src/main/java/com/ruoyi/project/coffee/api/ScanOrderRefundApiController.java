package com.ruoyi.project.coffee.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.common.annotation.RateLimit;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;
import com.ruoyi.project.coffee.scanOrder.service.IScanOrderRefundService;
import com.ruoyi.project.coffee.scanOrder.service.IScanOrderService;

import java.util.Map;

/**
 * 扫码点单退款API
 */
@RestController
@RequestMapping("/api/scanOrder/refund")
public class ScanOrderRefundApiController extends BaseController
{
    @Autowired
    private IScanOrderService scanOrderService;

    @Autowired
    private IScanOrderRefundService scanOrderRefundService;

    /**
     * 申请退款
     */
    @PostMapping("/apply")
    @RateLimit(key = "refund_apply", time = 60, count = 3, limitType = RateLimit.LimitType.USER)
    public AjaxResult applyRefund(@RequestBody Map<String, Object> params)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("请先登录");
        }

        Long orderId = parseLong(params.get("orderId"));
        String refundReason = parseString(params.get("refundReason"));

        if (orderId == null)
        {
            return AjaxResult.error("订单ID不能为空");
        }

        if (refundReason == null || refundReason.trim().isEmpty())
        {
            return AjaxResult.error("请填写退款原因");
        }

        try
        {
            boolean success = scanOrderRefundService.applyRefund(orderId, userId, refundReason);
            if (success)
            {
                return AjaxResult.success("退款申请已提交，请等待处理");
            }
            else
            {
                return AjaxResult.error("退款申请失败，请稍后重试");
            }
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 查询订单退款信息
     */
    @GetMapping("/{orderId}")
    public AjaxResult getRefundInfo(@PathVariable Long orderId)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("请先登录");
        }

        ScanOrder order = scanOrderService.selectScanOrderById(orderId);
        if (order == null || !userId.equals(order.getUserId()))
        {
            return AjaxResult.error("订单不存在");
        }

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("refundStatus", order.getRefundStatus());
        data.put("refundReason", order.getRefundReason());
        data.put("refundRejectReason", order.getRefundRejectReason());
        data.put("refundApplyTime", order.getRefundApplyTime());
        data.put("refundTime", order.getRefundTime());
        data.put("refundAmount", order.getRefundAmount());

        return AjaxResult.success(data);
    }

    private Long parseLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        try
        {
            return Long.parseLong(String.valueOf(value));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private String parseString(Object value)
    {
        if (value == null)
        {
            return null;
        }
        String str = String.valueOf(value).trim();
        return str.isEmpty() ? null : str;
    }
}
