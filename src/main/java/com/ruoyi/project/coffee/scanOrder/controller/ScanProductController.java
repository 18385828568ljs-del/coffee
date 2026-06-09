package com.ruoyi.project.coffee.scanOrder.controller;

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
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCategory;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.service.IScanCategoryService;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;

@Controller
@RequestMapping("/coffee/scanProduct")
public class ScanProductController extends BaseController
{
    private String prefix = "coffee/scanProduct";

    @Autowired
    private IScanProductService scanProductService;

    @Autowired
    private IScanCategoryService scanCategoryService;

    @RequiresPermissions("coffee:scanProduct:view")
    @GetMapping()
    public String scanProduct(ModelMap mmap)
    {
        mmap.put("categories", getCategories());
        return prefix + "/scanProduct";
    }

    @RequiresPermissions("coffee:scanProduct:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ScanProduct scanProduct)
    {
        startPage();
        List<ScanProduct> list = scanProductService.selectScanProductList(scanProduct);
        return getDataTable(list);
    }

    @GetMapping("/add")
    public String add(ModelMap mmap)
    {
        mmap.put("categories", getCategories());
        return prefix + "/add";
    }

    @RequiresPermissions("coffee:scanProduct:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(ScanProduct scanProduct)
    {
        scanProduct.setCreateBy(getLoginName());
        return toAjax(scanProductService.insertScanProduct(scanProduct));
    }

    @RequiresPermissions("coffee:scanProduct:edit")
    @GetMapping("/edit/{productId}")
    public String edit(@PathVariable("productId") Long productId, ModelMap mmap)
    {
        mmap.put("scanProduct", scanProductService.selectScanProductById(productId));
        mmap.put("categories", getCategories());
        return prefix + "/edit";
    }

    @RequiresPermissions("coffee:scanProduct:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(ScanProduct scanProduct)
    {
        scanProduct.setUpdateBy(getLoginName());
        return toAjax(scanProductService.updateScanProduct(scanProduct));
    }

    @RequiresPermissions("coffee:scanProduct:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(scanProductService.deleteScanProductByIds(ids));
    }

    private List<ScanCategory> getCategories()
    {
        return scanCategoryService.selectScanCategoryList(new ScanCategory());
    }
}
