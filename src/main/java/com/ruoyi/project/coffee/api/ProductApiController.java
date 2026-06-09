package com.ruoyi.project.coffee.api;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.auth.WxUserTokenService;
import com.ruoyi.project.coffee.category.domain.TCategory;
import com.ruoyi.project.coffee.category.service.ITCategoryService;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.common.annotation.RateLimit;

/**
 * Mini-program product APIs.
 */
@RestController
@RequestMapping("/api/product")
public class ProductApiController extends BaseController
{
    @Autowired
    private ITProductService productService;

    @Autowired
    private ITCategoryService categoryService;

    @Autowired
    private MarketingActivityEngine marketingActivityEngine;

    @Autowired
    private WxUserTokenService wxUserTokenService;

    @GetMapping("/categories")
    public AjaxResult getCategoryList()
    {
        TCategory query = new TCategory();
        List<TCategory> list = categoryService.selectTCategoryList(query);
        return AjaxResult.success(list);
    }

    @GetMapping("/list")
    public TableDataInfo getProductList(TProduct product, HttpServletRequest request)
    {
        product.setStatus(1);
        startPage();
        List<TProduct> list = productService.selectTProductList(product);
        marketingActivityEngine.enrichProducts(list, wxUserTokenService.resolveUserId(request));
        return getDataTable(list);
    }

    @GetMapping("/category/{categoryId}")
    public TableDataInfo getProductsByCategory(@PathVariable Long categoryId, HttpServletRequest request)
    {
        TProduct query = new TProduct();
        query.setCategoryId(categoryId);
        query.setStatus(1);
        startPage();
        List<TProduct> list = productService.selectTProductList(query);
        marketingActivityEngine.enrichProducts(list, wxUserTokenService.resolveUserId(request));
        return getDataTable(list);
    }

    @GetMapping("/{productId}")
    public AjaxResult getProductDetail(@PathVariable Long productId, HttpServletRequest request)
    {
        TProduct product = productService.selectTProductByProductId(productId);
        if (product == null || product.getStatus() != 1)
        {
            return AjaxResult.error("Product not found");
        }
        if (product.getStock() == null || product.getStock() <= 0)
        {
            product.setStock(0L);
        }
        marketingActivityEngine.enrichProduct(product, wxUserTokenService.resolveUserId(request));
        return AjaxResult.success(product);
    }

    @GetMapping("/search")
    @RateLimit(key = "product_search", time = 60, count = 30, limitType = RateLimit.LimitType.IP)
    public TableDataInfo searchProducts(@RequestParam String keyword, HttpServletRequest request)
    {
        if (keyword == null || keyword.trim().isEmpty())
        {
            return getDataTable(java.util.Collections.emptyList());
        }

        keyword = keyword.trim();
        if (keyword.length() > 50)
        {
            keyword = keyword.substring(0, 50);
        }

        TProduct query = new TProduct();
        query.setProductName(keyword);
        query.setStatus(1);
        startPage();
        List<TProduct> list = productService.selectTProductList(query);
        marketingActivityEngine.enrichProducts(list, wxUserTokenService.resolveUserId(request));
        return getDataTable(list);
    }
}
