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
import com.ruoyi.project.coffee.profile.domain.ProfileTag;
import com.ruoyi.project.coffee.profile.domain.UserProfile;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;

/** 依据最近 180 天证据生成统一用户画像。 */
@Service
public class UserProfileService
{
    static final int RETENTION_DAYS = 180;
    private static final int READY_EVIDENCE_COUNT = 3;
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
        List<ProfileEvidence> behaviorEvidence = safeList(
            userProfileMapper.selectBehaviorEvidence(userId, cutoffTime));
        List<ProfileEvidence> purchaseEvidence = safeList(
            userProfileMapper.selectPurchaseEvidence(userId, cutoffTime));
        List<ProfileEvidence> allEvidence = new ArrayList<>(behaviorEvidence.size() + purchaseEvidence.size());
        allEvidence.addAll(behaviorEvidence);
        allEvidence.addAll(purchaseEvidence);

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setProfileStatus(resolveStatus(behaviorEvidence.size(), purchaseEvidence.size()));
        profile.setProfileData(buildProfileData(allEvidence, now));
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

    private String resolveStatus(int behaviorCount, int purchaseCount)
    {
        if (behaviorCount == 0 && purchaseCount == 0)
        {
            return "EMPTY";
        }
        if (purchaseCount > 0 || behaviorCount >= READY_EVIDENCE_COUNT)
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
        Map<String, PreferenceAccumulator> tags = new HashMap<>();
        List<ProfileEvidence> sceneEvidence = new ArrayList<>();
        for (ProfileEvidence evidence : evidenceList)
        {
            if (evidence == null || !scene.equals(evidence.getScene()))
            {
                continue;
            }
            sceneEvidence.add(evidence);
            BigDecimal score = score(evidence, now);
            if (score.signum() <= 0)
            {
                continue;
            }
            List<ProfileTag> evidenceTags = "MALL".equals(scene)
                ? ProfileTagUtils.mall(evidence) : ProfileTagUtils.scanBase(evidence);
            if ("SCAN".equals(scene) && !"PRODUCT_VIEW".equals(evidence.getEvidenceType()))
            {
                evidenceTags = new ArrayList<>(evidenceTags);
                evidenceTags.addAll(ProfileTagUtils.selectedScanSpecs(evidence.getSpecJson()));
            }
            for (ProfileTag tag : evidenceTags)
            {
                PreferenceAccumulator accumulator = tags.get(tag.getKey());
                if (accumulator == null)
                {
                    accumulator = new PreferenceAccumulator(tag.getKey(), tag.getDimension(), tag.getName());
                    tags.put(tag.getKey(), accumulator);
                }
                accumulator.score = accumulator.score.add(score);
                accumulator.evidenceCount++;
                if (evidence.getEvidenceTime() != null
                    && (accumulator.lastEvidenceTime == null
                        || evidence.getEvidenceTime().after(accumulator.lastEvidenceTime)))
                {
                    accumulator.lastEvidenceTime = evidence.getEvidenceTime();
                }
            }
        }

        List<PreferenceAccumulator> sorted = new ArrayList<>(tags.values());
        sorted.sort(Comparator.comparing((PreferenceAccumulator item) -> item.score).reversed()
            .thenComparing(item -> item.lastEvidenceTime, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(item -> item.key));
        JSONArray result = new JSONArray();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (PreferenceAccumulator item : sorted)
        {
            JSONObject json = new JSONObject(true);
            json.put("key", item.key);
            json.put("dimension", item.dimension);
            json.put("name", item.name);
            json.put("score", item.score.setScale(2, RoundingMode.HALF_UP));
            json.put("evidenceCount", item.evidenceCount);
            json.put("lastEvidenceTime", item.lastEvidenceTime == null
                ? null : dateFormat.format(item.lastEvidenceTime));
            result.add(json);
        }
        addPriceTag(result, sceneEvidence, now);
        JSONObject sceneData = new JSONObject(true);
        sceneData.put("tags", result);
        return sceneData;
    }

    private void addPriceTag(JSONArray tags, List<ProfileEvidence> evidenceList, Date now)
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
        if (preferredBucket == null)
        {
            return;
        }

        BigDecimal preferredMax = preferredBucket.add(new BigDecimal("9.99"));
        JSONObject priceTag = new JSONObject(true);
        priceTag.put("key", "price:" + preferredBucket.toPlainString() + "-" + preferredMax.toPlainString());
        priceTag.put("dimension", "price");
        priceTag.put("name", preferredBucket.toPlainString() + "-" + preferredMax.toPlainString());
        priceTag.put("score", preferredScore.setScale(2, RoundingMode.HALF_UP));
        priceTag.put("min", preferredBucket);
        priceTag.put("max", preferredMax);
        tags.add(priceTag);
    }

    private BigDecimal score(ProfileEvidence evidence, Date now)
    {
        return baseWeight(evidence.getEvidenceType()).multiply(decay(evidence.getEvidenceTime(), now));
    }

    private BigDecimal baseWeight(String evidenceType)
    {
        if ("PURCHASE".equals(evidenceType))
        {
            return new BigDecimal("8");
        }
        if ("CART_ADD".equals(evidenceType))
        {
            return new BigDecimal("4");
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
        private final String key;
        private final String dimension;
        private String name;
        private BigDecimal score = BigDecimal.ZERO;
        private int evidenceCount;
        private Date lastEvidenceTime;

        PreferenceAccumulator(String key, String dimension, String name)
        {
            this.key = key;
            this.dimension = dimension;
            this.name = name;
        }
    }
}
