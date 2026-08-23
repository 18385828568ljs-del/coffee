package com.ruoyi.project.coffee.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.profile.domain.UserProfile;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpec;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecOptionMapper;

class ProductRecommendationServiceTest
{
    @Mock private UserProfileMapper userProfileMapper;
    @Mock private ScanProductSpecMapper scanProductSpecMapper;
    @Mock private ScanProductSpecOptionMapper scanProductSpecOptionMapper;
    private ProductRecommendationService service;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new ProductRecommendationService();
        ReflectionTestUtils.setField(service, "userProfileMapper", userProfileMapper);
        ReflectionTestUtils.setField(service, "scanProductSpecMapper", scanProductSpecMapper);
        ReflectionTestUtils.setField(service, "scanProductSpecOptionMapper", scanProductSpecOptionMapper);
    }

    @Test
    void mallSumsMatchingTagsAndIgnoresMarketingData()
    {
        when(userProfileMapper.selectUserProfileByUserId(7L)).thenReturn(profile("READY",
            profileData("MALL", "category:11", "category", 8,
                "origin:埃塞俄比亚", "origin", 12)));
        TProduct preferred = product(101L, 11L, "埃塞俄比亚");
        TProduct other = product(102L, 12L, "巴西");
        other.setPrice(new BigDecimal("1.00"));
        other.setActivityTag("新品");

        List<TProduct> result = service.recommendMall(7L, Arrays.asList(other, preferred));

        assertEquals(Arrays.asList(preferred, other), result);
    }

    @Test
    void priceTagRanksAllProductsByDistanceWithoutFiltering()
    {
        UserProfile profile = profile("READY", profileDataWithPriceTag("MALL", "origin:其他", "origin", 1,
            new BigDecimal("20.00"), new BigDecimal("29.99")));
        when(userProfileMapper.selectUserProfileByUserId(11L)).thenReturn(profile);

        TProduct inRange = product(111L, 11L, "无匹配产地");
        inRange.setPrice(new BigDecimal("25.00"));
        TProduct outOfRange = product(112L, 12L, "无匹配产地");
        outOfRange.setPrice(new BigDecimal("40.00"));
        TProduct farther = product(115L, 12L, "other");
        farther.setPrice(new BigDecimal("60.00"));

        assertEquals(Arrays.asList(inRange, outOfRange, farther),
            service.recommendMall(11L, Arrays.asList(farther, outOfRange, inRange)));
    }

    @Test
    void readsPriceRangeFromUnifiedTags()
    {
        UserProfile profile = profile("READY", profileDataWithPriceTag("MALL", "origin:其他", "origin", 1,
            new BigDecimal("80.00"), new BigDecimal("89.99")));
        when(userProfileMapper.selectUserProfileByUserId(13L)).thenReturn(profile);

        TProduct scenePrice = product(113L, 11L, "无匹配产地");
        scenePrice.setPrice(new BigDecimal("85.00"));
        TProduct legacyPrice = product(114L, 12L, "无匹配产地");
        legacyPrice.setPrice(new BigDecimal("25.00"));

        assertEquals(Arrays.asList(scenePrice, legacyPrice),
            service.recommendMall(13L, Arrays.asList(legacyPrice, scenePrice)));
    }

    @Test
    void noMatchingTagKeepsOriginalOrder()
    {
        when(userProfileMapper.selectUserProfileByUserId(8L)).thenReturn(profile("READY",
            profileData("MALL", "origin:肯尼亚", "origin", 8)));
        List<TProduct> products = Arrays.asList(product(101L, 11L, "埃塞俄比亚"),
            product(102L, 12L, "巴西"));

        assertSame(products, service.recommendMall(8L, products));
    }

    @Test
    void scanUsesBestSupportedOptionPerDimension()
    {
        when(userProfileMapper.selectUserProfileByUserId(9L)).thenReturn(profile("READY",
            profileData("SCAN", "temperature:冰", "temperature", 12,
                "temperature:热", "temperature", 2, "sugar:少糖", "sugar", 8)));
        ScanProduct preferred = scanProduct(201L);
        preferred.setSpecs(Arrays.asList(spec(1L, "温度", "热", "冰"), spec(2L, "糖度", "少糖", "正常糖")));
        ScanProduct other = scanProduct(202L);
        other.setSpecs(Arrays.asList(spec(3L, "温度", "热"), spec(4L, "糖度", "正常糖")));

        assertEquals(Arrays.asList(preferred, other),
            service.recommendScan(9L, Arrays.asList(other, preferred)));
    }

    @Test
    void drinkWithoutCoffeeDimensionsIsNotPenalized()
    {
        when(userProfileMapper.selectUserProfileByUserId(10L)).thenReturn(profile("READY",
            profileData("SCAN", "sugar:少糖", "sugar", 8)));
        ScanProduct drink = scanProduct(201L);
        drink.setProductType("DRINK");
        drink.setSpecs(Collections.singletonList(spec(1L, "糖度", "少糖")));
        ScanProduct coffee = scanProduct(202L);
        coffee.setProductType("COFFEE");
        coffee.setSpecs(Arrays.asList(spec(2L, "糖度", "正常糖"), spec(3L, "咖啡浓度", "浓")));

        assertEquals(Arrays.asList(drink, coffee),
            service.recommendScan(10L, Arrays.asList(coffee, drink)));
    }

    private UserProfile profile(String status, String data)
    {
        UserProfile profile = new UserProfile();
        profile.setProfileStatus(status);
        profile.setProfileData(data);
        return profile;
    }

    private String profileData(String scene, Object... values)
    {
        JSONArray tags = new JSONArray();
        for (int i = 0; i < values.length; i += 3)
        {
            JSONObject tag = new JSONObject();
            tag.put("key", values[i]);
            tag.put("dimension", values[i + 1]);
            tag.put("score", values[i + 2]);
            tags.add(tag);
        }
        JSONObject sceneData = new JSONObject();
        sceneData.put("tags", tags);
        JSONObject root = new JSONObject();
        root.put(scene, sceneData);
        return root.toJSONString();
    }

    private String profileDataWithPriceTag(String scene, Object firstKey, Object firstDimension,
        Object firstScore, BigDecimal min, BigDecimal max)
    {
        JSONObject root = JSONObject.parseObject(profileData(scene, firstKey, firstDimension, firstScore));
        JSONObject sceneData = root.getJSONObject(scene);
        JSONObject price = new JSONObject();
        price.put("key", "price:" + min + "-" + max);
        price.put("dimension", "price");
        price.put("name", min + "-" + max);
        price.put("score", 8);
        price.put("min", min);
        price.put("max", max);
        sceneData.getJSONArray("tags").add(price);
        return root.toJSONString();
    }

    private TProduct product(Long id, Long categoryId, String origin)
    {
        TProduct product = new TProduct();
        product.setProductId(id);
        product.setCategoryId(categoryId);
        product.setProductName("商品" + id);
        product.setOrigin(origin);
        product.setStock(10L);
        product.setPrice(new BigDecimal("25.00"));
        return product;
    }

    private ScanProduct scanProduct(Long id)
    {
        ScanProduct product = new ScanProduct();
        product.setProductId(id);
        product.setCategoryId(2L);
        product.setProductName("饮品" + id);
        return product;
    }

    private ScanProductSpec spec(Long id, String name, String... optionNames)
    {
        ScanProductSpec spec = new ScanProductSpec();
        spec.setSpecId(id);
        spec.setSpecName(name);
        java.util.ArrayList<ScanProductSpecOption> options = new java.util.ArrayList<>();
        for (int i = 0; i < optionNames.length; i++)
        {
            ScanProductSpecOption option = new ScanProductSpecOption();
            option.setSpecId(id);
            option.setOptionId(id * 10 + i);
            option.setOptionName(optionNames[i]);
            options.add(option);
        }
        spec.setOptions(options);
        return spec;
    }
}
