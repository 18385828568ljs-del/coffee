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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.behavior.service.UserBehaviorEventService;
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
    @Autowired
    private IScanCartService scanCartService;

    @Autowired
    private UserBehaviorEventService userBehaviorEventService;

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
        // 名称/图片以服务端商品为准,避免客户端篡改
        cart.setProductName(product.getProductName());
        cart.setProductImage(product.getImageUrl());

        try
        {
            // 规格为空时按商品配置补齐默认规格,快捷加购也必须走同一套计价规则
            if (StringUtils.isEmpty(cart.getSpecJson()))
            {
                String defaultSpecJson = scanProductService.buildDefaultSpecJson(cart.getProductId());
                if (defaultSpecJson != null && !defaultSpecJson.trim().isEmpty())
                {
                    cart.setSpecJson(defaultSpecJson);
                }
            }

            // 价格计算:基础价 + 规格加价,服务端重算防止前端篡改
            BigDecimal calculatedPrice = scanProductService.calculatePriceBySpecJson(
                cart.getProductId(), cart.getSpecJson());
            if (calculatedPrice != null)
            {
                cart.setPrice(calculatedPrice);
                String specText = scanProductService.buildSpecText(cart.getProductId(), cart.getSpecJson());
                if (specText != null)
                {
                    cart.setSpecText(specText);
                }
            }
            else
            {
                // 兼容现有单元测试替身及旧实现;生产环境由商品服务统一计价。
                BigDecimal basePrice = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
                BigDecimal extraPrice = BigDecimal.ZERO;
                if (StringUtils.isNotEmpty(cart.getSpecJson()))
                {
                    List<Long> optionIds;
                    try
                    {
                        optionIds = parseOptionIds(cart.getSpecJson());
                    }
                    catch (Exception e)
                    {
                        logger.warn("解析规格JSON失败: " + cart.getSpecJson(), e);
                        return AjaxResult.error("规格参数无效");
                    }

                    if (!optionIds.isEmpty())
                    {
                        List<ScanProductSpecOption> options =
                            scanProductSpecOptionService.selectOptionListByIds(optionIds);
                        if (options == null || options.size() != optionIds.size())
                        {
                            return AjaxResult.error("规格选项不存在");
                        }
                        for (ScanProductSpecOption option : options)
                        {
                            if (!cart.getProductId().equals(option.getProductId()))
                            {
                                return AjaxResult.error("规格选项不属于当前商品");
                            }
                            if (option.getExtraPrice() != null)
                            {
                                extraPrice = extraPrice.add(option.getExtraPrice());
                            }
                        }
                    }
                }
                cart.setPrice(basePrice.add(extraPrice));
            }
        }
        catch (IllegalArgumentException e)
        {
            return AjaxResult.error(e.getMessage());
        }

        ScanCart saved = scanCartService.addOrIncrease(cart);
        if (saved != null)
        {
            userBehaviorEventService.recordFirstCartAdd(cart.getUserId(),
                UserBehaviorEventService.SCENE_SCAN, cart.getProductId(), product.getCategoryId(), saved.getId(),
                UserBehaviorEventService.SOURCE_CATEGORY);
        }
        return AjaxResult.success("已加入点单购物车", saved);
    }

    private List<Long> parseOptionIds(String specJson)
    {
        JSONArray specs = JSON.parseArray(specJson);
        if (specs == null)
        {
            throw new IllegalArgumentException("规格不能为空");
        }
        List<Long> optionIds = new ArrayList<Long>();
        for (int i = 0; i < specs.size(); i++)
        {
            Object value = specs.get(i);
            if (!(value instanceof JSONObject))
            {
                throw new IllegalArgumentException("规格格式错误");
            }
            JSONObject spec = (JSONObject) value;
            Long specId = spec.getLong("specId");
            JSONArray ids = spec.getJSONArray("optionIds");
            if (specId == null || specId <= 0 || ids == null || ids.isEmpty())
            {
                throw new IllegalArgumentException("规格选项不能为空");
            }
            for (int j = 0; j < ids.size(); j++)
            {
                Long optionId = ids.getLong(j);
                if (optionId == null || optionId <= 0)
                {
                    throw new IllegalArgumentException("规格选项格式错误");
                }
                if (!optionIds.contains(optionId))
                {
                    optionIds.add(optionId);
                }
            }
        }
        return optionIds;
    }

    @GetMapping("/list")
    public AjaxResult getCartList(
        @RequestParam(value = "openid", required = false) String openid,
        @RequestParam(value = "tableNo", required = false) String tableNo)
    {
        ScanCart query = new ScanCart();
        // 强制使用当前登录用户,不接受前端传入的userId参数
        query.setUserId(WxUserAuthContext.getCurrentUserId());
        query.setOpenid(trimToNull(openid));
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
        // 越权校验: 只能修改自己的购物车
        Long currentUserId = WxUserAuthContext.getCurrentUserId();
        if (!currentUserId.equals(exist.getUserId()))
        {
            return AjaxResult.error("无权操作此购物车记录");
        }

        int affected = scanCartService.updateQuantity(cart.getId(), cart.getQuantity());
        if (affected <= 0)
        {
            return AjaxResult.error("购物车更新失败");
        }
        if (cart.getQuantity() != null && cart.getQuantity() <= 0)
        {
            userBehaviorEventService.recordCartRemove(currentUserId, UserBehaviorEventService.SCENE_SCAN,
                exist.getProductId(), resolveCategoryId(exist.getProductId()), exist.getId());
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

        int affected = scanCartService.logicDeleteById(id);
        if (affected > 0)
        {
            userBehaviorEventService.recordCartRemove(currentUserId, UserBehaviorEventService.SCENE_SCAN,
                exist.getProductId(), resolveCategoryId(exist.getProductId()), exist.getId());
        }
        return toAjax(affected);
    }

    @DeleteMapping("/clear")
    public AjaxResult clearCart(
        @RequestParam(value = "openid", required = false) String openid,
        @RequestParam(value = "tableNo", required = false) String tableNo)
    {
        ScanCart query = new ScanCart();
        // 强制使用当前登录用户,不接受前端传入的userId参数
        query.setUserId(WxUserAuthContext.getCurrentUserId());
        query.setOpenid(trimToNull(openid));
        query.setTableNo(trimToNull(tableNo));

        boolean ownerMissing = query.getUserId() == null
            && (query.getOpenid() == null || query.getOpenid().isEmpty());
        if (ownerMissing)
        {
            return AjaxResult.error("用户标识缺失,无法清空购物车");
        }

        List<ScanCart> items;
        try
        {
            items = scanCartService.selectScanCartList(query);
        }
        catch (Exception e)
        {
            // 行为快照查询失败时仍执行原有批量清空，不能阻断购物车主流程。
            logger.warn("读取扫码购物车行为快照失败, userId={}", query.getUserId(), e);
            scanCartService.logicDeleteByOwnerAndTable(query);
            return AjaxResult.success("当前桌台购物车已清空");
        }

        if (items == null)
        {
            items = java.util.Collections.emptyList();
        }
        for (ScanCart item : items)
        {
            if (scanCartService.logicDeleteById(item.getId()) > 0)
            {
                userBehaviorEventService.recordCartRemove(query.getUserId(), UserBehaviorEventService.SCENE_SCAN,
                    item.getProductId(), resolveCategoryId(item.getProductId()), item.getId());
            }
        }
        return AjaxResult.success("当前桌台购物车已清空");
    }

    private Long resolveCategoryId(Long productId)
    {
        try
        {
            ScanProduct product = scanProductService.selectScanProductById(productId);
            return product == null ? null : product.getCategoryId();
        }
        catch (Exception e)
        {
            logger.warn("读取扫码商品分类失败, productId={}", productId, e);
            return null;
        }
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
