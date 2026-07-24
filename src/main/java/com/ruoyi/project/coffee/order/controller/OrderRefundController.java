package com.ruoyi.project.coffee.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.order.service.IOrderRefundService;

/**
 * 商城订单退款Controller。
 */
@Controller
@RequestMapping("/coffee/order/refund")
public class OrderRefundController extends BaseController
{
    @Autowired
    private IOrderRefundService orderRefundService;

    @PostMapping("/review/{orderId}")
    @ResponseBody
    public AjaxResult reviewRefund(@PathVariable Long orderId,
                                   @RequestParam Boolean approved,
                                   @RequestParam(required = false) String rejectReason)
    {
        try
        {
            boolean success = orderRefundService.reviewRefund(orderId, approved, rejectReason);
            return success ? AjaxResult.success(approved ? "退款审核通过" : "已拒绝退款")
                : AjaxResult.error("操作失败，请刷新后重试");
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
        catch (Exception e)
        {
            logger.error("商城订单退款审核失败", e);
            return AjaxResult.error("系统异常，请联系管理员");
        }
    }

    @PostMapping("/execute/{orderId}")
    @ResponseBody
    public AjaxResult executeRefund(@PathVariable Long orderId)
    {
        try
        {
            boolean success = orderRefundService.executeRefund(orderId);
            return success ? AjaxResult.success("退款成功") : AjaxResult.error("退款失败，请稍后重试");
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
        catch (Exception e)
        {
            logger.error("商城订单执行退款失败", e);
            return AjaxResult.error("系统异常，请联系管理员");
        }
    }
}
