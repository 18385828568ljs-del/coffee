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
import com.ruoyi.project.coffee.scanOrder.service.IScanCategoryService;

@Controller
@RequestMapping("/coffee/scanCategory")
public class ScanCategoryController extends BaseController
{
    private String prefix = "coffee/scanCategory";

    @Autowired
    private IScanCategoryService scanCategoryService;

    @RequiresPermissions("coffee:scanCategory:view")
    @GetMapping()
    public String scanCategory()
    {
        return prefix + "/scanCategory";
    }

    @RequiresPermissions("coffee:scanCategory:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ScanCategory scanCategory)
    {
        startPage();
        List<ScanCategory> list = scanCategoryService.selectScanCategoryList(scanCategory);
        return getDataTable(list);
    }

    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @RequiresPermissions("coffee:scanCategory:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(ScanCategory scanCategory)
    {
        scanCategory.setCreateBy(getLoginName());
        return toAjax(scanCategoryService.insertScanCategory(scanCategory));
    }

    @RequiresPermissions("coffee:scanCategory:edit")
    @GetMapping("/edit/{categoryId}")
    public String edit(@PathVariable("categoryId") Long categoryId, ModelMap mmap)
    {
        mmap.put("scanCategory", scanCategoryService.selectScanCategoryById(categoryId));
        return prefix + "/edit";
    }

    @RequiresPermissions("coffee:scanCategory:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(ScanCategory scanCategory)
    {
        scanCategory.setUpdateBy(getLoginName());
        return toAjax(scanCategoryService.updateScanCategory(scanCategory));
    }

    @RequiresPermissions("coffee:scanCategory:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(scanCategoryService.deleteScanCategoryByIds(ids));
    }
}
