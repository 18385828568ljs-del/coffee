package com.ruoyi.project.coffee.product.controller;

import java.util.Arrays;
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
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 商品Controller
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Controller
@RequestMapping("/coffee/product")
public class TProductController extends BaseController
{
    private String prefix = "coffee/product";

    private static final List<String> ORIGIN_OPTIONS = Arrays.asList(
            "肯尼亚", "埃塞俄比亚", "卢旺达", "坦桑尼亚", "乌干达", "布隆迪",
            "哥伦比亚", "巴拿马", "巴西", "哥斯达黎加", "危地马拉", "洪都拉斯", "萨尔瓦多", "尼加拉瓜", "秘鲁", "墨西哥",
            "印尼", "云南", "巴布亚新几内亚", "印度", "越南", "东帝汶",
            "牙买加", "夏威夷", "也门", "多产区");

    private static final List<String> PROCESSING_METHOD_OPTIONS = Arrays.asList(
            "水洗", "日晒", "蜜处理", "半水洗", "湿刨法", "厌氧发酵", "厌氧日晒", "厌氧水洗", "酒桶发酵", "双重发酵", "拼配");

    private static final List<String> ROAST_LEVEL_OPTIONS = Arrays.asList(
            "极浅焙", "浅焙", "中浅焙", "中焙", "中深焙", "深焙", "极深焙");

    private static final List<String> FLAVOR_OPTIONS = Arrays.asList(
            "花香、柑橘、茶感", "茉莉花、蜂蜜、热带水果", "莓果、葡萄、红酒",
            "柑橘、花香、茉莉", "坚果、焦糖、巧克力", "巧克力、坚果、焦糖",
            "黑莓、番茄、红酒", "可可、奶油、甜感", "苹果、梨、蜂蜜", "香料、木质、醇厚");

    @Autowired
    private ITProductService tProductService;

    @Autowired
    private ITCategoryService tCategoryService;

    @RequiresPermissions("coffee:product:view")
    @GetMapping()
    public String product(ModelMap mmap)
    {
        mmap.put("categories", getCategories());
        setProductOptions(mmap);
        return prefix + "/product";
    }

    /**
     * 查询商品列表
     */
    @RequiresPermissions("coffee:product:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TProduct tProduct)
    {
        startPage();
        List<TProduct> list = tProductService.selectTProductList(tProduct);
        return getDataTable(list);
    }

    /**
     * 导出商品列表
     */
    @RequiresPermissions("coffee:product:export")
    @Log(title = "商品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(TProduct tProduct)
    {
        List<TProduct> list = tProductService.selectTProductList(tProduct);
        ExcelUtil<TProduct> util = new ExcelUtil<TProduct>(TProduct.class);
        return util.exportExcel(list, "商品数据");
    }

    /**
     * 新增商品
     */
    @GetMapping("/add")
    public String add(ModelMap mmap)
    {
        mmap.put("categories", getCategories());
        setProductOptions(mmap);
        return prefix + "/add";
    }

    /**
     * 新增保存商品
     */
    @RequiresPermissions("coffee:product:add")
    @Log(title = "商品", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(TProduct tProduct)
    {
        return toAjax(tProductService.insertTProduct(tProduct));
    }

    /**
     * 修改商品
     */
    @RequiresPermissions("coffee:product:edit")
    @GetMapping("/edit/{productId}")
    public String edit(@PathVariable("productId") Long productId, ModelMap mmap)
    {
        TProduct tProduct = tProductService.selectTProductByProductId(productId);
        mmap.put("tProduct", tProduct);
        mmap.put("categories", getCategories());
        setProductOptions(mmap);
        return prefix + "/edit";
    }

    /**
     * 修改保存商品
     */
    @RequiresPermissions("coffee:product:edit")
    @Log(title = "商品", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TProduct tProduct)
    {
        return toAjax(tProductService.updateTProduct(tProduct));
    }

    /**
     * 删除商品
     */
    @RequiresPermissions("coffee:product:remove")
    @Log(title = "商品", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(tProductService.deleteTProductByProductIds(ids));
    }

    private List<TCategory> getCategories()
    {
        return tCategoryService.selectTCategoryList(new TCategory());
    }

    private void setProductOptions(ModelMap mmap)
    {
        mmap.put("originOptions", ORIGIN_OPTIONS);
        mmap.put("processingMethodOptions", PROCESSING_METHOD_OPTIONS);
        mmap.put("roastLevelOptions", ROAST_LEVEL_OPTIONS);
        mmap.put("flavorOptions", FLAVOR_OPTIONS);
    }
}
