package com.ruoyi.project.coffee.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.auth.WxUserTokenService;
import com.ruoyi.project.coffee.behavior.service.UserBehaviorEventService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCategory;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;
import com.ruoyi.project.coffee.scanOrder.service.IScanCategoryService;
import com.ruoyi.project.coffee.scanOrder.service.IScanCartService;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;
import com.ruoyi.project.coffee.scanOrder.service.IScanTableQrcodeService;
import com.ruoyi.project.coffee.profile.service.ProductRecommendationService;
import com.ruoyi.project.coffee.profile.domain.RecommendationIntent;
import com.ruoyi.project.coffee.profile.service.ProfileTagUtils;

/**
 * 小程序扫码点单菜单接口
 */
@RestController
@RequestMapping("/api/scanMenu")
public class ScanMenuApiController extends BaseController
{
    @Autowired
    private IScanCategoryService scanCategoryService;

    @Autowired
    private IScanProductService scanProductService;

    @Autowired
    private IScanCartService scanCartService;

    @Autowired
    private IScanTableQrcodeService scanTableQrcodeService;

    @Autowired
    private WxUserTokenService wxUserTokenService;

    @Autowired
    private UserBehaviorEventService userBehaviorEventService;

    @Autowired
    private ProductRecommendationService productRecommendationService;

    @GetMapping("/categories")
    public AjaxResult getCategoryList()
    {
        ScanCategory query = new ScanCategory();
        query.setStatus(1);
        List<ScanCategory> list = scanCategoryService.selectScanCategoryList(query);
        return AjaxResult.success(list);
    }

    @GetMapping("/products")
    public AjaxResult getProductList(
        @RequestParam(value = "categoryId", required = false) Long categoryId,
        HttpServletRequest request)
    {
        ScanProduct query = new ScanProduct();
        if (categoryId != null)
        {
            query.setCategoryId(categoryId);
        }
        query.setStatus(1);
        List<ScanProduct> list = scanProductService.selectScanProductList(query);
        Long userId = wxUserTokenService.resolveUserId(request);
        RecommendationIntent intent = buildIntent(userId, request);
        list = intent.hasSignals()
            ? productRecommendationService.recommendScan(userId, intent, list)
            : productRecommendationService.recommendScan(userId, list);
        return AjaxResult.success(list);
    }

    private RecommendationIntent buildIntent(Long userId, HttpServletRequest request)
    {
        RecommendationIntent intent = new RecommendationIntent();
        addRequestProduct(intent, request, "currentProductId", true);
        if (userId == null || scanCartService == null)
        {
            return intent;
        }
        try
        {
            ScanCart query = new ScanCart();
            query.setUserId(userId);
            query.setTableNo(request == null ? null : request.getParameter("tableNo"));
            query.setStatus(1);
            List<ScanCart> cartItems = scanCartService.selectScanCartList(query);
            if (cartItems != null)
            {
                for (ScanCart item : cartItems)
                {
                    if (item != null)
                    {
                        intent.addCartProduct(item.getProductId());
                        ProfileTagUtils.selectedScanSpecs(item.getSpecJson())
                            .forEach(tag -> intent.addTag(tag.getKey(), 6D));
                    }
                }
            }
        }
        catch (RuntimeException ignored) { }
        return intent;
    }

    private void addRequestProduct(RecommendationIntent intent, HttpServletRequest request,
        String parameter, boolean current)
    {
        Long productId = parseLongParameter(request, parameter);
        if (productId != null)
        {
            if (current) intent.addCurrentProduct(productId);
            else intent.addProduct(productId, 1D);
        }
    }

    private Long parseLongParameter(HttpServletRequest request, String parameter)
    {
        if (request == null || request.getParameter(parameter) == null) return null;
        try
        {
            Long value = Long.valueOf(request.getParameter(parameter));
            return value > 0 ? value : null;
        }
        catch (NumberFormatException ignored)
        {
            return null;
        }
    }

    @GetMapping("/products/{productId}")
    public AjaxResult getProductDetail(@PathVariable Long productId, HttpServletRequest request)
    {
        if (productId == null)
        {
            return AjaxResult.error("商品ID不能为空");
        }
        ScanProduct product = scanProductService.selectScanProductWithSpecs(productId);
        if (product == null)
        {
            return AjaxResult.error("商品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != 1)
        {
            return AjaxResult.error("商品已下架");
        }
        Long userId = wxUserTokenService.resolveUserId(request);
        userBehaviorEventService.recordProductView(userId, UserBehaviorEventService.SCENE_SCAN,
            product.getProductId(), product.getCategoryId(), UserBehaviorEventService.SOURCE_CATEGORY);
        return AjaxResult.success(product);
    }

    @GetMapping("/table/parse")
    public AjaxResult parseTable(@RequestParam("tableNo") String tableNo)
    {
        if (tableNo == null || tableNo.trim().isEmpty())
        {
            return AjaxResult.error("桌号不能为空");
        }
        ScanTableQrcode table = scanTableQrcodeService.selectByTableNo(tableNo);
        if (table == null)
        {
            return AjaxResult.error("桌台不存在或已停用");
        }
        if (table.getStatus() == null || table.getStatus() != 1)
        {
            return AjaxResult.error("桌台已停用");
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("tableId", table.getTableId());
        data.put("tableNo", table.getTableNo());
        data.put("scene", table.getScene());
        return AjaxResult.success(data);
    }
}
