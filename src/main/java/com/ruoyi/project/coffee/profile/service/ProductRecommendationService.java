package com.ruoyi.project.coffee.profile.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.profile.domain.ProductPopularity;
import com.ruoyi.project.coffee.profile.domain.RecommendedProduct;
import com.ruoyi.project.coffee.profile.domain.UserProfile;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;

/** 根据画像快照对商品候选集进行一次批量排序。 */
@Service
public class ProductRecommendationService
{
    private static final Logger log = LoggerFactory.getLogger(ProductRecommendationService.class);
    private static final String SCENE_MALL = "MALL";
    private static final String SCENE_SCAN = "SCAN";
    private static final double PRODUCT_WEIGHT = 0.45D;
    private static final double CATEGORY_WEIGHT = 0.30D;
    private static final double PRICE_WEIGHT = 0.10D;
    private static final double POPULARITY_WEIGHT = 0.10D;
    private static final double NEW_ITEM_WEIGHT = 0.05D;

    @Autowired
    private UserProfileMapper userProfileMapper;

    private Clock clock = Clock.systemDefaultZone();

    public List<TProduct> recommendMall(Long userId, List<TProduct> products)
    {
        List<TProduct> original = safeList(products);
        for (TProduct product : original)
        {
            if (product != null)
            {
                product.setRecommendationApplied(false);
            }
        }
        UserProfile profile = loadReadyProfile(userId);
        if (profile == null)
        {
            return original;
        }

        try
        {
            RecommendationContext context = buildContext(profile, SCENE_MALL);
            List<ScoredItem<TProduct>> scored = new ArrayList<>();
            for (TProduct product : original)
            {
                if (product == null || product.getStock() == null || product.getStock() <= 0)
                {
                    continue;
                }
                scored.add(scoreItem(context, product, product.getProductId(), product.getCategoryId(),
                    product.getPrice(), product.getCreateTime()));
            }
            scored.sort((left, right) -> Double.compare(right.score, left.score));
            List<TProduct> result = values(scored);
            for (TProduct product : result)
            {
                product.setRecommendationApplied(true);
            }
            return result;
        }
        catch (RuntimeException e)
        {
            log.warn("读取商城推荐数据失败，沿用原商品顺序，userId={}", userId, e);
            return original;
        }
    }

    public List<ScanProduct> recommendScan(Long userId, List<ScanProduct> products)
    {
        List<ScanProduct> original = safeList(products);
        UserProfile profile = loadReadyProfile(userId);
        if (profile == null)
        {
            return original;
        }

        try
        {
            RecommendationContext context = buildContext(profile, SCENE_SCAN);
            List<ScoredItem<ScanProduct>> scored = new ArrayList<>();
            for (ScanProduct product : original)
            {
                if (product == null)
                {
                    continue;
                }
                scored.add(scoreItem(context, product, product.getProductId(), product.getCategoryId(),
                    product.getPrice(), product.getCreateTime()));
            }
            scored.sort((left, right) -> Double.compare(right.score, left.score));
            return values(scored);
        }
        catch (RuntimeException e)
        {
            log.warn("读取扫码推荐数据失败，沿用原商品顺序，userId={}", userId, e);
            return original;
        }
    }

    /** Return the same mall ranking with the score contribution's primary explanation. */
    public List<RecommendedProduct> explainMall(Long userId, List<TProduct> products, int limit)
    {
        return explainMall(loadReadyProfile(userId), products, limit);
    }

    List<RecommendedProduct> explainMall(UserProfile profile, List<TProduct> products, int limit)
    {
        if (profile == null || !"READY".equalsIgnoreCase(profile.getProfileStatus()) || limit <= 0)
        {
            return Collections.emptyList();
        }
        try
        {
            RecommendationContext context = buildContext(profile, SCENE_MALL);
            List<ScoredItem<TProduct>> scored = new ArrayList<>();
            for (TProduct product : safeList(products))
            {
                if (product == null || product.getStock() == null || product.getStock() <= 0)
                {
                    continue;
                }
                scored.add(scoreItem(context, product, product.getProductId(), product.getCategoryId(),
                    product.getPrice(), product.getCreateTime()));
            }
            scored.sort((left, right) -> Double.compare(right.score, left.score));
            List<RecommendedProduct> result = new ArrayList<>();
            for (int i = 0; i < scored.size() && i < limit; i++)
            {
                TProduct product = scored.get(i).value;
                RecommendedProduct item = new RecommendedProduct();
                item.setProductId(product.getProductId());
                item.setProductName(product.getProductName());
                item.setCategoryId(product.getCategoryId());
                item.setPrice(product.getPrice());
                item.setScore(scored.get(i).score);
                item.setReason(scored.get(i).reason);
                result.add(item);
            }
            return result;
        }
        catch (RuntimeException e)
        {
            log.warn("读取用户画像推荐解释失败，userId={}", profile.getUserId(), e);
            return Collections.emptyList();
        }
    }

