package com.ruoyi.project.coffee.api;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.common.annotation.RateLimit;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.order.service.IOrderRefundService;
import com.ruoyi.project.coffee.order.service.ITOrderService;

/**
 * 商城订单退款API。
 */
@RestController
@RequestMapping("/api/order/refund")
public class OrderRefundApiController extends BaseController
{
    @Autowired
    private ITOrderService orderService;

    @Autowired
    private IOrderRefundService orderRefundService;

    @PostMapping("/apply")
    @RateLimit(key = "mall_refund_apply", time = 60, count = 3, limitType = RateLimit.LimitType.USER)
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
        if (refundReason == null)
        {
            return AjaxResult.error("请填写退款原因");
        }

        try
        {
            return orderRefundService.applyRefund(orderId, userId, refundReason)
                ? AjaxResult.success("退款申请已提交，请等待处理")
                : AjaxResult.error("退款申请失败，请稍后重试");
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("/{orderId}")
    public AjaxResult getRefundInfo(@PathVariable Long orderId)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("请先登录");
        }

        TOrder order = orderService.selectTOrderByOrderId(orderId);
        if (order == null || !userId.equals(order.getUserId()))
        {
            return AjaxResult.error("订单不存在");
        }

        Map<String, Object> data = new HashMap<>();
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
