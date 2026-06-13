package com.ruoyi.project.coffee.scanOrder.controller;

import java.util.List;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;
import com.ruoyi.project.coffee.scanOrder.service.IScanOrderService;

@Controller
@RequestMapping("/coffee/scanOrder")
public class ScanOrderController extends BaseController
{
    private String prefix = "coffee/scanOrder";

    @Autowired
    private IScanOrderService scanOrderService;

    @RequiresPermissions("coffee:scanOrder:view")
    @GetMapping()
    public String scanOrder()
    {
        return prefix + "/scanOrder";
    }

    @RequiresPermissions("coffee:scanOrder:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ScanOrder scanOrder)
    {
        startPage();
        List<ScanOrder> list = scanOrderService.selectScanOrderList(scanOrder);
        return getDataTable(list);
    }

    @RequiresPermissions(value = { "coffee:scanOrder:detail", "coffee:scanOrder:view" }, logical = Logical.OR)
    @GetMapping("/detail/{orderId}")
    public String detail(@PathVariable("orderId") Long orderId, ModelMap mmap)
    {
        mmap.put("scanOrder", scanOrderService.selectScanOrderWithItems(orderId));
        return prefix + "/detail";
    }

    @RequiresPermissions("coffee:scanOrder:edit")
    @PostMapping("/call")
    @ResponseBody
    public AjaxResult call(Long orderId)
    {
        return toAjax(scanOrderService.callOrder(orderId));
    }

    @RequiresPermissions("coffee:scanOrder:edit")
    @PostMapping("/complete")
    @ResponseBody
    public AjaxResult complete(Long orderId)
    {
        return toAjax(scanOrderService.completeOrder(orderId));
    }

    @RequiresPermissions("coffee:scanOrder:edit")
    @PostMapping("/cancelPaid")
    @ResponseBody
    public AjaxResult cancelPaid(Long orderId)
    {
        return toAjax(scanOrderService.cancelPaidOrder(orderId));
    }
}