    private UserProfile loadReadyProfile(Long userId)
    {
        if (userId == null)
        {
            return null;
        }
        try
        {
            UserProfile profile = userProfileMapper.selectUserProfileByUserId(userId);
            return profile != null && "READY".equalsIgnoreCase(profile.getProfileStatus()) ? profile : null;
        }
        catch (RuntimeException e)
        {
            log.warn("读取用户画像失败，沿用原商品顺序，userId={}", userId, e);
            return null;
        }
    }

    private RecommendationContext buildContext(UserProfile profile, String scene)
    {
        RecommendationContext context = new RecommendationContext();
        JSONObject sceneData = sceneData(profile, scene);
        if (sceneData != null)
        {
            readScores(sceneData.getJSONArray("products"), context.productScores);
            readScores(sceneData.getJSONArray("categories"), context.categoryScores);
        }
        context.productMax = max(context.productScores);
        context.categoryMax = max(context.categoryScores);
        context.priceMin = profile.getPreferredPriceMin();
        context.priceMax = profile.getPreferredPriceMax();

        Date cutoffTime = Date.from(clock.instant().minus(Duration.ofDays(30)));
        List<ProductPopularity> popularity = userProfileMapper.selectRecentProductPopularity(scene, cutoffTime);
        if (popularity != null)
        {
            for (ProductPopularity item : popularity)
            {
                if (item != null && item.getProductId() != null && item.getPopularity() != null
                    && item.getPopularity() > 0)
                {
                    context.popularity.put(item.getProductId(), item.getPopularity().doubleValue());
                }
            }
        }
        context.popularityMax = max(context.popularity);
        return context;
    }

