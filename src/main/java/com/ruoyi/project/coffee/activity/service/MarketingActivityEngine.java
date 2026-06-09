package com.ruoyi.project.coffee.activity.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.activity.domain.MarketingPreviewResult;
import com.ruoyi.project.coffee.activity.domain.TMarketingActivity;
import com.ruoyi.project.coffee.activity.util.MarketingActivityScopeUtils;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.order.service.ITOrderService;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;
import com.ruoyi.project.system.config.service.IConfigService;
import com.ruoyi.project.coffee.member.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 营销活动规则引擎
 */
@Component
public class MarketingActivityEngine
{
    /** 适用场景常量,与 t_marketing_activity.target_type 一致 */
    public static final String TARGET_MALL = "mall";
    public static final String TARGET_SCAN = "scan";
    public static final String TARGET_BOTH = "both";

    private static final String BASE_FREIGHT_CONFIG_KEY = "coffee.order.baseFreight";
    private static final BigDecimal DEFAULT_BASE_FREIGHT = new BigDecimal("10.00");

    @Autowired
    private ITMarketingActivityService marketingActivityService;

    @Autowired
    private ITProductService productService;

    @Autowired
    private IScanProductService scanProductService;

    @Autowired
    private ITOrderService orderService;

    @Autowired
    private IConfigService configService;

    @Autowired
    private MemberService memberService;

    /**
     * 返回所有处于上线状态、在有效期内的活动(不区分适用场景)
     */
    public List<TMarketingActivity> getActiveActivities()
    {
        return getActiveActivities(null);
    }

    /**
     * 按 targetType 返回处于上线状态、在有效期内的活动
     * @param targetType TARGET_MALL 仅返回 mall + both;
     *                   TARGET_SCAN 仅返回 scan + both;
     *                   null 不过滤 target_type
     */
    public List<TMarketingActivity> getActiveActivities(String targetType)
    {
        TMarketingActivity query = new TMarketingActivity();
        query.setStatus(1);
        List<TMarketingActivity> allActivities = marketingActivityService.selectTMarketingActivityList(query);
        if (allActivities == null || allActivities.isEmpty())
        {
            return Collections.emptyList();
        }

        Date now = DateUtils.getNowDate();
        List<TMarketingActivity> activeActivities = new ArrayList<TMarketingActivity>();
        for (TMarketingActivity activity : allActivities)
        {
            if (activity == null || activity.getStatus() == null || activity.getStatus() != 1)
            {
                continue;
            }
            if (activity.getStartTime() != null && now.before(activity.getStartTime()))
            {
                continue;
            }
            if (activity.getEndTime() != null && now.after(activity.getEndTime()))
            {
                continue;
            }
            if (!matchTargetType(activity, targetType))
            {
                continue;
            }
            activeActivities.add(activity);
        }
        Collections.sort(activeActivities, new Comparator<TMarketingActivity>()
        {
            @Override
            public int compare(TMarketingActivity left, TMarketingActivity right)
            {
                Long rightId = right == null || right.getActivityId() == null ? 0L : right.getActivityId();
                Long leftId = left == null || left.getActivityId() == null ? 0L : left.getActivityId();
                return rightId.compareTo(leftId);
            }
        });
        return activeActivities;
    }

    /**
     * targetType 过滤逻辑:
     *   targetType=null   → 全部通过
     *   targetType=mall   → activity.target_type ∈ {mall, both, null/空}
     *   targetType=scan   → activity.target_type ∈ {scan, both}
     * (历史活动 target_type 为空时按 mall 处理,保证向后兼容)
     */
    private boolean matchTargetType(TMarketingActivity activity, String targetType)
    {
        if (targetType == null)
        {
            return true;
        }
        String activityTarget = activity == null ? null : activity.getTargetType();
        if (StringUtils.isEmpty(activityTarget))
        {
            activityTarget = TARGET_MALL;
        }
        if (TARGET_BOTH.equals(activityTarget))
        {
            return true;
        }
        return targetType.equals(activityTarget);
    }

    public void enrichProducts(List<TProduct> products, Long userId)
    {
        if (products == null || products.isEmpty())
        {
            return;
        }

        List<TMarketingActivity> activities = getActiveActivities(TARGET_MALL);
        boolean isNewUser = isNewUser(userId);
        for (TProduct product : products)
        {
            enrichProduct(product, activities, isNewUser);
        }
    }

