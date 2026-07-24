package com.ruoyi.project.coffee.payment.controller;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.coffee.payment.domain.PaymentLog;
import com.ruoyi.project.coffee.payment.service.PaymentLogService;

/**
 * 支付日志 后台Controller
 */
@Controller
@RequestMapping("/coffee/paymentLog")
public class PaymentLogController extends BaseController
{
    @Autowired
    private PaymentLogService paymentLogService;

    @RequiresPermissions("coffee:paymentLog:view")
    @GetMapping()
    public String paymentLog()
    {
        return "coffee/paymentLog/paymentLog";
    }

    @RequiresPermissions("coffee:paymentLog:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(PaymentLog paymentLog)
    {
        startPage();
        List<PaymentLog> list = paymentLogService.selectPaymentLogList(paymentLog);
        return getDataTable(list);
    }
}