    private JSONObject sceneData(UserProfile profile, String scene)
    {
        if (profile.getProfileData() == null || profile.getProfileData().trim().isEmpty())
        {
            return null;
        }
        try
        {
            JSONObject root = JSON.parseObject(profile.getProfileData());
            return root == null ? null : root.getJSONObject(scene);
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private void readScores(JSONArray values, Map<Long, Double> target)
    {
        if (values == null)
        {
            return;
        }
        for (int i = 0; i < values.size(); i++)
        {
            JSONObject value = values.getJSONObject(i);
            if (value == null || value.getLong("id") == null || value.getBigDecimal("score") == null)
            {
                continue;
            }
            BigDecimal score = value.getBigDecimal("score");
            if (score.signum() > 0)
            {
                target.put(value.getLong("id"), score.doubleValue());
            }
        }
    }

    private ScoredItem<TProduct> scoreItem(RecommendationContext context, TProduct product,
        Long productId, Long categoryId, BigDecimal price, Date createTime)
    {
        double productInterest = normalized(context.productScores.get(productId), context.productMax);
        double categoryInterest = normalized(context.categoryScores.get(categoryId), context.categoryMax);
        double priceMatch = priceMatch(price, context.priceMin, context.priceMax);
        double popularity = normalized(context.popularity.get(productId), context.popularityMax);
        double newItem = isRecent(createTime) ? 1D : 0D;
        double productContribution = PRODUCT_WEIGHT * productInterest;
        double categoryContribution = CATEGORY_WEIGHT * categoryInterest;
        double priceContribution = PRICE_WEIGHT * priceMatch;
        double popularityContribution = POPULARITY_WEIGHT * popularity;
        double newItemContribution = NEW_ITEM_WEIGHT * newItem;
        double total = productContribution
            + categoryContribution
            + priceContribution
            + popularityContribution
            + newItemContribution;
        return new ScoredItem<>(product, total, primaryReason(productContribution, categoryContribution,
            priceContribution, popularityContribution, newItemContribution));
    }

    private ScoredItem<ScanProduct> scoreItem(RecommendationContext context, ScanProduct product,
        Long productId, Long categoryId, BigDecimal price, Date createTime)
    {
        double productInterest = normalized(context.productScores.get(productId), context.productMax);
        double categoryInterest = normalized(context.categoryScores.get(categoryId), context.categoryMax);
        double priceMatch = priceMatch(price, context.priceMin, context.priceMax);
        double popularity = normalized(context.popularity.get(productId), context.popularityMax);
        double newItem = isRecent(createTime) ? 1D : 0D;
        double productContribution = PRODUCT_WEIGHT * productInterest;
        double categoryContribution = CATEGORY_WEIGHT * categoryInterest;
        double priceContribution = PRICE_WEIGHT * priceMatch;
        double popularityContribution = POPULARITY_WEIGHT * popularity;
        double newItemContribution = NEW_ITEM_WEIGHT * newItem;
        return new ScoredItem<>(product, productContribution
            + categoryContribution
            + priceContribution
            + popularityContribution
            + newItemContribution, primaryReason(productContribution, categoryContribution,
                priceContribution, popularityContribution, newItemContribution));
    }

    private String primaryReason(double product, double category, double price, double popularity, double newer)
    {
        double max = product;
        if (Math.max(Math.max(product, category), Math.max(price, Math.max(popularity, newer))) <= 0D)
        {
            return "默认排序";
        }
        String reason = "常购或感兴趣商品";
        if (category > max) { max = category; reason = "偏好分类"; }
        if (price > max) { max = price; reason = "符合常见价格"; }
        if (popularity > max) { max = popularity; reason = "近期热门"; }
        if (newer > max) { reason = "近期新品"; }
        return reason;
    }

    private double priceMatch(BigDecimal price, BigDecimal min, BigDecimal max)
    {
        if (price == null || min == null || max == null)
        {
            return 0D;
        }
        BigDecimal lower = min.min(max);
        BigDecimal upper = min.max(max);
        if (price.compareTo(lower) >= 0 && price.compareTo(upper) <= 0)
        {
            return 1D;
        }
        BigDecimal distance = price.compareTo(lower) < 0 ? lower.subtract(price) : price.subtract(upper);
        BigDecimal range = upper.subtract(lower);
        if (range.signum() <= 0)
        {
            range = new BigDecimal("10");
        }
        double match = 1D - distance.divide(range, 8, BigDecimal.ROUND_HALF_UP).doubleValue();
        return Math.max(0D, match);
    }

    private boolean isRecent(Date createTime)
    {
        return createTime != null && !createTime.before(
            Date.from(clock.instant().minus(Duration.ofDays(30))));
    }

    private double normalized(Double value, double maximum)
    {
        if (value == null || value <= 0D || maximum <= 0D)
        {
            return 0D;
        }
        return Math.min(1D, value / maximum);
    }

    private double max(Map<Long, Double> values)
    {
        double maximum = 0D;
        for (Double value : values.values())
        {
            if (value != null && value > maximum)
            {
                maximum = value;
            }
        }
        return maximum;
    }

    private <T> List<T> values(List<ScoredItem<T>> scored)
    {
        List<T> result = new ArrayList<>(scored.size());
        for (ScoredItem<T> item : scored)
        {
            result.add(item.value);
        }
        return result;
    }

    private <T> List<T> safeList(List<T> values)
    {
        return values == null ? Collections.emptyList() : values;
    }

    private static class RecommendationContext
    {
        private final Map<Long, Double> productScores = new HashMap<>();
        private final Map<Long, Double> categoryScores = new HashMap<>();
        private final Map<Long, Double> popularity = new HashMap<>();
        private double productMax;
        private double categoryMax;
        private double popularityMax;
        private BigDecimal priceMin;
        private BigDecimal priceMax;
    }

    private static class ScoredItem<T>
    {
        private final T value;
        private final double score;
        private final String reason;

        ScoredItem(T value, double score, String reason)
        {
            this.value = value;
            this.score = score;
            this.reason = reason;
        }
    }
}
