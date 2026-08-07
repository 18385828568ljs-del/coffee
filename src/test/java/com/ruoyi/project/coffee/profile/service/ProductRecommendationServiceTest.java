package com.ruoyi.project.coffee.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.profile.domain.ProductPopularity;
import com.ruoyi.project.coffee.profile.domain.RecommendedProduct;
import com.ruoyi.project.coffee.profile.domain.UserProfile;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;

class ProductRecommendationServiceTest
{
    private static final Instant NOW = Instant.parse("2026-08-07T03:00:00Z");

    @Mock
    private UserProfileMapper userProfileMapper;

    private ProductRecommendationService service;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new ProductRecommendationService();
        ReflectionTestUtils.setField(service, "userProfileMapper", userProfileMapper);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(NOW, ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void readyMallProfileRanksInterestedProductsAndRemovesOutOfStock()
    {
        when(userProfileMapper.selectUserProfileByUserId(7L)).thenReturn(profile("READY",
            "{\"MALL\":{\"products\":[{\"id\":101,\"score\":7}],"
                + "\"categories\":[{\"id\":11,\"score\":7}]},\"SCAN\":{}}"));
        ProductPopularity popularity = new ProductPopularity();
        popularity.setProductId(102L);
        popularity.setPopularity(10L);
        when(userProfileMapper.selectRecentProductPopularity(eq("MALL"), any(Date.class)))
            .thenReturn(Collections.singletonList(popularity));

        TProduct interested = product(101L, 11L, 20L, dateDaysAgo(5));
        TProduct popular = product(102L, 12L, 20L, dateDaysAgo(5));
        TProduct outOfStock = product(103L, 11L, 0L, dateDaysAgo(5));

        List<TProduct> result = service.recommendMall(7L, Arrays.asList(interested, popular, outOfStock));

        assertEquals(Arrays.asList(interested, popular), result);
        assertTrue(interested.getRecommendationApplied());
        assertTrue(popular.getRecommendationApplied());
        assertFalse(outOfStock.getRecommendationApplied());
        verify(userProfileMapper).selectRecentProductPopularity(eq("MALL"), any(Date.class));
    }

    @Test
    void learningProfileKeepsOriginalOrderWithoutPopularityQuery()
    {
        List<TProduct> products = Arrays.asList(product(101L, 11L, 20L, dateDaysAgo(5)),
            product(102L, 12L, 20L, dateDaysAgo(5)));
        when(userProfileMapper.selectUserProfileByUserId(8L)).thenReturn(profile("LEARNING", "{}"));

        List<TProduct> result = service.recommendMall(8L, products);

        assertSame(products, result);
        assertFalse(products.get(0).getRecommendationApplied());
        verify(userProfileMapper, never()).selectRecentProductPopularity(any(), any(Date.class));
    }

    @Test
    void mallExplanationUsesLargestRankingContribution()
    {
        when(userProfileMapper.selectUserProfileByUserId(10L)).thenReturn(profile("READY",
            "{\"MALL\":{\"products\":[{\"id\":101,\"score\":7}],\"categories\":[]},\"SCAN\":{}}"));
        when(userProfileMapper.selectRecentProductPopularity(eq("MALL"), any(Date.class)))
            .thenReturn(Collections.emptyList());
        TProduct preferred = product(101L, 11L, 20L, dateDaysAgo(60));
        preferred.setProductName("Preferred coffee");

        List<RecommendedProduct> result = service.explainMall(10L,
            Collections.singletonList(preferred), 5);

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getProductId());
        assertEquals("常购或感兴趣商品", result.get(0).getReason());
        assertEquals(0.55D, result.get(0).getScore(), 0.0001D);
    }

    @Test
    void readyScanProfileUsesScanSceneScores()
    {
        when(userProfileMapper.selectUserProfileByUserId(9L)).thenReturn(profile("READY",
            "{\"MALL\":{},\"SCAN\":{\"products\":[{\"id\":201,\"score\":4}]}}"));
        when(userProfileMapper.selectRecentProductPopularity(eq("SCAN"), any(Date.class)))
            .thenReturn(Collections.emptyList());

        ScanProduct preferred = scanProduct(201L);
        ScanProduct other = scanProduct(202L);

        assertEquals(Arrays.asList(preferred, other), service.recommendScan(9L, Arrays.asList(other, preferred)));
    }

    private UserProfile profile(String status, String data)
    {
        UserProfile profile = new UserProfile();
        profile.setProfileStatus(status);
        profile.setProfileData(data);
        profile.setPreferredPriceMin(new BigDecimal("20.00"));
        profile.setPreferredPriceMax(new BigDecimal("29.99"));
        return profile;
    }

    private TProduct product(Long productId, Long categoryId, Long stock, Date createTime)
    {
        TProduct product = new TProduct();
        product.setProductId(productId);
        product.setCategoryId(categoryId);
        product.setPrice(new BigDecimal("25.00"));
        product.setStock(stock);
        product.setCreateTime(createTime);
        return product;
    }

    private ScanProduct scanProduct(Long productId)
    {
        ScanProduct product = new ScanProduct();
        product.setProductId(productId);
        product.setPrice(new BigDecimal("25.00"));
        product.setCreateTime(dateDaysAgo(60));
        return product;
    }

    private Date dateDaysAgo(long days)
    {
        return Date.from(NOW.minusSeconds(days * 24L * 60L * 60L));
    }
}
