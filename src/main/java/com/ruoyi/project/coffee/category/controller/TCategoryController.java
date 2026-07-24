package com.ruoyi.project.coffee.category.controller;

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
import com.ruoyi.project.coffee.category.domain.TCategory;
import com.ruoyi.project.coffee.category.service.ITCategoryService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 商品分类Controller
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Controller
@RequestMapping("/coffee/category")
public class TCategoryController extends BaseController
{
    private String prefix = "coffee/category";

    @Autowired
    private ITCategoryService tCategoryService;

    @RequiresPermissions("coffee:category:view")
    @GetMapping()
    public String category()
    {
        return prefix + "/category";
    }

    /**
     * 查询商品分类列表
     */
    @RequiresPermissions("coffee:category:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TCategory tCategory)
    {
        startPage();
        List<TCategory> list = tCategoryService.selectTCategoryList(tCategory);
        return getDataTable(list);
    }

    /**
     * 导出商品分类列表
     */
    @RequiresPermissions("coffee:category:export")
    @Log(title = "商品分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(TCategory tCategory)
    {
        List<TCategory> list = tCategoryService.selectTCategoryList(tCategory);
        ExcelUtil<TCategory> util = new ExcelUtil<TCategory>(TCategory.class);
        return util.exportExcel(list, "商品分类数据");
    }

    /**
     * 新增商品分类
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存商品分类
     */
    @RequiresPermissions("coffee:category:add")
    @Log(title = "商品分类", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(TCategory tCategory)
    {
        return toAjax(tCategoryService.insertTCategory(tCategory));
    }

    /**
     * 修改商品分类
     */
    @RequiresPermissions("coffee:category:edit")
    @GetMapping("/edit/{categoryId}")
    public String edit(@PathVariable("categoryId") Long categoryId, ModelMap mmap)
    {
        TCategory tCategory = tCategoryService.selectTCategoryByCategoryId(categoryId);
        mmap.put("tCategory", tCategory);
        return prefix + "/edit";
    }

    /**
     * 修改保存商品分类
     */
    @RequiresPermissions("coffee:category:edit")
    @Log(title = "商品分类", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TCategory tCategory)
    {
        return toAjax(tCategoryService.updateTCategory(tCategory));
    }

    /**
     * 删除商品分类
     */
    @RequiresPermissions("coffee:category:remove")
    @Log(title = "商品分类", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(tCategoryService.deleteTCategoryByCategoryIds(ids));
    }
}
