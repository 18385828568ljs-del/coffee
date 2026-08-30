package com.ruoyi.project.coffee.profile.domain;

import java.util.HashMap;
import java.util.Map;

/** 当前请求内的短期用户意图，不写入长期用户画像。 */
public class RecommendationIntent
{
    private final Map<Long, Double> productScores = new HashMap<>();
    private final Map<String, Double> tagScores = new HashMap<>();

    public void addCurrentProduct(Long productId)
    {
        addProduct(productId, 3D);
    }

    public void addCartProduct(Long productId)
    {
        addProduct(productId, 5D);
    }

    public void addProduct(Long productId, double score)
    {
        if (productId != null && score > 0D)
        {
            productScores.merge(productId, score, Double::sum);
        }
    }

    public void addTag(String key, double score)
    {
        if (key != null && !key.trim().isEmpty() && score > 0D)
        {
            tagScores.merge(key, score, Double::sum);
        }
    }

    public Map<Long, Double> getProductScores()
    {
        return productScores;
    }

    public Map<String, Double> getTagScores()
    {
        return tagScores;
    }

    public boolean hasSignals()
    {
        return !productScores.isEmpty() || !tagScores.isEmpty();
    }

    public double totalScore()
    {
        return productScores.values().stream().mapToDouble(Double::doubleValue).sum()
            + tagScores.values().stream().mapToDouble(Double::doubleValue).sum();
    }

}
