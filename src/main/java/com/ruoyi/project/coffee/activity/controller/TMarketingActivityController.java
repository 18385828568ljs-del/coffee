package com.ruoyi.project.coffee.activity.controller;

import java.math.BigDecimal;
import java.util.Collections;
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
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.coffee.activity.domain.TMarketingActivity;
import com.ruoyi.project.coffee.activity.service.ITMarketingActivityService;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.activity.util.MarketingActivityScopeUtils;
import com.ruoyi.project.coffee.category.domain.TCategory;
import com.ruoyi.project.coffee.category.service.ITCategoryService;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCategory;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.service.IScanCategoryService;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;

/**
 * 营销活动 Controller
 */
@Controller
@RequestMapping("/coffee/activity")
public class TMarketingActivityController extends BaseController
{
    private String prefix = "coffee/activity";

    @Autowired
    private ITMarketingActivityService tMarketingActivityService;

    @Autowired
    private ITCategoryService tCategoryService;

    @Autowired
    private ITProductService tProductService;

    @Autowired
    private IScanCategoryService scanCategoryService;

    @Autowired
    private IScanProductService scanProductService;

    @RequiresPermissions("coffee:activity:view")
    @GetMapping()
    public String activity()
    {
        return prefix + "/activity";
    }

    @RequiresPermissions("coffee:activity:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TMarketingActivity tMarketingActivity)
    {
        startPage();
        List<TMarketingActivity> list = tMarketingActivityService.selectTMarketingActivityList(tMarketingActivity);
        return getDataTable(list);
    }

    @RequiresPermissions("coffee:activity:export")
    @Log(title = "营销活动", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(TMarketingActivity tMarketingActivity)
    {
        List<TMarketingActivity> list = tMarketingActivityService.selectTMarketingActivityList(tMarketingActivity);
        ExcelUtil<TMarketingActivity> util = new ExcelUtil<TMarketingActivity>(TMarketingActivity.class);
        return util.exportExcel(list, "营销活动数据");
    }

    @GetMapping("/add")
    public String add(ModelMap mmap)
    {
        loadActivityFormOptions(mmap);
        return prefix + "/add";
    }

    @RequiresPermissions("coffee:activity:add")
    @Log(title = "营销活动", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(TMarketingActivity tMarketingActivity)
    {
        try
        {
            normalizeMarketingActivityConfig(tMarketingActivity);
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
        AjaxResult validateResult = validateMarketingActivityConfig(tMarketingActivity);
        if (isErrorResult(validateResult))
        {
            return validateResult;
        }
        return toAjax(tMarketingActivityService.insertTMarketingActivity(tMarketingActivity));
    }

    @RequiresPermissions("coffee:activity:edit")
    @GetMapping("/edit/{activityId}")
    public String edit(@PathVariable("activityId") Long activityId, ModelMap mmap)
    {
        TMarketingActivity tMarketingActivity = tMarketingActivityService.selectTMarketingActivityByActivityId(activityId);
        fillMarketingActivityFormFields(tMarketingActivity);
        loadActivityFormOptions(mmap);
        mmap.put("tMarketingActivity", tMarketingActivity);
        return prefix + "/edit";
    }

    @RequiresPermissions("coffee:activity:edit")
    @Log(title = "营销活动", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TMarketingActivity tMarketingActivity)
    {
        try
        {
            normalizeMarketingActivityConfig(tMarketingActivity);
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
        AjaxResult validateResult = validateMarketingActivityConfig(tMarketingActivity);
        if (isErrorResult(validateResult))
        {
            return validateResult;
        }
        return toAjax(tMarketingActivityService.updateTMarketingActivity(tMarketingActivity));
    }

    @RequiresPermissions("coffee:activity:remove")
    @Log(title = "营销活动", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(tMarketingActivityService.deleteTMarketingActivityByActivityIds(ids));
    }

    private AjaxResult validateMarketingActivityConfig(TMarketingActivity activity)
    {
        if (activity == null)
        {
            return AjaxResult.error("活动信息不能为空");
        }
        if (activity.getType() == null)
        {
            return AjaxResult.error("请选择活动分类");
        }
        if (activity.getScopeType() == null)
        {
            return AjaxResult.error("请选择适用范围");
        }
        // 适用场景: mall / scan / both, 缺省按 mall 兼容历史数据
        String targetType = activity.getTargetType();
        if (StringUtils.isEmpty(targetType))
        {
            targetType = MarketingActivityEngine.TARGET_MALL;
            activity.setTargetType(targetType);
        }
        if (!MarketingActivityEngine.TARGET_MALL.equals(targetType)
            && !MarketingActivityEngine.TARGET_SCAN.equals(targetType)
            && !MarketingActivityEngine.TARGET_BOTH.equals(targetType))
        {
            return AjaxResult.error("适用场景参数错误");
        }
        // both 场景下 mall/scan 商品 ID 互不相通,只允许 scope_type=0 (全场)
        if (MarketingActivityEngine.TARGET_BOTH.equals(targetType) && activity.getScopeType() != 0)
        {
            return AjaxResult.error("「商城+点单」场景仅支持作用于全部商品,如需限定商品请改为单一场景");
        }
        // 包邮活动只对 mall 有意义,堂食无运费
        if ("free_shipping".equals(activity.getEffectMode())
            && !MarketingActivityEngine.TARGET_MALL.equals(targetType))
        {
            return AjaxResult.error("包邮活动只能用于商城场景");
        }
        if (activity.getStatus() == null)
        {
            return AjaxResult.error("请选择活动状态");
        }
        if (activity.getStartTime() != null && activity.getEndTime() != null
            && activity.getStartTime().after(activity.getEndTime()))
        {
            return AjaxResult.error("结束时间不能早于开始时间");
        }
        if (activity.getScopeType() != null && activity.getScopeType() != 0 && StringUtils.isEmpty(activity.getScopeIdsText()))
        {
            return AjaxResult.error("请选择适用的分类或商品");
        }
        if (StringUtils.isEmpty(activity.getEffectMode()))
        {
            return AjaxResult.error("请选择优惠方式");
        }
        if (!"free_shipping".equals(activity.getEffectMode())
            && !"gift".equals(activity.getEffectMode())
            && (activity.getEffectValue() == null || activity.getEffectValue().compareTo(BigDecimal.ZERO) <= 0))
        {
            return AjaxResult.error("请填写有效的优惠值");
        }
        if ("discount".equals(activity.getEffectMode())
            && activity.getEffectValue() != null
            && activity.getEffectValue().compareTo(BigDecimal.ONE) >= 0)
        {
            return AjaxResult.error("折扣活动的优惠值必须小于 1");
        }
        if ("gift".equals(activity.getEffectMode()))
        {
            if (activity.getGiftProductId() == null || activity.getGiftProductId() <= 0)
            {
                return AjaxResult.error("请选择赠品商品");
            }
            if (activity.getGiftQuantity() == null || activity.getGiftQuantity() < 1)
            {
                return AjaxResult.error("请填写有效的赠品数量");
            }
        }
        return AjaxResult.success();
    }

    private void loadActivityFormOptions(ModelMap mmap)
    {
        // mall 商品/分类
        List<TCategory> categoryList = tCategoryService.selectTCategoryList(new TCategory());
        TProduct productQuery = new TProduct();
        productQuery.setStatus(1);
        List<TProduct> productList = tProductService.selectTProductList(productQuery);
        mmap.put("categoryList", categoryList == null ? Collections.emptyList() : categoryList);
        mmap.put("productList", productList == null ? Collections.emptyList() : productList);

        // scan 商品/分类(点单使用)
        ScanCategory scanCategoryQuery = new ScanCategory();
        scanCategoryQuery.setStatus(1);
        List<ScanCategory> scanCategoryList = scanCategoryService.selectScanCategoryList(scanCategoryQuery);
        ScanProduct scanProductQuery = new ScanProduct();
        scanProductQuery.setStatus(1);
        List<ScanProduct> scanProductList = scanProductService.selectScanProductList(scanProductQuery);
        mmap.put("scanCategoryList", scanCategoryList == null ? Collections.emptyList() : scanCategoryList);
        mmap.put("scanProductList", scanProductList == null ? Collections.emptyList() : scanProductList);
    }

    private void fillMarketingActivityFormFields(TMarketingActivity activity)
    {
        if (activity == null)
        {
            return;
        }
        activity.setScopeIdsText(MarketingActivityScopeUtils.joinScopeIds(activity.getScopeIds()));
        if (activity.getConditionMinQuantity() == null || activity.getConditionMinQuantity() < 1)
        {
            activity.setConditionMinQuantity(1L);
        }
        if (activity.getConditionNewUserOnly() == null)
        {
            activity.setConditionNewUserOnly(0);
        }
        if (activity.getEffectValue() == null)
        {
            activity.setEffectValue(BigDecimal.ZERO);
        }
        if (activity.getGiftQuantity() == null || activity.getGiftQuantity() < 1)
        {
            activity.setGiftQuantity(1L);
        }
        if (activity.getShippingBaseFreight() == null)
        {
            activity.setShippingBaseFreight(BigDecimal.ZERO);
        }
        activity.setEffectMode(normalizeMode(activity.getEffectMode(), activity.getType()));
    }

    private void normalizeMarketingActivityConfig(TMarketingActivity activity)
    {
        if (activity == null)
        {
            return;
        }

        List<Long> scopeIds = activity.getScopeType() == null || activity.getScopeType() == 0
            ? Collections.<Long>emptyList()
            : MarketingActivityScopeUtils.parseScopeIds(activity.getScopeIdsText());
        activity.setScopeIds(scopeIds);
        activity.setScopeIdsText(MarketingActivityScopeUtils.joinScopeIds(scopeIds));

        BigDecimal minAmount = activity.getConditionMinAmount() == null ? BigDecimal.ZERO : activity.getConditionMinAmount();
        Long minQuantity = activity.getConditionMinQuantity() == null || activity.getConditionMinQuantity() < 1 ? 1L : activity.getConditionMinQuantity();
        Integer newUserOnly = activity.getConditionNewUserOnly() == null ? 0 : activity.getConditionNewUserOnly();
        String effectMode = normalizeMode(activity.getEffectMode(), activity.getType());
        BigDecimal effectValue = activity.getEffectValue() == null ? BigDecimal.ZERO : activity.getEffectValue();
        Long giftProductId = activity.getGiftProductId() == null ? 0L : activity.getGiftProductId();
        Long giftQuantity = activity.getGiftQuantity() == null || activity.getGiftQuantity() < 1 ? 1L : activity.getGiftQuantity();
        BigDecimal shippingBaseFreight = activity.getShippingBaseFreight() == null ? BigDecimal.ZERO : activity.getShippingBaseFreight();

        activity.setConditionMinAmount(minAmount);
        activity.setConditionMinQuantity(minQuantity);
        activity.setConditionNewUserOnly(newUserOnly);
        activity.setEffectMode(effectMode);
        activity.setEffectValue(("gift".equals(effectMode) || "free_shipping".equals(effectMode)) ? BigDecimal.ZERO : effectValue);
        activity.setGiftProductId("gift".equals(effectMode) ? giftProductId : 0L);
        activity.setGiftQuantity("gift".equals(effectMode) ? giftQuantity : 1L);
        activity.setShippingBaseFreight(shippingBaseFreight);
    }

    private String normalizeMode(String mode, Integer type)
    {
        String normalized = StringUtils.isEmpty(mode) ? "" : mode.trim().toLowerCase();
        if (type != null)
        {
            if (type == 1)
            {
                return "reduce";
            }
            if (type == 2)
            {
                return "discount";
            }
            if (type == 3)
            {
                return "gift";
            }
            if (type == 4)
            {
                return "free_shipping";
            }
            if (type == 5)
            {
                return "set_price";
            }
        }
        if ("discount".equals(normalized) || "reduce".equals(normalized)
            || "set_price".equals(normalized) || "free_shipping".equals(normalized)
            || "gift".equals(normalized))
        {
            return normalized;
        }
        return "reduce";
    }

    private boolean isErrorResult(AjaxResult result)
    {
        if (result == null)
        {
            return false;
        }
        Object code = result.get("code");
        return code != null && !"0".equals(String.valueOf(code)) && !"200".equals(String.valueOf(code));
    }
}
