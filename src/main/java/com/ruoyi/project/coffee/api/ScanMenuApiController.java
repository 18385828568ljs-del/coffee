package com.ruoyi.project.coffee.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCategory;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;
import com.ruoyi.project.coffee.scanOrder.service.IScanCategoryService;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;
import com.ruoyi.project.coffee.scanOrder.service.IScanTableQrcodeService;

/**
 * 小程序扫码点单菜单接口
 */
@RestController
@RequestMapping("/api/scanMenu")
public class ScanMenuApiController extends BaseController
{
    private static final long DEFAULT_SHOP_ID = 1L;

    @Autowired
    private IScanCategoryService scanCategoryService;

    @Autowired
    private IScanProductService scanProductService;

    @Autowired
    private IScanTableQrcodeService scanTableQrcodeService;

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
        @RequestParam(value = "shopId", required = false) Long shopId)
    {
        ScanProduct query = new ScanProduct();
        if (categoryId != null)
        {
            query.setCategoryId(categoryId);
        }
        query.setStatus(1);
        // shopId 当前商品表无字段;参数保留用于后续扩展,默认门店为 1
        List<ScanProduct> list = scanProductService.selectScanProductList(query);
        return AjaxResult.success(list);
    }

    @GetMapping("/products/{productId}")
    public AjaxResult getProductDetail(@PathVariable Long productId)
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
        return AjaxResult.success(product);
    }

    @GetMapping("/table/parse")
    public AjaxResult parseTable(
        @RequestParam(value = "shopId", required = false) Long shopId,
        @RequestParam("tableNo") String tableNo)
    {
        if (tableNo == null || tableNo.trim().isEmpty())
        {
            return AjaxResult.error("桌号不能为空");
        }
        Long resolvedShopId = shopId == null ? DEFAULT_SHOP_ID : shopId;
        ScanTableQrcode table = scanTableQrcodeService.selectByShopAndTable(resolvedShopId, tableNo);
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
        data.put("shopId", table.getShopId());
        data.put("shopName", table.getShopName());
        data.put("tableNo", table.getTableNo());
        data.put("scene", table.getScene());
        return AjaxResult.success(data);
    }
}
