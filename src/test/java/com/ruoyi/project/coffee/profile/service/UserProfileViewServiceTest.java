package com.ruoyi.project.coffee.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.profile.domain.RecommendedProduct;
import com.ruoyi.project.coffee.profile.domain.UserProfile;
import com.ruoyi.project.coffee.profile.domain.UserProfileView;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;

class UserProfileViewServiceTest
{
    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private ITProductService productService;

    @Mock
    private ProductRecommendationService recommendationService;

    private UserProfileViewService service;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new UserProfileViewService();
        ReflectionTestUtils.setField(service, "userProfileMapper", userProfileMapper);
        ReflectionTestUtils.setField(service, "productService", productService);
        ReflectionTestUtils.setField(service, "recommendationService", recommendationService);
    }

    @Test
    void missingSnapshotReturnsEmptyWithoutLoadingProducts()
    {
        UserProfileView result = service.getProfileView(7L);

        assertEquals("EMPTY", result.getProfileStatus());
        assertTrue(result.getRecommendations().isEmpty());
        verify(productService, never()).selectTProductList(any(TProduct.class));
    }

    @Test
    void learningSnapshotDoesNotBuildRecommendations()
    {
        UserProfile profile = new UserProfile();
        profile.setUserId(9L);
        profile.setProfileStatus("LEARNING");
        profile.setProfileData("{}");
        when(userProfileMapper.selectUserProfileByUserId(9L)).thenReturn(profile);

        UserProfileView result = service.getProfileView(9L);

        assertEquals("LEARNING", result.getProfileStatus());
        verify(productService, never()).selectTProductList(any(TProduct.class));
        verify(recommendationService, never()).explainMall(any(UserProfile.class), any(), anyInt());
    }

    @Test
    void readySnapshotReturnsTopPreferencesAndRecommendations()
    {
        UserProfile profile = new UserProfile();
        profile.setUserId(8L);
        profile.setProfileStatus("READY");
        profile.setOrderCount(2);
        profile.setTotalAmount(new BigDecimal("88.00"));
        profile.setProfileData("{\"MALL\":{\"categories\":["
            + "{\"id\":11,\"name\":\"Coffee beans\",\"score\":9,\"evidenceCount\":3},"
            + "{\"id\":12,\"name\":\"Drip bags\",\"score\":7},"
            + "{\"id\":13,\"name\":\"Tools\",\"score\":5},"
            + "{\"id\":14,\"name\":\"Extra\",\"score\":1}],"
            + "\"products\":[{\"id\":101,\"name\":\"House blend\",\"score\":8}]}}");
        when(userProfileMapper.selectUserProfileByUserId(8L)).thenReturn(profile);
        TProduct candidate = new TProduct();
        when(productService.selectTProductList(any(TProduct.class)))
            .thenReturn(Collections.singletonList(candidate));
        RecommendedProduct recommendation = new RecommendedProduct();
        recommendation.setProductId(101L);
        when(recommendationService.explainMall(eq(profile), any(), eq(5)))
            .thenReturn(Collections.singletonList(recommendation));

        UserProfileView result = service.getProfileView(8L);

        assertEquals(3, result.getMallCategories().size());
        assertEquals("Coffee beans", result.getMallCategories().get(0).getName());
        assertEquals(1, result.getMallProducts().size());
        assertEquals(1, result.getRecommendations().size());
        verify(recommendationService).explainMall(eq(profile), any(), eq(5));
    }
}
