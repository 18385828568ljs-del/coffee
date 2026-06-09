package com.ruoyi.project.coffee.cart.controller;

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
import com.ruoyi.project.coffee.cart.domain.TCart;
import com.ruoyi.project.coffee.cart.service.ITCartService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 购物车Controller
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Controller
@RequestMapping("/coffee/cart")
public class TCartController extends BaseController
{
    private String prefix = "coffee/cart";

    @Autowired
    private ITCartService tCartService;

    @RequiresPermissions("coffee:cart:view")
    @GetMapping()
    public String cart()
    {
        return prefix + "/cart";
    }

    /**
     * 查询购物车列表
     */
    @RequiresPermissions("coffee:cart:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TCart tCart)
    {
        startPage();
        List<TCart> list = tCartService.selectTCartList(tCart);
        return getDataTable(list);
    }

    /**
     * 导出购物车列表
     */
    @RequiresPermissions("coffee:cart:export")
    @Log(title = "购物车", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(TCart tCart)
    {
        List<TCart> list = tCartService.selectTCartList(tCart);
        ExcelUtil<TCart> util = new ExcelUtil<TCart>(TCart.class);
        return util.exportExcel(list, "购物车数据");
    }

    /**
     * 新增购物车
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存购物车
     */
    @RequiresPermissions("coffee:cart:add")
    @Log(title = "购物车", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(TCart tCart)
    {
        return toAjax(tCartService.insertTCart(tCart));
    }

    /**
     * 修改购物车
     */
    @RequiresPermissions("coffee:cart:edit")
    @GetMapping("/edit/{cartId}")
    public String edit(@PathVariable("cartId") Long cartId, ModelMap mmap)
    {
        TCart tCart = tCartService.selectTCartByCartId(cartId);
        mmap.put("tCart", tCart);
        return prefix + "/edit";
    }

    /**
     * 修改保存购物车
     */
    @RequiresPermissions("coffee:cart:edit")
    @Log(title = "购物车", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TCart tCart)
    {
        return toAjax(tCartService.updateTCart(tCart));
    }

    /**
     * 删除购物车
     */
    @RequiresPermissions("coffee:cart:remove")
    @Log(title = "购物车", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(tCartService.deleteTCartByCartIds(ids));
    }
}
