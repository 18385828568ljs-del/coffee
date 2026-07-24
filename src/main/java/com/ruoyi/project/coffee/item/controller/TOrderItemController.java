package com.ruoyi.project.coffee.item.controller;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import com.ruoyi.project.coffee.item.service.ITOrderItemService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 订单明细Controller
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Controller
@RequestMapping("/coffee/item")
public class TOrderItemController extends BaseController
{
    private String prefix = "coffee/item";

    @Autowired
    private ITOrderItemService tOrderItemService;

    @RequiresPermissions("coffee:item:view")
    @GetMapping()
    public String item()
    {
        return prefix + "/item";
    }

    /**
     * 查询订单明细列表
     */
    @RequiresPermissions("coffee:item:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TOrderItem tOrderItem)
    {
        startPage();
        List<TOrderItem> list = tOrderItemService.selectTOrderItemList(tOrderItem);
        return getDataTable(list);
    }

    /**
     * 导出订单明细列表
     */
    @RequiresPermissions("coffee:item:export")
    @Log(title = "订单明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(TOrderItem tOrderItem)
    {
        List<TOrderItem> list = tOrderItemService.selectTOrderItemList(tOrderItem);
        ExcelUtil<TOrderItem> util = new ExcelUtil<TOrderItem>(TOrderItem.class);
        return util.exportExcel(list, "订单明细数据");
    }

    /**
     * 新增订单明细
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存订单明细
     */
    @RequiresPermissions("coffee:item:add")
    @Log(title = "订单明细", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(TOrderItem tOrderItem)
    {
        return toAjax(tOrderItemService.insertTOrderItem(tOrderItem));
    }

    /**
     * 修改订单明细
     */
    @RequiresPermissions("coffee:item:edit")
    @GetMapping("/edit/{itemId}")
    public String edit(@PathVariable("itemId") Long itemId, ModelMap mmap)
    {
        TOrderItem tOrderItem = tOrderItemService.selectTOrderItemByItemId(itemId);
        mmap.put("tOrderItem", tOrderItem);
        return prefix + "/edit";
    }

    /**
     * 修改保存订单明细
     */
    @RequiresPermissions("coffee:item:edit")
    @Log(title = "订单明细", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TOrderItem tOrderItem)
    {
        return toAjax(tOrderItemService.updateTOrderItem(tOrderItem));
    }

    /**
     * 删除订单明细
     */
    @RequiresPermissions("coffee:item:remove")
    @Log(title = "订单明细", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(tOrderItemService.deleteTOrderItemByItemIds(ids));
    }
}
