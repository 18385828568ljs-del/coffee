package com.ruoyi.project.coffee.profile.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import com.ruoyi.project.coffee.profile.domain.ProfileTag;
import com.ruoyi.project.coffee.profile.domain.UserProfile;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpec;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecOptionMapper;

/** Sorts candidates by their match with the user's scene-specific tag profile. */
@Service
public class ProductRecommendationService
{
    private static final Logger log = LoggerFactory.getLogger(ProductRecommendationService.class);
    private static final String SCENE_MALL = "MALL";
    private static final String SCENE_SCAN = "SCAN";

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private ScanProductSpecMapper scanProductSpecMapper;

    @Autowired
    private ScanProductSpecOptionMapper scanProductSpecOptionMapper;

    public List<TProduct> recommendMall(Long userId, List<TProduct> products)
    {
        List<TProduct> original = safeList(products);
        RecommendationContext context = loadContext(userId, SCENE_MALL);
        if (context == null) return original;
        try
        {
            List<ScoredItem<TProduct>> scored = new ArrayList<>();
            for (TProduct product : original)
            {
                if (product != null && product.getStock() != null && product.getStock() > 0)
                {
                    scored.add(score(product, ProfileTagUtils.mall(product), context));
                }
            }
            if (!context.hasPriceTag() && !hasPositiveScore(scored)) return original;
            scored.sort(scoreComparator());
            List<TProduct> result = values(scored);
            return result;
        }
        catch (RuntimeException e)
        {
            log.warn("商城推荐排序失败，沿用原商品顺序，userId={}", userId, e);
            return original;
        }
    }

    public List<ScanProduct> recommendScan(Long userId, List<ScanProduct> products)
    {
        List<ScanProduct> original = safeList(products);
        RecommendationContext context = loadContext(userId, SCENE_SCAN);
        if (context == null) return original;
        try
        {
            List<ScoredItem<ScanProduct>> scored = new ArrayList<>();
            for (ScanProduct product : original)
            {
                if (product == null) continue;
                List<ScanProductSpec> specs = product.getSpecs();
                if (specs == null && product.getProductId() != null
                    && scanProductSpecMapper != null && scanProductSpecOptionMapper != null)
                {
                    specs = loadSpecs(product.getProductId());
                }
                List<ProfileTag> tags = new ArrayList<>(ProfileTagUtils.scanBase(product));
                tags.addAll(ProfileTagUtils.scanSpecs(specs));
                scored.add(score(product, tags, context));
            }
            if (!context.hasPriceTag() && !hasPositiveScore(scored)) return original;
            scored.sort(scoreComparator());
            return values(scored);
        }
        catch (RuntimeException e)
        {
            log.warn("点单推荐排序失败，沿用原商品顺序，userId={}", userId, e);
            return original;
        }
    }

    private RecommendationContext loadContext(Long userId, String scene)
    {
        if (userId == null) return null;
        try
        {
            UserProfile profile = userProfileMapper.selectUserProfileByUserId(userId);
            return profile == null || !"READY".equalsIgnoreCase(profile.getProfileStatus())
                ? null : parseContext(profile, scene);
        }
        catch (RuntimeException e)
        {
            log.warn("读取用户画像失败，沿用原商品顺序，userId={}", userId, e);
            return null;
        }
    }