    public void enrichProduct(TProduct product, Long userId)
    {
        if (product == null)
        {
            return;
        }
        enrichProduct(product, getActiveActivities(TARGET_MALL), isNewUser(userId));
    }

    public MarketingPreviewResult previewOrder(Long userId, List<TOrderItem> orderItems)
    {
        List<PricingItem> pricingItems = buildPricingItems(orderItems, false);
        return calculateBestResult(pricingItems, getActiveActivities(TARGET_MALL), isNewUser(userId), userId, true);
    }

    public MarketingPreviewResult applyOrderMarketing(TOrder order, List<TOrderItem> orderItems)
    {
        MarketingPreviewResult previewResult = previewOrder(order.getUserId(), orderItems);
        order.setTotalAmount(previewResult.getTotalAmount());
        order.setPayAmount(previewResult.getPayAmount());
        order.setDiscountAmount(previewResult.getDiscountAmount());
        order.setMemberDiscount(previewResult.getMemberDiscount());
        order.setFreightAmount(previewResult.getFreightAmount());
        order.setActivitySummary(previewResult.getActivitySummary());
        return previewResult;
    }

    /**
     * 扫码点单(堂食)按当前活动 + 会员折扣预览金额。
     * 与 mall 流程的差异:
     *   1) 仅匹配 target_type=scan / both 的活动
     *   2) 商品来源是 t_scan_product
     *   3) 不计运费(堂食无配送)
     */
    public MarketingPreviewResult previewScanOrder(Long userId, List<ScanCart> scanCarts)
    {
        List<PricingItem> pricingItems = buildScanPricingItems(scanCarts);
        return calculateBestResult(pricingItems, getActiveActivities(TARGET_SCAN), isNewUser(userId), userId, false);
    }

    /**
     * 把扫码订单的金额字段按预览结果回填,供 ScanOrderServiceImpl 落库。
     */
    public MarketingPreviewResult applyScanOrderMarketing(ScanOrder order, List<ScanCart> scanCarts)
    {
        MarketingPreviewResult previewResult = previewScanOrder(order.getUserId(), scanCarts);
        order.setTotalAmount(previewResult.getTotalAmount());
        order.setPayAmount(previewResult.getPayAmount());
        order.setDiscountAmount(previewResult.getDiscountAmount());
        order.setMemberDiscount(previewResult.getMemberDiscount());
        order.setActivitySummary(previewResult.getActivitySummary());
        return previewResult;
    }

