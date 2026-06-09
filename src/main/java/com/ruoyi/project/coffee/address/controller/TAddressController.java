package com.ruoyi.project.coffee.address.controller;

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
import com.ruoyi.project.coffee.address.domain.TAddress;
import com.ruoyi.project.coffee.address.service.ITAddressService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 收货地址Controller
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Controller
@RequestMapping("/coffee/address")
public class TAddressController extends BaseController
{
    private String prefix = "coffee/address";

    @Autowired
    private ITAddressService tAddressService;

    @RequiresPermissions("coffee:address:view")
    @GetMapping()
    public String address()
    {
        return prefix + "/address";
    }

    /**
     * 查询收货地址列表
     */
    @RequiresPermissions("coffee:address:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TAddress tAddress)
    {
        startPage();
        List<TAddress> list = tAddressService.selectTAddressList(tAddress);
        return getDataTable(list);
    }

    /**
     * 导出收货地址列表
     */
    @RequiresPermissions("coffee:address:export")
    @Log(title = "收货地址", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(TAddress tAddress)
    {
        List<TAddress> list = tAddressService.selectTAddressList(tAddress);
        ExcelUtil<TAddress> util = new ExcelUtil<TAddress>(TAddress.class);
        return util.exportExcel(list, "收货地址数据");
    }

    /**
     * 新增收货地址
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存收货地址
     */
    @RequiresPermissions("coffee:address:add")
    @Log(title = "收货地址", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(TAddress tAddress)
    {
        return toAjax(tAddressService.insertTAddress(tAddress));
    }

    /**
     * 修改收货地址
     */
    @RequiresPermissions("coffee:address:edit")
    @GetMapping("/edit/{addressId}")
    public String edit(@PathVariable("addressId") Long addressId, ModelMap mmap)
    {
        TAddress tAddress = tAddressService.selectTAddressByAddressId(addressId);
        mmap.put("tAddress", tAddress);
        return prefix + "/edit";
    }

    /**
     * 修改保存收货地址
     */
    @RequiresPermissions("coffee:address:edit")
    @Log(title = "收货地址", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TAddress tAddress)
    {
        return toAjax(tAddressService.updateTAddress(tAddress));
    }

    /**
     * 删除收货地址
     */
    @RequiresPermissions("coffee:address:remove")
    @Log(title = "收货地址", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(tAddressService.deleteTAddressByAddressIds(ids));
    }
}
