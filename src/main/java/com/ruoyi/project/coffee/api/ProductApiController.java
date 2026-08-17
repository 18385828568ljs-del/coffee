package com.ruoyi.project.coffee.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.auth.WxUserTokenService;
import com.ruoyi.project.coffee.behavior.service.UserBehaviorEventService;
import com.ruoyi.project.coffee.category.domain.TCategory;
import com.ruoyi.project.coffee.category.service.ITCategoryService;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.profile.service.ProductRecommendationService;

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

    @Autowired
    private UserBehaviorEventService userBehaviorEventService;

    @Autowired
    private ProductRecommendationService productRecommendationService;

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
        Long userId = wxUserTokenService.resolveUserId(request);
        List<TProduct> candidates = productService.selectTProductList(product);
        List<TProduct> ranked = productRecommendationService.recommendMall(userId, candidates);
        List<TProduct> page = paginate(ranked, request);
        marketingActivityEngine.enrichProducts(page, userId);
        return toTableDataInfo(page, ranked.size());
    }

    @GetMapping("/category/{categoryId}")
    public TableDataInfo getProductsByCategory(@PathVariable Long categoryId, HttpServletRequest request)
    {
        TProduct query = new TProduct();
        query.setCategoryId(categoryId);
        query.setStatus(1);
        Long userId = wxUserTokenService.resolveUserId(request);
        List<TProduct> candidates = productService.selectTProductList(query);
        List<TProduct> ranked = productRecommendationService.recommendMall(userId, candidates);
        List<TProduct> page = paginate(ranked, request);
        marketingActivityEngine.enrichProducts(page, userId);
        return toTableDataInfo(page, ranked.size());
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
        Long userId = wxUserTokenService.resolveUserId(request);
        marketingActivityEngine.enrichProduct(product, userId);
        userBehaviorEventService.recordProductView(userId, UserBehaviorEventService.SCENE_MALL,
            product.getProductId(), product.getCategoryId(), request.getParameter("source"));
        return AjaxResult.success(product);
    }

    private <T> List<T> paginate(List<T> list, HttpServletRequest request)
    {
        List<T> values = list == null ? Collections.emptyList() : list;
        int pageNum = positiveOrDefault(parsePositiveParameter(request, "pageNum"), 1);
        int pageSize = positiveOrDefault(parsePositiveParameter(request, "pageSize"), 10);
        long fromLong = ((long) pageNum - 1L) * pageSize;
        if (fromLong >= values.size())
        {
            return Collections.emptyList();
        }
        int from = (int) fromLong;
        int to = Math.min(values.size(), from + pageSize);
        return new ArrayList<>(values.subList(from, to));
    }

    private int positiveOrDefault(Integer value, int defaultValue)
    {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private Integer parsePositiveParameter(HttpServletRequest request, String name)
    {
        if (request == null || request.getParameter(name) == null)
        {
            return null;
        }
        try
        {
            return Integer.valueOf(request.getParameter(name));
        }
        catch (NumberFormatException ignored)
        {
            return null;
        }
    }

    private TableDataInfo toTableDataInfo(List<?> rows, int total)
    {
        TableDataInfo result = new TableDataInfo();
        result.setCode(0);
        result.setRows(rows);
        result.setTotal(total);
        return result;
    }
}
