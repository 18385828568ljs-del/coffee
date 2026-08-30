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
import com.ruoyi.project.coffee.profile.domain.RecommendationIntent;
import com.ruoyi.project.coffee.profile.domain.UserProfile;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpec;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecOptionMapper;

/** Ranks candidates by long-term scene preferences and current request intent. */
@Service
public class ProductRecommendationService
{
    private static final Logger log = LoggerFactory.getLogger(ProductRecommendationService.class);
    private static final String SCENE_MALL = "MALL";
    private static final String SCENE_SCAN = "SCAN";
    private static final double INTENT_WEIGHT = 0.5D;
    private static final double TAG_WEIGHT = 0.3D;
    private static final double PRICE_WEIGHT = 0.2D;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private ScanProductSpecMapper scanProductSpecMapper;

    @Autowired
    private ScanProductSpecOptionMapper scanProductSpecOptionMapper;

    public List<TProduct> recommendMall(Long userId, List<TProduct> products)
    {
        return recommendMall(userId, null, products);
    }

    public List<TProduct> recommendMall(Long userId, RecommendationIntent intent, List<TProduct> products)
    {
        List<TProduct> original = safeList(products);
        RecommendationContext context = loadContext(userId, SCENE_MALL, intent);
        if (context == null) return original;
        try
        {
            List<ScoredItem<TProduct>> scored = new ArrayList<>();
            for (int i = 0; i < original.size(); i++)
            {
                TProduct product = original.get(i);
                if (product != null && product.getStock() != null && product.getStock() > 0)
                {
                    scored.add(score(product, ProfileTagUtils.mall(product), context, i));
                }
            }
            if (!hasPositiveScore(scored)) return original;
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
        return recommendScan(userId, null, products);
    }

    public List<ScanProduct> recommendScan(Long userId, RecommendationIntent intent, List<ScanProduct> products)
    {
        List<ScanProduct> original = safeList(products);
        RecommendationContext context = loadContext(userId, SCENE_SCAN, intent);
        if (context == null) return original;
        try
        {
            List<ScoredItem<ScanProduct>> scored = new ArrayList<>();
            for (int i = 0; i < original.size(); i++)
            {
                ScanProduct product = original.get(i);
                if (product == null) continue;
                List<ScanProductSpec> specs = product.getSpecs();
                if (specs == null && product.getProductId() != null
                    && scanProductSpecMapper != null && scanProductSpecOptionMapper != null)
                {
                    specs = loadSpecs(product.getProductId());
                }
                List<ProfileTag> tags = new ArrayList<>(ProfileTagUtils.scanBase(product));
                tags.addAll(ProfileTagUtils.scanSpecs(specs));
                scored.add(score(product, tags, context, i));
            }
            if (!hasPositiveScore(scored)) return original;
            scored.sort(scoreComparator());
            return values(scored);
        }
        catch (RuntimeException e)
        {
            log.warn("点单推荐排序失败，沿用原商品顺序，userId={}", userId, e);
            return original;
        }
    }

    private RecommendationContext loadContext(Long userId, String scene, RecommendationIntent intent)
    {
        if (userId == null) return null;
        try
        {
            UserProfile profile = userProfileMapper.selectUserProfileByUserId(userId);
            return profile == null || !"READY".equalsIgnoreCase(profile.getProfileStatus())
                ? null : parseContext(profile, scene, intent);
        }
        catch (RuntimeException e)
        {
            log.warn("读取用户画像失败，沿用原商品顺序，userId={}", userId, e);
            return null;
        }
    }

    private RecommendationContext parseContext(UserProfile profile, String scene, RecommendationIntent intent)
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
                            Double min = tag.getDouble("min");
                            Double max = tag.getDouble("max");
                            if (min != null && max != null && min <= max)
                            {
                                context.pricePreferences.add(new PricePreference(min, max, score.doubleValue()));
                            }
                            continue;
                        }
                        if (key != null && dimension != null && score != null && score.signum() > 0)
                        {
                            context.scores.put(key, score.doubleValue());
                            context.preferenceTotal += score.doubleValue();
                        }
                    }
                }
            }
            catch (RuntimeException ignored) { }
        }
        if (intent != null)
        {
            context.intent = intent;
        }
        return context.hasAnySignal() ? context : null;
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

    private <T> ScoredItem<T> score(T value, List<ProfileTag> tags, RecommendationContext context, int originalIndex)
    {
        double preferenceMatch = 0D;
        double intentMatch = 0D;
        for (ProfileTag tag : tags)
        {
            Double current = context.scores.get(tag.getKey());
            if (current != null && current > 0D)
            {
                preferenceMatch += current;
            }
            if (context.intent != null)
            {
                Double intentScore = context.intent.getTagScores().get(tag.getKey());
                if (intentScore != null && intentScore > 0D)
                {
                    intentMatch += intentScore;
                }
            }
        }
        Long productId = value instanceof TProduct ? ((TProduct) value).getProductId()
            : value instanceof ScanProduct ? ((ScanProduct) value).getProductId() : null;
        if (context.intent != null && productId != null)
        {
            intentMatch += context.intent.getProductScores().getOrDefault(productId, 0D);
        }
        BigDecimal price = value instanceof TProduct ? ((TProduct) value).getPrice()
            : value instanceof ScanProduct ? ((ScanProduct) value).getPrice() : null;
        double tagPreferenceScore = context.preferenceTotal <= 0D ? 0D : preferenceMatch / context.preferenceTotal;
        double intentScore = context.intent == null || context.intent.totalScore() <= 0D
            ? 0D : Math.min(1D, intentMatch / context.intent.totalScore());
        double pricePreferenceScore = priceFit(price, context);
        double longTermPreferenceScore = tagPreferenceScore * (TAG_WEIGHT / (TAG_WEIGHT + PRICE_WEIGHT))
            + pricePreferenceScore * (PRICE_WEIGHT / (TAG_WEIGHT + PRICE_WEIGHT));
        double recommendationScore = context.hasIntent()
            ? intentScore * INTENT_WEIGHT + tagPreferenceScore * TAG_WEIGHT + pricePreferenceScore * PRICE_WEIGHT
            : longTermPreferenceScore;
        return new ScoredItem<>(value, recommendationScore, originalIndex);
    }

    private double priceFit(BigDecimal price, RecommendationContext context)
    {
        if (!context.hasPriceTag() || price == null) return 0D;
        double fit = 0D;
        double total = 0D;
        for (PricePreference preference : context.pricePreferences)
        {
            fit += preference.score / (1D + preference.distance(price.doubleValue()) / 10D);
            total += preference.score;
        }
        return total <= 0D ? 0D : fit / total;
    }

    private boolean hasPositiveScore(List<? extends ScoredItem<?>> values)
    {
        for (ScoredItem<?> item : values)
        {
            if (item.score > 0D) return true;
        }
        return false;
    }

    private <T> Comparator<ScoredItem<T>> scoreComparator()
    {
        return Comparator.comparingDouble((ScoredItem<T> item) -> -item.score)
            .thenComparingInt(item -> item.originalIndex);
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
        private final List<PricePreference> pricePreferences = new ArrayList<>();
        private double preferenceTotal;
        private RecommendationIntent intent;

        private boolean hasPriceTag()
        {
            return !pricePreferences.isEmpty();
        }

        private boolean hasIntent()
        {
            return intent != null && intent.hasSignals();
        }

        private boolean hasAnySignal()
        {
            return preferenceTotal > 0D || hasPriceTag() || hasIntent();
        }
    }

    private static class PricePreference
    {
        private final double min;
        private final double max;
        private final double score;

        PricePreference(double min, double max, double score)
        {
            this.min = min;
            this.max = max;
            this.score = score;
        }

        private double distance(double price)
        {
            if (price < min) return min - price;
            if (price > max) return price - max;
            return 0D;
        }
    }

    private static class ScoredItem<T>
    {
        private final T value;
        private final double score;
        private final int originalIndex;

        ScoredItem(T value, double score, int originalIndex)
        {
            this.value = value;
            this.score = score;
            this.originalIndex = originalIndex;
        }
    }
}
