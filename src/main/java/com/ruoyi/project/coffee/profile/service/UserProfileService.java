package com.ruoyi.project.coffee.profile.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.project.coffee.profile.domain.ProfileEvidence;
import com.ruoyi.project.coffee.profile.domain.ProfileTag;
import com.ruoyi.project.coffee.profile.domain.UserProfile;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;

/** 依据全部有效历史证据生成统一用户画像。 */
@Service
public class UserProfileService
{
    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);
    private static final int READY_EVIDENCE_COUNT = 3;
    private static final BigDecimal PRICE_BUCKET_SIZE = new BigDecimal("10");

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired(required = false)
    @Qualifier("threadPoolTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    private Clock clock = Clock.systemDefaultZone();

    /**
     * 在当前事务提交后异步刷新画像，避免读取到尚未提交的订单或行为证据。
     */
    public void recalculateAsync(Long userId)
    {
        if (userId == null || taskExecutor == null)
        {
            return;
        }
        Runnable refresh = () -> {
            try
            {
                recalculateUser(userId);
            }
            catch (Exception e)
            {
                log.warn("异步刷新用户画像失败, userId={}", userId, e);
            }
        };

        try
        {
            if (TransactionSynchronizationManager.isSynchronizationActive())
            {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
                {
                    @Override
                    public void afterCommit()
                    {
                        submitRefresh(userId, refresh);
                    }
                });
                return;
            }
            submitRefresh(userId, refresh);
        }
        catch (Exception e)
        {
            log.warn("注册用户画像异步刷新失败, userId={}", userId, e);
        }
    }

    private void submitRefresh(Long userId, Runnable refresh)
    {
        try
        {
            taskExecutor.execute(refresh);
        }
        catch (Exception e)
        {
            log.warn("提交用户画像异步刷新失败, userId={}", userId, e);
        }
    }

    public UserProfile recalculateUser(Long userId)
    {
        if (userId == null)
        {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        Date now = Date.from(clock.instant());
        List<ProfileEvidence> behaviorEvidence = safeList(
            userProfileMapper.selectBehaviorEvidence(userId));
        List<ProfileEvidence> purchaseEvidence = safeList(
            userProfileMapper.selectPurchaseEvidence(userId));
        List<ProfileEvidence> allEvidence = new ArrayList<>(behaviorEvidence.size() + purchaseEvidence.size());
        allEvidence.addAll(behaviorEvidence);
        allEvidence.addAll(purchaseEvidence);

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setProfileStatus(resolveStatus(behaviorEvidence.size(), purchaseEvidence.size()));
        profile.setProfileData(buildProfileData(allEvidence));
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

    private String buildProfileData(List<ProfileEvidence> evidenceList)
    {
        JSONObject root = new JSONObject(true);
        root.put("MALL", buildSceneData("MALL", evidenceList));
        root.put("SCAN", buildSceneData("SCAN", evidenceList));
        return JSON.toJSONString(root);
    }

    private JSONObject buildSceneData(String scene, List<ProfileEvidence> evidenceList)
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
            BigDecimal score = score(evidence);
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

        normalizeByDimension(tags);
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
            json.put("score", item.normalizedScore.setScale(4, RoundingMode.HALF_UP));
            json.put("rawScore", item.score.setScale(2, RoundingMode.HALF_UP));
            json.put("evidenceCount", item.evidenceCount);
            json.put("lastEvidenceTime", item.lastEvidenceTime == null
                ? null : dateFormat.format(item.lastEvidenceTime));
            result.add(json);
        }
        addPriceTags(result, sceneEvidence);
        JSONObject sceneData = new JSONObject(true);
        sceneData.put("tags", result);
        return sceneData;
    }

    private void normalizeByDimension(Map<String, PreferenceAccumulator> tags)
    {
        Map<String, BigDecimal> totals = new HashMap<>();
        for (PreferenceAccumulator item : tags.values())
        {
            totals.put(item.dimension, totals.getOrDefault(item.dimension, BigDecimal.ZERO).add(item.score));
        }
        for (PreferenceAccumulator item : tags.values())
        {
            BigDecimal total = totals.get(item.dimension);
            item.normalizedScore = total == null || total.signum() <= 0
                ? BigDecimal.ZERO : item.score.divide(total, 8, RoundingMode.HALF_UP);
        }
    }

    private void addPriceTags(JSONArray tags, List<ProfileEvidence> evidenceList)
    {
        Map<BigDecimal, BigDecimal> buckets = new HashMap<>();
        for (ProfileEvidence evidence : evidenceList)
        {
            if (evidence == null || evidence.getPrice() == null || evidence.getPrice().signum() < 0)
            {
                continue;
            }
            BigDecimal score = score(evidence);
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

        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal score : buckets.values())
        {
            total = total.add(score);
        }
        if (total.signum() <= 0)
        {
            return;
        }
        List<Map.Entry<BigDecimal, BigDecimal>> sorted = new ArrayList<>(buckets.entrySet());
        sorted.sort(Map.Entry.comparingByKey());
        for (Map.Entry<BigDecimal, BigDecimal> entry : sorted)
        {
            BigDecimal bucket = entry.getKey();
            BigDecimal max = bucket.add(new BigDecimal("9.99"));
            JSONObject priceTag = new JSONObject(true);
            priceTag.put("key", "price:" + bucket.toPlainString() + "-" + max.toPlainString());
            priceTag.put("dimension", "price");
            priceTag.put("name", bucket.toPlainString() + "-" + max.toPlainString());
            priceTag.put("score", entry.getValue().divide(total, 8, RoundingMode.HALF_UP)
                .setScale(4, RoundingMode.HALF_UP));
            priceTag.put("rawScore", entry.getValue().setScale(2, RoundingMode.HALF_UP));
            priceTag.put("min", bucket);
            priceTag.put("max", max);
            tags.add(priceTag);
        }
    }

    private BigDecimal score(ProfileEvidence evidence)
    {
        if (evidence == null || evidence.getEvidenceTime() == null)
        {
            return BigDecimal.ZERO;
        }
        return baseWeight(evidence.getEvidenceType());
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
        private BigDecimal normalizedScore = BigDecimal.ZERO;
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
