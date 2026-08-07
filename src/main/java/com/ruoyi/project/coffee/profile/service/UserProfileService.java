package com.ruoyi.project.coffee.profile.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.project.coffee.profile.domain.ProfileEvidence;
import com.ruoyi.project.coffee.profile.domain.ProfileOrderSummary;
import com.ruoyi.project.coffee.profile.domain.UserProfile;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;

/** 依据最近 180 天证据生成统一用户画像。 */
@Service
public class UserProfileService
{
    static final int RETENTION_DAYS = 180;
    private static final int READY_EVIDENCE_COUNT = 3;
    private static final int MAX_CATEGORIES = 5;
    private static final int MAX_PRODUCTS = 20;
    private static final BigDecimal PRICE_BUCKET_SIZE = new BigDecimal("10");

    @Autowired
    private UserProfileMapper userProfileMapper;

    private Clock clock = Clock.systemDefaultZone();

    public UserProfile recalculateUser(Long userId)
    {
        if (userId == null)
        {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        Date now = Date.from(clock.instant());
        Date cutoffTime = Date.from(clock.instant().minus(Duration.ofDays(RETENTION_DAYS)));
        ProfileOrderSummary orderSummary = userProfileMapper.selectOrderSummary(userId);
        if (orderSummary == null)
        {
            orderSummary = new ProfileOrderSummary();
        }

        List<ProfileEvidence> behaviorEvidence = safeList(
            userProfileMapper.selectBehaviorEvidence(userId, cutoffTime));
        List<ProfileEvidence> purchaseEvidence = safeList(
            userProfileMapper.selectPurchaseEvidence(userId, cutoffTime));
        List<ProfileEvidence> allEvidence = new ArrayList<>(behaviorEvidence.size() + purchaseEvidence.size());
        allEvidence.addAll(behaviorEvidence);
        allEvidence.addAll(purchaseEvidence);

        int orderCount = valueOrZero(orderSummary.getOrderCount());
        BigDecimal totalAmount = moneyOrZero(orderSummary.getTotalAmount());
        int recentActivityCount = userProfileMapper.countRecentBehaviorEvents(userId, cutoffTime);

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setOrderCount(orderCount);
        profile.setTotalAmount(totalAmount);
        profile.setAvgOrderAmount(orderCount == 0
            ? BigDecimal.ZERO.setScale(2)
            : totalAmount.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP));
        profile.setLastOrderTime(orderSummary.getLastOrderTime());
        profile.setLastActiveTime(userProfileMapper.selectLastBehaviorTime(userId));
        profile.setEvidenceCount(allEvidence.size());
        profile.setProfileStatus(resolveStatus(orderCount, recentActivityCount, behaviorEvidence.size()));
        profile.setProfileData(buildProfileData(allEvidence, now));
        setPreferredPrice(profile, allEvidence, now);
        profile.setCalculateTime(now);
        profile.setCreateTime(now);
        profile.setUpdateTime(now);

        userProfileMapper.upsertUserProfile(profile);
        return profile;
    }

    public List<Long> selectChangedUserIds()
    {
        return safeLongList(userProfileMapper.selectChangedUserIds());
    }

    public List<Long> selectAllUserIds()
    {
        return safeLongList(userProfileMapper.selectAllUserIds());
    }

    public int deleteExpiredBehavior()
    {
        Date cutoffTime = Date.from(clock.instant().minus(Duration.ofDays(RETENTION_DAYS)));
        return userProfileMapper.deleteExpiredBehavior(cutoffTime);
    }

    private String resolveStatus(int orderCount, int recentActivityCount, int positiveBehaviorCount)
    {
        if (orderCount == 0 && recentActivityCount == 0)
        {
            return "EMPTY";
        }
        if (orderCount > 0 || positiveBehaviorCount >= READY_EVIDENCE_COUNT)
        {
            return "READY";
        }
        return "LEARNING";
    }

    private String buildProfileData(List<ProfileEvidence> evidenceList, Date now)
    {
        JSONObject root = new JSONObject(true);
        root.put("MALL", buildSceneData("MALL", evidenceList, now));
        root.put("SCAN", buildSceneData("SCAN", evidenceList, now));
        return JSON.toJSONString(root);
    }

    private JSONObject buildSceneData(String scene, List<ProfileEvidence> evidenceList, Date now)
    {
        Map<Long, PreferenceAccumulator> categories = new HashMap<>();
        Map<Long, PreferenceAccumulator> products = new HashMap<>();
        for (ProfileEvidence evidence : evidenceList)
        {
            if (evidence == null || !scene.equals(evidence.getScene()) || evidence.getProductId() == null)
            {
                continue;
            }
            BigDecimal score = score(evidence, now);
            if (score.signum() <= 0)
            {
                continue;
            }
            addEvidence(products, evidence.getProductId(), evidence.getProductName(), score,
                evidence.getEvidenceTime());
            if (evidence.getCategoryId() != null)
            {
                addEvidence(categories, evidence.getCategoryId(), evidence.getCategoryName(), score,
                    evidence.getEvidenceTime());
            }
        }

        JSONObject sceneData = new JSONObject(true);
        sceneData.put("categories", toJsonArray(categories, MAX_CATEGORIES));
        sceneData.put("products", toJsonArray(products, MAX_PRODUCTS));
        return sceneData;
    }

