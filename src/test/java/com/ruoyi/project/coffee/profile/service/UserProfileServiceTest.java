package com.ruoyi.project.coffee.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.project.coffee.profile.domain.ProfileEvidence;
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
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(NOW, ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void recalculateBuildsMallTagsWithOneFourEightWeights()
    {
        when(userProfileMapper.selectBehaviorEvidence(eq(7L), any(Date.class))).thenReturn(Arrays.asList(
            evidence("MALL", "PRODUCT_VIEW", 101L, 11L, 40),
            evidence("MALL", "CART_ADD", 101L, 11L, 40)));
        when(userProfileMapper.selectPurchaseEvidence(eq(7L), any(Date.class)))
            .thenReturn(Collections.singletonList(evidence("MALL", "PURCHASE", 101L, 11L, 10)));

        UserProfile result = service.recalculateUser(7L);

        JSONObject tags = JSON.parseObject(result.getProfileData()).getJSONObject("MALL");
        JSONObject product = tags.getJSONArray("tags").stream().map(value -> (JSONObject) value)
            .filter(value -> "product:101".equals(value.getString("key"))).findFirst().get();
        assertEquals("product:101", product.getString("key"));
        assertEquals(new BigDecimal("11.00"), product.getBigDecimal("score"));
        assertEquals("READY", result.getProfileStatus());
        verify(userProfileMapper).upsertUserProfile(result);
    }

    @Test
    void scanCartAndOrderAddSelectedSpecTagsOnly()
    {
        ProfileEvidence cart = evidence("SCAN", "CART_ADD", 201L, 2L, 1);
        cart.setProductType("COFFEE");
        cart.setSpecJson("[{\"specName\":\"温度\",\"optionNames\":[\"冰\"]},"
            + "{\"specName\":\"糖度\",\"optionNames\":[\"少糖\"]}]");
        when(userProfileMapper.selectBehaviorEvidence(eq(8L), any(Date.class)))
            .thenReturn(Collections.singletonList(cart));

        UserProfile profile = service.recalculateUser(8L);
        JSONObject scan = JSON.parseObject(profile.getProfileData()).getJSONObject("SCAN");

        assertEquals("LEARNING", profile.getProfileStatus());
        assertEquals(1, scan.getJSONArray("tags").stream()
            .filter(value -> "temperature:冰".equals(((JSONObject) value).getString("key"))).count());
        assertEquals(1, scan.getJSONArray("tags").stream()
            .filter(value -> "sugar:少糖".equals(((JSONObject) value).getString("key"))).count());
    }

    @Test
    void thirtyDaysAndOneHourUsesSixtyPercentDecay()
    {
        ProfileEvidence evidence = evidence("MALL", "PRODUCT_VIEW", 101L, 11L, 30);
        evidence.setEvidenceTime(Date.from(NOW.minus(30, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS)));
        when(userProfileMapper.selectBehaviorEvidence(eq(9L), any(Date.class)))
            .thenReturn(Collections.singletonList(evidence));

        JSONObject data = JSON.parseObject(service.recalculateUser(9L).getProfileData());
        assertEquals(new BigDecimal("0.60"),
            data.getJSONObject("MALL").getJSONArray("tags").getJSONObject(0).getBigDecimal("score"));
    }

    @Test
    void buildsPriceTagPerScene()
    {
        ProfileEvidence mall = evidence("MALL", "PURCHASE", 101L, 11L, 1);
        mall.setPrice(new BigDecimal("85.00"));
        ProfileEvidence scan = evidence("SCAN", "PURCHASE", 201L, 2L, 1);
        scan.setPrice(new BigDecimal("25.00"));
        when(userProfileMapper.selectPurchaseEvidence(eq(12L), any(Date.class)))
            .thenReturn(Arrays.asList(mall, scan));

        JSONObject data = JSON.parseObject(service.recalculateUser(12L).getProfileData());

        JSONObject mallPrice = priceTag(data.getJSONObject("MALL").getJSONArray("tags"));
        JSONObject scanPrice = priceTag(data.getJSONObject("SCAN").getJSONArray("tags"));
        assertEquals(new BigDecimal("80.00"), mallPrice.getBigDecimal("min"));
        assertEquals(new BigDecimal("89.99"), mallPrice.getBigDecimal("max"));
        assertEquals(new BigDecimal("8.00"), mallPrice.getBigDecimal("score"));
        assertEquals(new BigDecimal("20.00"), scanPrice.getBigDecimal("min"));
        assertEquals(new BigDecimal("29.99"), scanPrice.getBigDecimal("max"));
    }

    @Test
    void failedUpsertIsPropagated()
    {
        when(userProfileMapper.upsertUserProfile(any(UserProfile.class)))
            .thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> service.recalculateUser(10L));
    }

    @Test
    void noEvidenceIsEmptyAndThreeBehaviorsAreReady()
    {
        assertEquals("EMPTY", service.recalculateUser(13L).getProfileStatus());
        when(userProfileMapper.selectBehaviorEvidence(eq(14L), any(Date.class))).thenReturn(Arrays.asList(
            evidence("MALL", "PRODUCT_VIEW", 101L, 11L, 1),
            evidence("MALL", "PRODUCT_VIEW", 102L, 11L, 1),
            evidence("MALL", "CART_ADD", 103L, 11L, 1)));
        assertEquals("READY", service.recalculateUser(14L).getProfileStatus());
    }

    private ProfileEvidence evidence(String scene, String type, Long productId, Long categoryId, long daysAgo)
    {
        ProfileEvidence evidence = new ProfileEvidence();
        evidence.setScene(scene);
        evidence.setEvidenceType(type);
        evidence.setProductId(productId);
        evidence.setProductName("商品");
        evidence.setCategoryId(categoryId);
        evidence.setCategoryName("分类");
        evidence.setOrigin("埃塞俄比亚");
        evidence.setEvidenceTime(dateDaysAgo(daysAgo));
        evidence.setPrice(new BigDecimal("25.00"));
        return evidence;
    }

    private JSONObject priceTag(com.alibaba.fastjson.JSONArray tags)
    {
        return tags.stream().map(value -> (JSONObject) value)
            .filter(value -> "price".equals(value.getString("dimension"))).findFirst().get();
    }

    private Date dateDaysAgo(long days)
    {
        return Date.from(NOW.minus(days, ChronoUnit.DAYS));
    }
}
