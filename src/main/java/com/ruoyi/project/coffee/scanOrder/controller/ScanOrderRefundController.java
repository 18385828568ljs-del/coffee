package com.ruoyi.project.coffee.scanOrder.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.project.coffee.scanOrder.service.IScanOrderRefundService;

/**
 * 扫码订单退款Controller
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/coffee/scanOrder/refund")
public class ScanOrderRefundController extends BaseController
{
    @Autowired
    private IScanOrderRefundService scanOrderRefundService;

    /**
     * 审核退款
     *
     * @param orderId 订单ID
     * @param approved 是否同意
     * @param rejectReason 拒绝原因
     * @return 结果
     */
    @PostMapping("/review/{orderId}")
    @ResponseBody
    public AjaxResult reviewRefund(@PathVariable Long orderId,
                                   @RequestParam Boolean approved,
                                   @RequestParam(required = false) String rejectReason)
    {
        try
        {
            boolean success = scanOrderRefundService.reviewRefund(orderId, approved, rejectReason);
            if (success)
            {
                return AjaxResult.success(approved ? "退款审核通过" : "已拒绝退款");
            }
            else
            {
                return AjaxResult.error("操作失败，请刷新后重试");
            }
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
        catch (Exception e)
        {
            logger.error("退款审核失败", e);
            return AjaxResult.error("系统异常，请联系管理员");
        }
    }

    /**
     * 执行退款
     *
     * @param orderId 订单ID
     * @return 结果
     */
    @PostMapping("/execute/{orderId}")
    @ResponseBody
    public AjaxResult executeRefund(@PathVariable Long orderId)
    {
        try
        {
            boolean success = scanOrderRefundService.executeRefund(orderId);
            if (success)
            {
                return AjaxResult.success("退款成功");
            }
            else
            {
                return AjaxResult.error("退款失败，请稍后重试");
            }
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
        catch (Exception e)
        {
            logger.error("执行退款失败", e);
            return AjaxResult.error("系统异常，请联系管理员");
        }
    }
}