    private void addEvidence(Map<Long, PreferenceAccumulator> target, Long id, String name,
        BigDecimal score, Date evidenceTime)
    {
        PreferenceAccumulator accumulator = target.get(id);
        if (accumulator == null)
        {
            accumulator = new PreferenceAccumulator(id, name);
            target.put(id, accumulator);
        }
        else if (name != null && !name.trim().isEmpty())
        {
            accumulator.name = name;
        }
        accumulator.score = accumulator.score.add(score);
        accumulator.evidenceCount++;
        if (evidenceTime != null
            && (accumulator.lastEvidenceTime == null || evidenceTime.after(accumulator.lastEvidenceTime)))
        {
            accumulator.lastEvidenceTime = evidenceTime;
        }
    }

    private JSONArray toJsonArray(Map<Long, PreferenceAccumulator> values, int limit)
    {
        List<PreferenceAccumulator> sorted = new ArrayList<>(values.values());
        sorted.sort(Comparator.comparing((PreferenceAccumulator item) -> item.score).reversed()
            .thenComparing(item -> item.lastEvidenceTime, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(item -> item.id));

        JSONArray result = new JSONArray();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < sorted.size() && i < limit; i++)
        {
            PreferenceAccumulator item = sorted.get(i);
            JSONObject json = new JSONObject(true);
            json.put("id", item.id);
            json.put("name", item.name);
            json.put("score", item.score.setScale(2, RoundingMode.HALF_UP));
            json.put("evidenceCount", item.evidenceCount);
            json.put("lastEvidenceTime", item.lastEvidenceTime == null
                ? null : dateFormat.format(item.lastEvidenceTime));
            result.add(json);
        }
        return result;
    }

    private void setPreferredPrice(UserProfile profile, List<ProfileEvidence> evidenceList, Date now)
    {
        Map<BigDecimal, BigDecimal> buckets = new HashMap<>();
        for (ProfileEvidence evidence : evidenceList)
        {
            if (evidence == null || evidence.getPrice() == null || evidence.getPrice().signum() < 0)
            {
                continue;
            }
            BigDecimal score = score(evidence, now);
            if (score.signum() <= 0)
            {
                continue;
            }
            BigDecimal bucketStart = evidence.getPrice()
                .divide(PRICE_BUCKET_SIZE, 0, RoundingMode.FLOOR)
                .multiply(PRICE_BUCKET_SIZE)
                .setScale(2);
            buckets.put(bucketStart, buckets.getOrDefault(bucketStart, BigDecimal.ZERO).add(score));
        }

        BigDecimal preferredBucket = null;
        BigDecimal preferredScore = null;
        for (Map.Entry<BigDecimal, BigDecimal> entry : buckets.entrySet())
        {
            if (preferredScore == null || entry.getValue().compareTo(preferredScore) > 0
                || (entry.getValue().compareTo(preferredScore) == 0
                    && entry.getKey().compareTo(preferredBucket) < 0))
            {
                preferredBucket = entry.getKey();
                preferredScore = entry.getValue();
            }
        }
        if (preferredBucket != null)
        {
            profile.setPreferredPriceMin(preferredBucket);
            profile.setPreferredPriceMax(preferredBucket.add(new BigDecimal("9.99")));
        }
    }

    private BigDecimal score(ProfileEvidence evidence, Date now)
    {
        return baseWeight(evidence.getEvidenceType()).multiply(decay(evidence.getEvidenceTime(), now));
    }

    private BigDecimal baseWeight(String evidenceType)
    {
        if ("PURCHASE".equals(evidenceType))
        {
            return new BigDecimal("5");
        }
        if ("CART_ADD".equals(evidenceType))
        {
            return new BigDecimal("3");
        }
        if ("PRODUCT_VIEW".equals(evidenceType))
        {
            return BigDecimal.ONE;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal decay(Date evidenceTime, Date now)
    {
        if (evidenceTime == null)
        {
            return BigDecimal.ZERO;
        }
        long ageMillis = Math.max(0, now.getTime() - evidenceTime.getTime());
        if (ageMillis <= Duration.ofDays(30).toMillis())
        {
            return BigDecimal.ONE;
        }
        if (ageMillis <= Duration.ofDays(90).toMillis())
        {
            return new BigDecimal("0.6");
        }
        if (ageMillis <= Duration.ofDays(RETENTION_DAYS).toMillis())
        {
            return new BigDecimal("0.3");
        }
        return BigDecimal.ZERO;
    }

    private int valueOrZero(Integer value)
    {
        return value == null ? 0 : value;
    }

    private BigDecimal moneyOrZero(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private List<ProfileEvidence> safeList(List<ProfileEvidence> values)
    {
        return values == null ? Collections.emptyList() : values;
    }

    private List<Long> safeLongList(List<Long> values)
    {
        return values == null ? Collections.emptyList() : values;
    }

    private static class PreferenceAccumulator
    {
        private final Long id;
        private String name;
        private BigDecimal score = BigDecimal.ZERO;
        private int evidenceCount;
        private Date lastEvidenceTime;

        PreferenceAccumulator(Long id, String name)
        {
            this.id = id;
            this.name = name;
        }
    }
}