    private RecommendationContext parseContext(UserProfile profile, String scene)
    {
        RecommendationContext context = new RecommendationContext();
        if (profile.getProfileData() != null && !profile.getProfileData().trim().isEmpty())
        {
            try
            {
                JSONObject root = JSON.parseObject(profile.getProfileData());
                JSONObject sceneData = root == null ? null : root.getJSONObject(scene);
                JSONArray tags = sceneData == null ? null : sceneData.getJSONArray("tags");
                if (tags != null)
                {
                    for (Object raw : tags)
                    {
                        if (!(raw instanceof JSONObject)) continue;
                        JSONObject tag = (JSONObject) raw;
                        String key = tag.getString("key");
                        String dimension = tag.getString("dimension");
                        BigDecimal score = tag.getBigDecimal("score");
                        if ("price".equals(dimension) && score != null && score.signum() > 0)
                        {
                            context.priceTagMin = tag.getDouble("min");
                            context.priceTagMax = tag.getDouble("max");
                            continue;
                        }
                        if (key != null && dimension != null && score != null && score.signum() > 0)
                        {
                            context.scores.put(key, score.doubleValue());
                        }
                    }
                }
            }
            catch (RuntimeException ignored) { }
        }
        return context.scores.isEmpty() && !context.hasPriceTag() ? null : context;
    }

    private List<ScanProductSpec> loadSpecs(Long productId)
    {
        List<ScanProductSpec> specs = scanProductSpecMapper.selectSpecListByProductId(productId);
        if (specs == null) return Collections.emptyList();
        Map<Long, List<ScanProductSpecOption>> optionsBySpec = new HashMap<>();
        List<ScanProductSpecOption> options = scanProductSpecOptionMapper.selectOptionListByProductId(productId);
        if (options != null)
        {
            for (ScanProductSpecOption option : options)
            {
                if (option != null && option.getSpecId() != null)
                {
                    optionsBySpec.computeIfAbsent(option.getSpecId(), key -> new ArrayList<>()).add(option);
                }
            }
        }
        for (ScanProductSpec spec : specs)
        {
            spec.setOptions(optionsBySpec.getOrDefault(spec.getSpecId(), Collections.emptyList()));
        }
        return specs;
    }

    private <T> ScoredItem<T> score(T value, List<ProfileTag> tags, RecommendationContext context)
    {
        double match = 0D;
        for (ProfileTag tag : tags)
        {
            Double current = context.scores.get(tag.getKey());
            if (current != null && current > 0D)
            {
                match += current;
            }
        }
        BigDecimal price = value instanceof TProduct ? ((TProduct) value).getPrice()
            : value instanceof ScanProduct ? ((ScanProduct) value).getPrice() : null;
        return new ScoredItem<>(value, match, priceDistance(price, context));
    }

    private double priceDistance(BigDecimal price, RecommendationContext context)
    {
        if (!context.hasPriceTag()) return 0D;
        if (price == null) return Double.MAX_VALUE;
        double value = price.doubleValue();
        if (value < context.priceTagMin) return context.priceTagMin - value;
        if (value > context.priceTagMax) return value - context.priceTagMax;
        return 0D;
    }

    private boolean hasPositiveScore(List<? extends ScoredItem<?>> values)
    {
        for (ScoredItem<?> item : values) if (item.score > 0D) return true;
        return false;
    }

    private <T> Comparator<ScoredItem<T>> scoreComparator()
    {
        return Comparator.comparingDouble((ScoredItem<T> item) -> item.priceDistance)
            .thenComparing((left, right) -> Double.compare(right.score, left.score));
    }

    private <T> List<T> values(List<ScoredItem<T>> scored)
    {
        List<T> result = new ArrayList<>(scored.size());
        for (ScoredItem<T> item : scored) result.add(item.value);
        return result;
    }

    private <T> List<T> safeList(List<T> values)
    {
        return values == null ? Collections.emptyList() : values;
    }

    private static class RecommendationContext
    {
        private final Map<String, Double> scores = new HashMap<>();
        private Double priceTagMin;
        private Double priceTagMax;

        private boolean hasPriceTag()
        {
            return priceTagMin != null && priceTagMax != null && priceTagMin <= priceTagMax;
        }
    }

    private static class ScoredItem<T>
    {
        private final T value;
        private final double score;
        private final double priceDistance;

        ScoredItem(T value, double score, double priceDistance)
        {
            this.value = value;
            this.score = score;
            this.priceDistance = priceDistance;
        }
    }
}
