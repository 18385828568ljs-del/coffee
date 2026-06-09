package com.ruoyi.project.coffee.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;
import com.ruoyi.project.coffee.scanOrder.service.IScanCartService;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductSpecOptionService;

/**
 * 小程序扫码点单购物车接口
 *
 * 与商城购物车 t_cart 物理隔离,数据库表为 t_scan_cart。
 */
@RestController
@RequestMapping("/api/scanCart")
public class ScanCartApiController extends BaseController
{
    private static final long DEFAULT_SHOP_ID = 1L;

    @Autowired
    private IScanCartService scanCartService;

    @Autowired
    private IScanProductService scanProductService;

    @Autowired
    private IScanProductSpecOptionService scanProductSpecOptionService;

    @PostMapping("/add")
    public AjaxResult addToCart(@RequestBody ScanCart cart)
    {
        if (cart == null)
        {
            return AjaxResult.error("请求参数不能为空");
        }
        if (cart.getProductId() == null)
        {
            return AjaxResult.error("商品ID不能为空");
        }
        if (cart.getQuantity() == null || cart.getQuantity() < 1)
        {
            cart.setQuantity(1);
        }

        // 校验商品有效性
        ScanProduct product = scanProductService.selectScanProductById(cart.getProductId());
        if (product == null)
        {
            return AjaxResult.error("商品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != 1)
        {
            return AjaxResult.error("商品已下架");
        }

        // 用户标识:强制从鉴权上下文获取,不信任前端传入
        cart.setUserId(WxUserAuthContext.getCurrentUserId());
        normalizeOwner(cart);
        if (cart.getShopId() == null)
        {
            cart.setShopId(DEFAULT_SHOP_ID);
        }
        // 名称/图片以服务端商品为准,避免客户端篡改
        cart.setProductName(product.getProductName());
        cart.setProductImage(product.getImageUrl());

        // 价格计算:基础价 + 规格加价,服务端重算防止前端篡改
        BigDecimal basePrice = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        BigDecimal extraPrice = BigDecimal.ZERO;

        if (StringUtils.isNotEmpty(cart.getSpecJson()))
        {
            try
            {
                // 解析前端传入的规格选项ID数组
                List<Long> optionIds = JSON.parseArray(cart.getSpecJson(), Long.class);
                if (optionIds != null && !optionIds.isEmpty())
                {
                    List<ScanProductSpecOption> options =
                        scanProductSpecOptionService.selectOptionListByIds(optionIds);
                    for (ScanProductSpecOption option : options)
                    {
                        if (option.getExtraPrice() != null)
                        {
                            extraPrice = extraPrice.add(option.getExtraPrice());
                        }
                    }
                }
            }
            catch (Exception e)
            {
                // specJson 格式错误,忽略规格加价
                logger.warn("解析规格JSON失败: " + cart.getSpecJson(), e);
            }
        }

        cart.setPrice(basePrice.add(extraPrice));

        ScanCart saved = scanCartService.addOrIncrease(cart);
        return AjaxResult.success("已加入点单购物车", saved);
    }

    @GetMapping("/list")
    public AjaxResult getCartList(
        @RequestParam(value = "openid", required = false) String openid,
        @RequestParam(value = "shopId", required = false) Long shopId,
        @RequestParam(value = "tableNo", required = false) String tableNo)
    {
        ScanCart query = new ScanCart();
        // 强制使用当前登录用户,不接受前端传入的userId参数
        query.setUserId(WxUserAuthContext.getCurrentUserId());
        query.setOpenid(trimToNull(openid));
        query.setShopId(shopId == null ? DEFAULT_SHOP_ID : shopId);
        query.setTableNo(trimToNull(tableNo));
        query.setStatus(1);

        boolean ownerMissing = query.getUserId() == null
            && (query.getOpenid() == null || query.getOpenid().isEmpty());
        if (ownerMissing && (query.getTableNo() == null || query.getTableNo().isEmpty()))
        {
            // 完全无身份且无桌号时,直接返回空结果而不是全表扫描
            return AjaxResult.success(buildSummary(java.util.Collections.<ScanCart>emptyList()));
        }

        List<ScanCart> list = scanCartService.selectScanCartList(query);
        return AjaxResult.success(buildSummary(list));
    }

    @PutMapping("/update")
    public AjaxResult updateCart(@RequestBody ScanCart cart)
    {
        if (cart == null || cart.getId() == null)
        {
            return AjaxResult.error("购物车ID不能为空");
        }
        ScanCart exist = scanCartService.selectScanCartById(cart.getId());
        if (exist == null || (exist.getDelFlag() != null && exist.getDelFlag() == 1))
        {
            return AjaxResult.error("购物车记录不存在");
        }

        int affected = scanCartService.updateQuantity(cart.getId(), cart.getQuantity());
        if (affected <= 0)
        {
            return AjaxResult.error("购物车更新失败");
        }
        return AjaxResult.success("已更新");
    }

    @DeleteMapping("/{id}")
    public AjaxResult deleteCart(@PathVariable Long id)
    {
        if (id == null)
        {
            return AjaxResult.error("购物车ID不能为空");
        }
        ScanCart exist = scanCartService.selectScanCartById(id);
        if (exist == null || (exist.getDelFlag() != null && exist.getDelFlag() == 1))
        {
            return AjaxResult.error("购物车记录不存在");
        }

        // 越权校验:只能删除自己的购物车
        Long currentUserId = WxUserAuthContext.getCurrentUserId();
        if (!currentUserId.equals(exist.getUserId()))
        {
            return AjaxResult.error("无权操作此购物车记录");
        }

        return toAjax(scanCartService.logicDeleteById(id));
    }

    @DeleteMapping("/clear")
    public AjaxResult clearCart(
        @RequestParam(value = "openid", required = false) String openid,
        @RequestParam(value = "shopId", required = false) Long shopId,
        @RequestParam(value = "tableNo", required = false) String tableNo)
    {
        ScanCart query = new ScanCart();
        // 强制使用当前登录用户,不接受前端传入的userId参数
        query.setUserId(WxUserAuthContext.getCurrentUserId());
        query.setOpenid(trimToNull(openid));
        query.setShopId(shopId == null ? DEFAULT_SHOP_ID : shopId);
        query.setTableNo(trimToNull(tableNo));

        boolean ownerMissing = query.getUserId() == null
            && (query.getOpenid() == null || query.getOpenid().isEmpty());
        if (ownerMissing)
        {
            return AjaxResult.error("用户标识缺失,无法清空购物车");
        }

        scanCartService.logicDeleteByOwnerAndTable(query);
        return AjaxResult.success("当前桌台购物车已清空");
    }

    private void normalizeOwner(ScanCart cart)
    {
        if (cart.getOpenid() != null)
        {
            String trimmed = cart.getOpenid().trim();
            cart.setOpenid(trimmed.isEmpty() ? null : trimmed);
        }
        if (cart.getTableNo() != null)
        {
            String trimmed = cart.getTableNo().trim();
            cart.setTableNo(trimmed.isEmpty() ? null : trimmed);
        }
    }

    private String trimToNull(String value)
    {
        if (value == null)
        {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> buildSummary(List<ScanCart> list)
    {
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (list != null)
        {
            for (ScanCart item : list)
            {
                int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                totalQuantity += qty;
                BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
                totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(qty)));
            }
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list == null ? java.util.Collections.<ScanCart>emptyList() : list);
        data.put("totalQuantity", totalQuantity);
        data.put("totalAmount", totalAmount);
        return data;
    }
}