    private void enrichProduct(TProduct product, List<TMarketingActivity> activities, boolean isNewUser)
    {
        if (product == null)
        {
            return;
        }

        product.setOriginalPrice(product.getPrice());

        Set<String> activityTags = new LinkedHashSet<String>();
        PricingItem pricingItem = new PricingItem();
        pricingItem.setProductId(product.getProductId());
        pricingItem.setCategoryId(product.getCategoryId());
        pricingItem.setUnitPrice(product.getPrice());
        pricingItem.setQuantity(1L);

        for (TMarketingActivity activity : activities)
        {
            RuleDefinition rule = parseRule(activity);
            if (!matchScope(activity, pricingItem))
            {
                continue;
            }
            if (rule.isNewUserOnly() && !isNewUser)
            {
                continue;
            }
            activityTags.add(activity.getTitle());
        }

        MarketingPreviewResult previewResult = calculateBestResult(Collections.singletonList(pricingItem), activities, isNewUser, null, true);
        if (previewResult.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
        {
            product.setSalePrice(product.getPrice().subtract(previewResult.getDiscountAmount()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
            product.setActivitySummary(previewResult.getActivitySummary());
        }

        List<String> tags = new ArrayList<String>(activityTags);
        product.setActivityTags(tags);
        product.setActivityTag(tags.isEmpty() ? null : tags.get(0));
    }

    private List<PricingItem> buildPricingItems(List<TOrderItem> orderItems, boolean requireStock)
    {
        if (orderItems == null || orderItems.isEmpty())
        {
            return Collections.emptyList();
        }

        List<Long> productIds = new ArrayList<Long>();
        for (TOrderItem orderItem : orderItems)
        {
            if (orderItem != null && orderItem.getProductId() != null)
            {
                productIds.add(orderItem.getProductId());
            }
        }

        List<TProduct> products = productService.selectTProductByProductIds(productIds);
        Map<Long, TProduct> productMap = new java.util.HashMap<Long, TProduct>();
        for (TProduct product : products)
        {
            if (product != null && product.getProductId() != null)
            {
                productMap.put(product.getProductId(), product);
            }
        }

        List<PricingItem> pricingItems = new ArrayList<PricingItem>();
        for (TOrderItem orderItem : orderItems)
        {
            if (orderItem == null || orderItem.getProductId() == null || orderItem.getQuantity() == null || orderItem.getQuantity() < 1)
            {
                throw new IllegalArgumentException("订单商品参数不完整");
            }

            TProduct product = productMap.get(orderItem.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != 1)
            {
                throw new IllegalArgumentException("商品不存在或已下架");
            }
            if (requireStock && product.getStock() != null && product.getStock() < orderItem.getQuantity())
            {
                throw new IllegalArgumentException("商品库存不足");
            }

            PricingItem pricingItem = new PricingItem();
            pricingItem.setProductId(product.getProductId());
            pricingItem.setCategoryId(product.getCategoryId());
            pricingItem.setUnitPrice(product.getPrice());
            pricingItem.setQuantity(orderItem.getQuantity());
            pricingItems.add(pricingItem);
        }
        return pricingItems;
    }

    /**
     * 把扫码购物车转换成计价用的 PricingItem。
     * 注意:
     *   1) 商品来自 t_scan_product, productId / categoryId 与 t_product 完全独立
     *   2) 单价以 ScanCart.price 为准(加购时已强制覆盖为商品当前价),fallback 到 ScanProduct.price
     *   3) 商品不存在或已下架直接抛错,与 mall 流程一致
     */
    private List<PricingItem> buildScanPricingItems(List<ScanCart> scanCarts)
    {
        if (scanCarts == null || scanCarts.isEmpty())
        {
            return Collections.emptyList();
        }

        List<PricingItem> pricingItems = new ArrayList<PricingItem>();
        for (ScanCart cart : scanCarts)
        {
            if (cart == null || cart.getProductId() == null
                || cart.getQuantity() == null || cart.getQuantity() < 1)
            {
                throw new IllegalArgumentException("点单商品参数不完整");
            }
            ScanProduct product = scanProductService.selectScanProductById(cart.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != 1)
            {
                throw new IllegalArgumentException("点单商品不存在或已下架");
            }

            BigDecimal unitPrice = cart.getPrice() != null ? cart.getPrice()
                : (product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);

            PricingItem pricingItem = new PricingItem();
            pricingItem.setProductId(product.getProductId());
            pricingItem.setCategoryId(product.getCategoryId());
            pricingItem.setUnitPrice(unitPrice);
            pricingItem.setQuantity(cart.getQuantity().longValue());
            pricingItems.add(pricingItem);
        }
        return pricingItems;
    }

    private MarketingPreviewResult calculateBestResult(List<PricingItem> pricingItems, List<TMarketingActivity> activities, boolean isNewUser, Long userId, boolean includeFreight)
    {
        MarketingPreviewResult emptyResult = createBaseResult(pricingItems);
        if (pricingItems == null || pricingItems.isEmpty())
        {
            return emptyResult;
        }

        BigDecimal originalTotal = emptyResult.getTotalAmount();
        BigDecimal baseFreight = includeFreight ? resolveBaseFreight(activities) : BigDecimal.ZERO;
        List<MatchedActivity> matchedActivities = buildMatchedActivities(pricingItems, activities, isNewUser);
        MatchedActivity priceActivity = selectBestPriceActivity(matchedActivities);
        MatchedActivity freeShippingActivity = includeFreight ? selectBestFreeShippingActivity(matchedActivities) : null;
        MatchedActivity giftActivity = selectBestGiftActivity(matchedActivities);

        List<String> activityTitles = new ArrayList<String>();
        BigDecimal totalDiscount = BigDecimal.ZERO;
        if (priceActivity != null)
        {
            totalDiscount = totalDiscount.add(priceActivity.getDiscountAmount());
            addActivityTitle(activityTitles, priceActivity);
        }
        if (freeShippingActivity != null)
        {
            addActivityTitle(activityTitles, freeShippingActivity);
        }
        if (giftActivity != null)
        {
            addActivityTitle(activityTitles, giftActivity);
        }

        BigDecimal memberDiscount = BigDecimal.ZERO;
        if (userId != null)
        {
            BigDecimal discountRate = memberService.getMemberDiscountRate(userId);
            if (discountRate != null && discountRate.compareTo(BigDecimal.ONE) < 0)
            {
                BigDecimal amountForDiscount = originalTotal.subtract(totalDiscount);
                if (amountForDiscount.compareTo(BigDecimal.ZERO) > 0)
                {
                    memberDiscount = amountForDiscount.multiply(BigDecimal.ONE.subtract(discountRate)).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }

        MarketingPreviewResult result = createBaseResult(pricingItems);
        BigDecimal freightAmount = includeFreight && freeShippingActivity == null ? baseFreight : BigDecimal.ZERO;
        result.setDiscountAmount(totalDiscount.setScale(2, RoundingMode.HALF_UP));
        result.setMemberDiscount(memberDiscount);
        result.setFreightAmount(freightAmount.setScale(2, RoundingMode.HALF_UP));

        BigDecimal payAmount = originalTotal.add(freightAmount).subtract(totalDiscount).subtract(memberDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        result.setPayAmount(payAmount);

        result.setGiftItems(giftActivity == null ? Collections.<TOrderItem>emptyList() : giftActivity.getGiftItems());
        result.setActivityTitles(activityTitles);
        result.setActivitySummary(activityTitles.isEmpty() ? "" : StringUtils.join(activityTitles, " / "));
        return result;
    }

    private List<MatchedActivity> buildMatchedActivities(List<PricingItem> pricingItems, List<TMarketingActivity> activities,
                                                         boolean isNewUser)
    {
        if (pricingItems == null || pricingItems.isEmpty() || activities == null || activities.isEmpty())
        {
            return Collections.emptyList();
        }

        List<MatchedActivity> matchedActivities = new ArrayList<MatchedActivity>();
        for (TMarketingActivity activity : activities)
        {
            RuleDefinition rule = parseRule(activity);
            if (rule.isNewUserOnly() && !isNewUser)
            {
                continue;
            }

            BigDecimal matchedSubtotal = BigDecimal.ZERO;
            long matchedQuantity = 0L;
            for (PricingItem pricingItem : pricingItems)
            {
                if (matchScope(activity, pricingItem))
                {
                    matchedSubtotal = matchedSubtotal.add(pricingItem.getSubtotal());
                    matchedQuantity += pricingItem.getQuantity();
                }
            }

            if (matchedSubtotal.compareTo(BigDecimal.ZERO) <= 0)
            {
                continue;
            }
            if (matchedSubtotal.compareTo(rule.getMinAmount()) < 0 || matchedQuantity < rule.getMinQuantity())
            {
                continue;
            }

            BigDecimal discountAmount = calculateDiscount(rule, matchedSubtotal, matchedQuantity);
            boolean freeShipping = calculateFreeShipping(rule, matchedSubtotal, matchedQuantity);
            GiftResult giftResult = buildGiftResult(rule);
            if (discountAmount.compareTo(BigDecimal.ZERO) <= 0 && !freeShipping && giftResult.getItems().isEmpty())
            {
                continue;
            }

            MatchedActivity matchedActivity = new MatchedActivity();
            matchedActivity.setActivity(activity);
            matchedActivity.setRule(rule);
            matchedActivity.setMatchedSubtotal(matchedSubtotal.setScale(2, RoundingMode.HALF_UP));
            matchedActivity.setMatchedQuantity(matchedQuantity);
            matchedActivity.setDiscountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP));
            matchedActivity.setFreeShipping(freeShipping);
            matchedActivity.setGiftItems(giftResult.getItems());
            matchedActivity.setPriceType(isPriceEffect(rule.getEffectMode()));
            matchedActivity.setBenefit(giftResult.getBenefit().setScale(2, RoundingMode.HALF_UP));
            matchedActivities.add(matchedActivity);
        }
        return matchedActivities;
    }

    private MatchedActivity selectBestPriceActivity(List<MatchedActivity> matchedActivities)
    {
        MatchedActivity bestActivity = null;
        for (MatchedActivity matchedActivity : matchedActivities)
        {
            if (matchedActivity == null || !matchedActivity.isPriceType())
            {
                continue;
            }
            if (bestActivity == null)
            {
                bestActivity = matchedActivity;
                continue;
            }

            int discountCompare = matchedActivity.getDiscountAmount().compareTo(bestActivity.getDiscountAmount());
            if (discountCompare > 0 || (discountCompare == 0 && hasHigherActivityId(matchedActivity, bestActivity)))
            {
                bestActivity = matchedActivity;
            }
        }
        return bestActivity;
    }

    private MatchedActivity selectBestFreeShippingActivity(List<MatchedActivity> matchedActivities)
    {
        MatchedActivity bestActivity = null;
        for (MatchedActivity matchedActivity : matchedActivities)
        {
            if (matchedActivity == null || !matchedActivity.isFreeShipping())
            {
                continue;
            }
            if (bestActivity == null)
            {
                bestActivity = matchedActivity;
                continue;
            }

            int thresholdCompare = compareThreshold(matchedActivity, bestActivity);
            if (thresholdCompare > 0 || (thresholdCompare == 0 && hasHigherActivityId(matchedActivity, bestActivity)))
            {
                bestActivity = matchedActivity;
            }
        }
        return bestActivity;
    }

    private MatchedActivity selectBestGiftActivity(List<MatchedActivity> matchedActivities)
    {
        MatchedActivity bestActivity = null;
        for (MatchedActivity matchedActivitiesItem : matchedActivities)
        {
            if (matchedActivitiesItem == null || matchedActivitiesItem.getGiftItems().isEmpty())
            {
                continue;
            }
            if (bestActivity == null)
            {
                bestActivity = matchedActivitiesItem;
                continue;
            }

            int benefitCompare = matchedActivitiesItem.getBenefit().compareTo(bestActivity.getBenefit());
            if (benefitCompare > 0 || (benefitCompare == 0 && hasHigherActivityId(matchedActivitiesItem, bestActivity)))
            {
                bestActivity = matchedActivitiesItem;
            }
        }
        return bestActivity;
    }

    private void addActivityTitle(List<String> activityTitles, MatchedActivity matchedActivity)
    {
        if (activityTitles == null || matchedActivity == null || StringUtils.isEmpty(matchedActivity.getTitle()))
        {
            return;
        }
        activityTitles.add(matchedActivity.getTitle());
    }

    private int compareThreshold(MatchedActivity left, MatchedActivity right)
    {
        if (left == null || right == null)
        {
            return 0;
        }
        int amountCompare = left.getRule().getMinAmount().compareTo(right.getRule().getMinAmount());
        if (amountCompare != 0)
        {
            return amountCompare;
        }
        return Long.compare(left.getRule().getMinQuantity(), right.getRule().getMinQuantity());
    }

    private boolean hasHigherActivityId(MatchedActivity left, MatchedActivity right)
    {
        return resolveActivityId(left == null ? null : left.getActivity())
            .compareTo(resolveActivityId(right == null ? null : right.getActivity())) > 0;
    }

    private MarketingPreviewResult createBaseResult(List<PricingItem> pricingItems)
    {
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (pricingItems != null)
        {
            for (PricingItem pricingItem : pricingItems)
            {
                totalAmount = totalAmount.add(pricingItem.getSubtotal());
            }
        }

        MarketingPreviewResult result = new MarketingPreviewResult();
        result.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        result.setPayAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        result.setDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        result.setFreightAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        result.setActivityTitles(Collections.<String>emptyList());
        result.setActivitySummary("");
        result.setGiftItems(Collections.<TOrderItem>emptyList());
        return result;
    }

    private BigDecimal resolveBaseFreight(List<TMarketingActivity> activities)
    {
        for (TMarketingActivity activity : activities)
        {
            RuleDefinition rule = parseRule(activity);
            if (rule.getBaseFreight().compareTo(BigDecimal.ZERO) > 0)
            {
                return rule.getBaseFreight().setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal configuredFreight = getDecimal(configService.selectConfigByKey(BASE_FREIGHT_CONFIG_KEY));
        if (configuredFreight.compareTo(BigDecimal.ZERO) > 0)
        {
            return configuredFreight.setScale(2, RoundingMode.HALF_UP);
        }
        return DEFAULT_BASE_FREIGHT.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDiscount(RuleDefinition rule, BigDecimal matchedSubtotal, long matchedQuantity)
    {
        if ("reduce".equals(rule.getEffectMode()))
        {
            return rule.getEffectValue().min(matchedSubtotal).setScale(2, RoundingMode.HALF_UP);
        }
        if ("discount".equals(rule.getEffectMode()))
        {
            BigDecimal rate = rule.getEffectValue();
            if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0)
            {
                return BigDecimal.ZERO;
            }
            return matchedSubtotal.multiply(BigDecimal.ONE.subtract(rate)).setScale(2, RoundingMode.HALF_UP);
        }
        if ("set_price".equals(rule.getEffectMode()))
        {
            BigDecimal targetPrice = rule.getEffectValue();
            BigDecimal targetTotal = targetPrice.multiply(BigDecimal.valueOf(matchedQuantity)).setScale(2, RoundingMode.HALF_UP);
            if (targetPrice.compareTo(BigDecimal.ZERO) < 0 || matchedSubtotal.compareTo(targetTotal) <= 0)
            {
                return BigDecimal.ZERO;
            }
            return matchedSubtotal.subtract(targetTotal).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private GiftResult buildGiftResult(RuleDefinition rule)
    {
        GiftResult giftResult = new GiftResult();
        if (!"gift".equals(rule.getEffectMode()) || rule.getGiftProductId() <= 0 || rule.getGiftQuantity() < 1)
        {
            return giftResult;
        }

        TProduct giftProduct = productService.selectTProductByProductId(rule.getGiftProductId());
        if (giftProduct == null || giftProduct.getStatus() == null || giftProduct.getStatus() != 1)
        {
            return giftResult;
        }
        if (giftProduct.getStock() == null || giftProduct.getStock() < rule.getGiftQuantity())
        {
            return giftResult;
        }

        TOrderItem giftItem = new TOrderItem();
        giftItem.setProductId(giftProduct.getProductId());
        giftItem.setProductName(giftProduct.getProductName());
        giftItem.setProductImage(giftProduct.getImageUrl());
        giftItem.setPrice(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        giftItem.setQuantity(rule.getGiftQuantity());
        giftItem.setSpec("赠品 / 活动附送");
        giftItem.setTotalPrice(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        giftItem.setGiftItem(Boolean.TRUE);

        giftResult.setItems(Collections.singletonList(giftItem));
        giftResult.setBenefit(
            giftProduct.getPrice().multiply(BigDecimal.valueOf(rule.getGiftQuantity())).setScale(2, RoundingMode.HALF_UP));
        return giftResult;
    }

    private boolean calculateFreeShipping(RuleDefinition rule, BigDecimal matchedSubtotal, long matchedQuantity)
    {
        if (!"free_shipping".equals(rule.getEffectMode()))
        {
            return false;
        }
        if (matchedSubtotal.compareTo(rule.getMinAmount()) < 0)
        {
            return false;
        }
        return matchedQuantity >= rule.getMinQuantity();
    }

    private boolean matchScope(TMarketingActivity activity, PricingItem pricingItem)
    {
        if (activity == null || pricingItem == null || activity.getScopeType() == null)
        {
            return false;
        }

        if (activity.getScopeType() == 0)
        {
            return true;
        }

        List<Long> scopeIds = resolveScopeIds(activity);
        if (scopeIds.isEmpty())
        {
            return false;
        }

        if (activity.getScopeType() == 1)
        {
            return pricingItem.getCategoryId() != null && scopeIds.contains(pricingItem.getCategoryId());
        }
        if (activity.getScopeType() == 2)
        {
            return pricingItem.getProductId() != null && scopeIds.contains(pricingItem.getProductId());
        }
        return false;
    }

    private RuleDefinition parseRule(TMarketingActivity activity)
    {
        RuleDefinition definition = new RuleDefinition();
        definition.setEffectMode("none");
        definition.setEffectValue(BigDecimal.ZERO);
        definition.setMinAmount(BigDecimal.ZERO);
        definition.setMinQuantity(1L);
        definition.setBaseFreight(BigDecimal.ZERO);

        if (activity == null)
        {
            return definition;
        }

        definition.setNewUserOnly(activity.getConditionNewUserOnly() != null && activity.getConditionNewUserOnly() == 1);
        definition.setMinAmount(activity.getConditionMinAmount());
        definition.setMinQuantity(activity.getConditionMinQuantity() == null ? 1L : activity.getConditionMinQuantity());
        definition.setBaseFreight(activity.getShippingBaseFreight());
        definition.setGiftProductId(activity.getGiftProductId() == null ? 0L : activity.getGiftProductId());
        definition.setGiftQuantity(activity.getGiftQuantity() == null ? 1L : activity.getGiftQuantity());

        String structuredMode = normalizeMode(activity.getEffectMode());
        if (!"none".equals(structuredMode))
        {
            definition.setEffectMode(structuredMode);
            definition.setEffectValue(activity.getEffectValue());
            if ("gift".equals(structuredMode) || "free_shipping".equals(structuredMode))
            {
                definition.setEffectValue(BigDecimal.ZERO);
            }
        }
        else if (definition.getGiftProductId() > 0)
        {
            definition.setEffectMode("gift");
            definition.setEffectValue(BigDecimal.ZERO);
        }

        return definition;
    }

    private List<Long> resolveScopeIds(TMarketingActivity activity)
    {
        if (activity == null)
        {
            return Collections.emptyList();
        }
        if (activity.getScopeIds() != null && !activity.getScopeIds().isEmpty())
        {
            return MarketingActivityScopeUtils.dedupeScopeIds(activity.getScopeIds());
        }
        if (StringUtils.isNotEmpty(activity.getScopeIdsText()))
        {
            return MarketingActivityScopeUtils.parseScopeIds(activity.getScopeIdsText());
        }
        return Collections.emptyList();
    }

    private boolean isPriceEffect(String effectMode)
    {
        return "reduce".equals(effectMode) || "discount".equals(effectMode) || "set_price".equals(effectMode);
    }

    private Long resolveActivityId(TMarketingActivity activity)
    {
        return activity == null || activity.getActivityId() == null ? 0L : activity.getActivityId();
    }

    private boolean isNewUser(Long userId)
    {
        if (userId == null)
        {
            return false;
        }

        return orderService.countCompletedOrdersByUserId(userId) == 0;
    }

    private BigDecimal getDecimal(Object value)
    {
        if (value == null)
        {
            return BigDecimal.ZERO;
        }
        try
        {
            return new BigDecimal(String.valueOf(value));
        }
        catch (Exception ignored)
        {
            return BigDecimal.ZERO;
        }
    }

    private String normalizeMode(String mode)
    {
        String value = mode == null ? "" : mode.trim().toLowerCase();
        if ("discount".equals(value) || "reduce".equals(value) || "set_price".equals(value)
            || "free_shipping".equals(value) || "gift".equals(value))
        {
            return value;
        }
        if ("freeshipping".equals(value) || "shipping_free".equals(value))
        {
            return "free_shipping";
        }
        if ("special_price".equals(value) || "specialprice".equals(value) || "fixed".equals(value))
        {
            return "set_price";
        }
        return "none";
    }

    private static class MatchedActivity
    {
        private TMarketingActivity activity;
        private RuleDefinition rule;
        private BigDecimal matchedSubtotal = BigDecimal.ZERO;
        private long matchedQuantity;
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private boolean freeShipping;
        private List<TOrderItem> giftItems = Collections.emptyList();
        private BigDecimal benefit = BigDecimal.ZERO;
        private boolean priceType;

        public TMarketingActivity getActivity()
        {
            return activity;
        }

        public void setActivity(TMarketingActivity activity)
        {
            this.activity = activity;
        }

        public RuleDefinition getRule()
        {
            return rule;
        }

        public void setRule(RuleDefinition rule)
        {
            this.rule = rule;
        }

        public BigDecimal getMatchedSubtotal()
        {
            return matchedSubtotal == null ? BigDecimal.ZERO : matchedSubtotal;
        }

        public void setMatchedSubtotal(BigDecimal matchedSubtotal)
        {
            this.matchedSubtotal = matchedSubtotal;
        }

        public long getMatchedQuantity()
        {
            return matchedQuantity;
        }

        public void setMatchedQuantity(long matchedQuantity)
        {
            this.matchedQuantity = matchedQuantity;
        }

        public BigDecimal getDiscountAmount()
        {
            return discountAmount == null ? BigDecimal.ZERO : discountAmount;
        }

        public void setDiscountAmount(BigDecimal discountAmount)
        {
            this.discountAmount = discountAmount;
        }

        public boolean isFreeShipping()
        {
            return freeShipping;
        }

        public void setFreeShipping(boolean freeShipping)
        {
            this.freeShipping = freeShipping;
        }

        public List<TOrderItem> getGiftItems()
        {
            return giftItems == null ? Collections.<TOrderItem>emptyList() : giftItems;
        }

        public void setGiftItems(List<TOrderItem> giftItems)
        {
            this.giftItems = giftItems == null ? Collections.<TOrderItem>emptyList() : giftItems;
        }

        public BigDecimal getBenefit()
        {
            return benefit == null ? BigDecimal.ZERO : benefit;
        }

        public void setBenefit(BigDecimal benefit)
        {
            this.benefit = benefit;
        }

        public boolean isPriceType()
        {
            return priceType;
        }

        public void setPriceType(boolean priceType)
        {
            this.priceType = priceType;
        }

        public String getTitle()
        {
            return activity == null ? null : activity.getTitle();
        }
    }

    private static class PricingItem
    {
        private Long productId;
        private Long categoryId;
        private BigDecimal unitPrice;
        private Long quantity;

        public Long getProductId()
        {
            return productId;
        }

        public void setProductId(Long productId)
        {
            this.productId = productId;
        }

        public Long getCategoryId()
        {
            return categoryId;
        }

        public void setCategoryId(Long categoryId)
        {
            this.categoryId = categoryId;
        }

        public BigDecimal getUnitPrice()
        {
            return unitPrice == null ? BigDecimal.ZERO : unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice)
        {
            this.unitPrice = unitPrice;
        }

        public Long getQuantity()
        {
            return quantity == null ? 0L : quantity;
        }

        public void setQuantity(Long quantity)
        {
            this.quantity = quantity;
        }

        public BigDecimal getSubtotal()
        {
            return getUnitPrice().multiply(BigDecimal.valueOf(getQuantity())).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private static class RuleDefinition
    {
        private String effectMode;
        private BigDecimal effectValue;
        private BigDecimal minAmount;
        private long minQuantity;
        private BigDecimal baseFreight;
        private boolean newUserOnly;
        private long giftProductId;
        private long giftQuantity;

        public String getEffectMode()
        {
            return effectMode;
        }

        public void setEffectMode(String effectMode)
        {
            this.effectMode = effectMode;
        }

        public BigDecimal getEffectValue()
        {
            return effectValue == null ? BigDecimal.ZERO : effectValue;
        }

        public void setEffectValue(BigDecimal effectValue)
        {
            this.effectValue = effectValue;
        }

        public BigDecimal getMinAmount()
        {
            return minAmount == null ? BigDecimal.ZERO : minAmount;
        }

        public void setMinAmount(BigDecimal minAmount)
        {
            this.minAmount = minAmount;
        }

        public long getMinQuantity()
        {
            return minQuantity < 1 ? 1L : minQuantity;
        }

        public void setMinQuantity(long minQuantity)
        {
            this.minQuantity = minQuantity;
        }

        public BigDecimal getBaseFreight()
        {
            return baseFreight == null ? BigDecimal.ZERO : baseFreight;
        }

        public void setBaseFreight(BigDecimal baseFreight)
        {
            this.baseFreight = baseFreight;
        }

        public boolean isNewUserOnly()
        {
            return newUserOnly;
        }

        public void setNewUserOnly(boolean newUserOnly)
        {
            this.newUserOnly = newUserOnly;
        }

        public long getGiftProductId()
        {
            return giftProductId;
        }

        public void setGiftProductId(long giftProductId)
        {
            this.giftProductId = giftProductId;
        }

        public long getGiftQuantity()
        {
            return giftQuantity < 1 ? 1L : giftQuantity;
        }

        public void setGiftQuantity(long giftQuantity)
        {
            this.giftQuantity = giftQuantity;
        }
    }

    private static class GiftResult
    {
        private List<TOrderItem> items = Collections.emptyList();
        private BigDecimal benefit = BigDecimal.ZERO;

        public List<TOrderItem> getItems()
        {
            return items;
        }

        public void setItems(List<TOrderItem> items)
        {
            this.items = items == null ? Collections.<TOrderItem>emptyList() : items;
        }

        public BigDecimal getBenefit()
        {
            return benefit == null ? BigDecimal.ZERO : benefit;
        }

        public void setBenefit(BigDecimal benefit)
        {
            this.benefit = benefit;
        }
    }
}
