package com.ruoyi.project.coffee.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.project.coffee.profile.domain.ProfileEvidence;
import com.ruoyi.project.coffee.profile.domain.ProfileOrderSummary;
import com.ruoyi.project.coffee.profile.domain.UserProfile;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;

class UserProfileServiceTest
{
    private static final Instant NOW = Instant.parse("2026-08-07T03:00:00Z");

    @Mock
    private UserProfileMapper userProfileMapper;

    private UserProfileService service;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new UserProfileService();
        ReflectionTestUtils.setField(service, "userProfileMapper", userProfileMapper);
        ReflectionTestUtils.setField(service, "clock",
            Clock.fixed(NOW, ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void recalculateBuildsWeightedProfileAndOrderSummary()
    {
        ProfileOrderSummary summary = new ProfileOrderSummary();
        summary.setOrderCount(2);
        summary.setTotalAmount(new BigDecimal("68.00"));
        summary.setLastOrderTime(dateDaysAgo(10));
        when(userProfileMapper.selectOrderSummary(7L)).thenReturn(summary);
        when(userProfileMapper.countRecentBehaviorEvents(eq(7L), any(Date.class))).thenReturn(2);
        when(userProfileMapper.selectLastBehaviorTime(7L)).thenReturn(dateDaysAgo(2));
        when(userProfileMapper.selectBehaviorEvidence(eq(7L), any(Date.class))).thenReturn(Arrays.asList(
            evidence("MALL", "CART_ADD", 101L, "拿铁", 11L, "奶咖", "32.00", 40),
            evidence("MALL", "PRODUCT_VIEW", 101L, "拿铁", 11L, "奶咖", "32.00", 100)));
        when(userProfileMapper.selectPurchaseEvidence(eq(7L), any(Date.class))).thenReturn(
            Collections.singletonList(
                evidence("MALL", "PURCHASE", 101L, "拿铁", 11L, "奶咖", "32.00", 10)));

        UserProfile result = service.recalculateUser(7L);

        assertEquals("READY", result.getProfileStatus());
        assertEquals(3, result.getEvidenceCount());
        assertEquals(new BigDecimal("68.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("34.00"), result.getAvgOrderAmount());
        assertEquals(new BigDecimal("30.00"), result.getPreferredPriceMin());
        assertEquals(new BigDecimal("39.99"), result.getPreferredPriceMax());

        JSONObject profileData = JSON.parseObject(result.getProfileData());
        JSONArray products = profileData.getJSONObject("MALL").getJSONArray("products");
        assertEquals(1, products.size());
        assertEquals(new BigDecimal("7.10"), products.getJSONObject(0).getBigDecimal("score"));
        assertEquals(3, products.getJSONObject(0).getIntValue("evidenceCount"));
        assertEquals(0, profileData.getJSONObject("SCAN").getJSONArray("products").size());

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileMapper).upsertUserProfile(captor.capture());
        assertEquals(result, captor.getValue());
    }

    @Test
    void searchOnlyActivityRemainsLearningAndDoesNotCreateInterestEvidence()
    {
        when(userProfileMapper.countRecentBehaviorEvents(eq(8L), any(Date.class))).thenReturn(1);

        UserProfile profile = service.recalculateUser(8L);

        assertEquals("LEARNING", profile.getProfileStatus());
        assertEquals(0, profile.getEvidenceCount());
        assertNull(profile.getPreferredPriceMin());
    }

    @Test
    void userWithoutOrderOrBehaviorGetsEmptyProfile()
    {
        UserProfile profile = service.recalculateUser(9L);

        assertEquals("EMPTY", profile.getProfileStatus());
        assertEquals(0, profile.getOrderCount());
        assertEquals(new BigDecimal("0.00"), profile.getTotalAmount());
    }

    @Test
    void failedUpsertIsReportedToTaskAndCannotReplaceOldProfileEarly()
    {
        when(userProfileMapper.upsertUserProfile(any(UserProfile.class)))
            .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> service.recalculateUser(10L));
        verify(userProfileMapper).upsertUserProfile(any(UserProfile.class));
    }

    @Test
    void exactlyThreePositiveBehaviorsMakeProfileReady()
    {
        when(userProfileMapper.countRecentBehaviorEvents(eq(11L), any(Date.class))).thenReturn(3);
        when(userProfileMapper.selectBehaviorEvidence(eq(11L), any(Date.class))).thenReturn(Arrays.asList(
            evidence("MALL", "PRODUCT_VIEW", 101L, "拿铁", 11L, "奶咖", "25.00", 1),
            evidence("MALL", "PRODUCT_VIEW", 102L, "美式", 12L, "黑咖", "18.00", 2),
            evidence("MALL", "CART_ADD", 103L, "摩卡", 11L, "奶咖", "28.00", 3)));

        assertEquals("READY", service.recalculateUser(11L).getProfileStatus());
    }

    @Test
    void evidenceOlderThanThirtyExactDaysUsesSixtyPercentDecay()
    {
        when(userProfileMapper.countRecentBehaviorEvents(eq(12L), any(Date.class))).thenReturn(1);
        ProfileEvidence evidence = evidence(
            "MALL", "PRODUCT_VIEW", 101L, "拿铁", 11L, "奶咖", "25.00", 30);
        evidence.setEvidenceTime(Date.from(NOW.minus(30, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS)));
        when(userProfileMapper.selectBehaviorEvidence(eq(12L), any(Date.class)))
            .thenReturn(Collections.singletonList(evidence));

        JSONObject data = JSON.parseObject(service.recalculateUser(12L).getProfileData());
        assertEquals(new BigDecimal("0.60"),
            data.getJSONObject("MALL").getJSONArray("products").getJSONObject(0).getBigDecimal("score"));
    }

    private ProfileEvidence evidence(String scene, String type, Long productId, String productName,
        Long categoryId, String categoryName, String price, long daysAgo)
    {
        ProfileEvidence evidence = new ProfileEvidence();
        evidence.setScene(scene);
        evidence.setEvidenceType(type);
        evidence.setProductId(productId);
        evidence.setProductName(productName);
        evidence.setCategoryId(categoryId);
        evidence.setCategoryName(categoryName);
        evidence.setPrice(new BigDecimal(price));
        evidence.setEvidenceTime(dateDaysAgo(daysAgo));
        return evidence;
    }

    private Date dateDaysAgo(long days)
    {
        return Date.from(NOW.minus(days, ChronoUnit.DAYS));
    }
}
