package com.ruoyi.project.coffee.order.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.order.service.ITOrderService;
import com.ruoyi.project.coffee.order.util.OrderUtils;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import com.ruoyi.project.coffee.item.service.ITOrderItemService;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 订单Controller
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Controller
@RequestMapping("/coffee/order")
public class TOrderController extends BaseController
{
    private String prefix = "coffee/order";

    @Autowired
    private ITOrderService tOrderService;

    @Autowired
    private ITOrderItemService tOrderItemService;

    @Autowired
    private ITProductService tProductService;

    @RequiresPermissions("coffee:order:view")
    @GetMapping()
    public String order()
    {
        return prefix + "/order";
    }

    /**
     * 查询订单列表
     */
    @RequiresPermissions("coffee:order:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TOrder tOrder)
    {
        startPage();
        List<TOrder> list = tOrderService.selectTOrderList(tOrder);
        enrichOrders(list, false);
        return getDataTable(list);
    }

    /**
     * 导出订单列表
     */
    @RequiresPermissions("coffee:order:export")
    @Log(title = "订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(TOrder tOrder)
    {
        List<TOrder> list = tOrderService.selectTOrderList(tOrder);
        enrichOrders(list, false);
        ExcelUtil<TOrder> util = new ExcelUtil<TOrder>(TOrder.class);
        return util.exportExcel(list, "订单数据");
    }

    /**
     * 新增订单
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存订单
     */
    @RequiresPermissions("coffee:order:add")
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(TOrder tOrder)
    {
        return toAjax(tOrderService.insertTOrder(tOrder));
    }

    /**
     * 修改订单
     */
    @RequiresPermissions("coffee:order:edit")
    @GetMapping("/edit/{orderId}")
    public String edit(@PathVariable("orderId") Long orderId, ModelMap mmap)
    {
        TOrder tOrder = tOrderService.selectTOrderByOrderId(orderId);
        enrichOrder(tOrder, true);
        mmap.put("tOrder", tOrder);
        return prefix + "/edit";
    }

    /**
     * 发货订单
     */
    @RequiresPermissions("coffee:order:edit")
    @GetMapping("/ship/{orderId}")
    public String ship(@PathVariable("orderId") Long orderId, ModelMap mmap)
    {
        TOrder tOrder = tOrderService.selectTOrderByOrderId(orderId);
        enrichOrder(tOrder, true);
        mmap.put("tOrder", tOrder);
        return prefix + "/ship";
    }

    /**
     * 修改保存订单
     */
    @RequiresPermissions("coffee:order:edit")
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    @Transactional
    public AjaxResult editSave(TOrder tOrder)
    {
        TOrder oldOrder = tOrderService.selectTOrderByOrderId(tOrder.getOrderId());
        if (oldOrder == null)
        {
            return AjaxResult.error("订单不存在");
        }

        Integer newStatus = tOrder.getStatus();
        Integer oldStatus = oldOrder.getStatus();
        boolean statusChanged = newStatus != null && !newStatus.equals(oldStatus);

        if (statusChanged && newStatus == 2)
        {
            if (oldStatus == null || oldStatus != 1)
            {
                return AjaxResult.error("只能对待发货订单执行发货");
            }
            if (StringUtils.isEmpty(tOrder.getExpressNo()))
            {
                return AjaxResult.error("发货时物流单号不能为空");
            }
            if (tOrder.getShipTime() == null)
            {
                tOrder.setShipTime(DateUtils.getNowDate());
            }
            return toAjax(tOrderService.changeOrderStatus(oldOrder.getOrderId(), 1, 2, null,
                tOrder.getShipTime(), null, null, tOrder.getExpressNo(), tOrder.getRemark()));
        }

        if (statusChanged && newStatus == 3)
        {
            if (oldStatus == null || oldStatus != 2)
            {
                return AjaxResult.error("只能将已发货订单更新为已完成");
            }
            return toAjax(tOrderService.changeOrderStatus(oldOrder.getOrderId(), 2, 3, null, null,
                tOrder.getFinishTime() == null ? DateUtils.getNowDate() : tOrder.getFinishTime(), null,
                oldOrder.getExpressNo(), tOrder.getRemark()));
        }

        if (statusChanged && newStatus == 4)
        {
            if (oldStatus == null || oldStatus != 0)
            {
                return AjaxResult.error("只能取消待支付订单");
            }
            int updated = tOrderService.changeOrderStatus(oldOrder.getOrderId(), 0, 4, null, null, null,
                tOrder.getCancelTime() == null ? DateUtils.getNowDate() : tOrder.getCancelTime(),
                oldOrder.getExpressNo(), tOrder.getRemark());
            if (updated <= 0)
            {
                return AjaxResult.error("订单状态已变化，请刷新后重试");
            }
            rollbackOrderStock(oldOrder.getOrderId());
            return AjaxResult.success();
        }

        if (statusChanged && newStatus == 1)
        {
            if (oldStatus == null || oldStatus != 0)
            {
                return AjaxResult.error("只能将待支付订单更新为待发货");
            }
            return toAjax(tOrderService.changeOrderStatus(oldOrder.getOrderId(), 0, 1,
                tOrder.getPayTime() == null ? DateUtils.getNowDate() : tOrder.getPayTime(), null, null,
                null, oldOrder.getExpressNo(), tOrder.getRemark()));
        }

        if (statusChanged && newStatus == 0)
        {
            return AjaxResult.error("不支持手工回退订单状态");
        }

        return toAjax(tOrderService.updateTOrder(tOrder));
    }

    /**
     * 保存发货
     */
    @RequiresPermissions("coffee:order:edit")
    @Log(title = "订单发货", businessType = BusinessType.UPDATE)
    @PostMapping("/ship")
    @ResponseBody
    public AjaxResult shipSave(TOrder tOrder)
    {
        if (tOrder == null || tOrder.getOrderId() == null)
        {
            return AjaxResult.error("订单信息不能为空");
        }
        if (StringUtils.isEmpty(tOrder.getExpressNo()))
        {
            return AjaxResult.error("物流单号不能为空");
        }

        TOrder oldOrder = tOrderService.selectTOrderByOrderId(tOrder.getOrderId());
        if (oldOrder == null)
        {
            return AjaxResult.error("订单不存在");
        }
        if (oldOrder.getStatus() == null || oldOrder.getStatus() != 1)
        {
            return AjaxResult.error("只有待发货订单才允许发货");
        }

        TOrder shipOrder = new TOrder();
        shipOrder.setOrderId(oldOrder.getOrderId());
        shipOrder.setExpressNo(tOrder.getExpressNo().trim());
        shipOrder.setShipTime(DateUtils.getNowDate());
        shipOrder.setRemark(StringUtils.isEmpty(tOrder.getRemark()) ? oldOrder.getRemark() : tOrder.getRemark());
        int rows = tOrderService.shipOrder(shipOrder);
        if (rows <= 0)
        {
            return AjaxResult.error("订单状态已变化，请刷新后重试");
        }
        return toAjax(rows);
    }

    /**
     * 删除订单
     */
    @RequiresPermissions("coffee:order:remove")
    @Log(title = "订单", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(tOrderService.deleteTOrderByOrderIds(ids));
    }

    private void rollbackOrderStock(Long orderId)
    {
        TOrderItem query = new TOrderItem();
        query.setOrderId(orderId);
        List<TOrderItem> items = tOrderItemService.selectTOrderItemList(query);
        for (TOrderItem item : items)
        {
            if (tProductService.increaseStock(item.getProductId(), item.getQuantity()) <= 0)
            {
                throw new IllegalStateException("库存回补失败");
            }
        }
    }

    private void enrichOrders(List<TOrder> orders, boolean includeItems)
    {
        if (orders == null || orders.isEmpty())
        {
            return;
        }

        Map<Long, List<TOrderItem>> itemMap = OrderUtils.buildOrderItemsMap(orders, tOrderItemService);
        for (TOrder order : orders)
        {
            if (order == null || order.getOrderId() == null)
            {
                continue;
            }
            List<TOrderItem> items = itemMap.get(order.getOrderId());
            if (includeItems)
            {
                order.setOrderItems(items);
            }
            order.setProductSummary(buildProductSummary(items));
        }
    }

    private void enrichOrder(TOrder order, boolean includeItems)
    {
        if (order == null || order.getOrderId() == null)
        {
            return;
        }
        List<TOrderItem> items = OrderUtils.loadOrderItems(order.getOrderId(), tOrderItemService);
        if (includeItems)
        {
            order.setOrderItems(items);
        }
        order.setProductSummary(buildProductSummary(items));
    }

    private String buildProductSummary(List<TOrderItem> items)
    {
        if (items == null || items.isEmpty())
        {
            return "-";
        }

        StringBuilder builder = new StringBuilder();
        for (TOrderItem item : items)
        {
            if (item == null)
            {
                continue;
            }
            if (builder.length() > 0)
            {
                builder.append("，");
            }
            String productName = StringUtils.isNotEmpty(item.getProductName()) ? item.getProductName() : "未知商品";
            long quantity = item.getQuantity() == null ? 0L : item.getQuantity();
            builder.append(productName).append(" x").append(quantity);
        }
        return builder.length() > 0 ? builder.toString() : "-";
    }
}
