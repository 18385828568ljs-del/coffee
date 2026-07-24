package com.ruoyi.project.coffee.activity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.activity.domain.MarketingPreviewResult;
import com.ruoyi.project.coffee.activity.domain.TMarketingActivity;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import com.ruoyi.project.coffee.member.service.MemberService;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.order.service.ITOrderService;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;
import com.ruoyi.project.system.config.service.IConfigService;

class MarketingActivityEngineTest
{
    private MarketingActivityEngine engine;

    @Mock
    private ITMarketingActivityService marketingActivityService;

    @Mock
    private ITProductService productService;

    @Mock
    private IScanProductService scanProductService;

    @Mock
    private ITOrderService orderService;

    @Mock
    private IConfigService configService;

    @Mock
    private MemberService memberService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        engine = new MarketingActivityEngine();
        ReflectionTestUtils.setField(engine, "marketingActivityService", marketingActivityService);
        ReflectionTestUtils.setField(engine, "productService", productService);
        ReflectionTestUtils.setField(engine, "scanProductService", scanProductService);
        ReflectionTestUtils.setField(engine, "orderService", orderService);
        ReflectionTestUtils.setField(engine, "configService", configService);
        ReflectionTestUtils.setField(engine, "memberService", memberService);
        when(configService.selectConfigByKey("coffee.order.baseFreight")).thenReturn("10.00");
        when(orderService.countCompletedOrdersByUserId(any())).thenReturn(1);
        when(memberService.getMemberDiscountRate(any())).thenReturn(BigDecimal.ONE);
    }

    @Test
    void previewOrderShouldChooseBestPriceActivityAndApplyMemberDiscountAfterActivityDiscount()
    {
        when(productService.selectTProductByProductIds(any())).thenReturn(Arrays.asList(
            product(1001L, 11L, "招牌拿铁", "latte.png", new BigDecimal("30.00"), 99L)
        ));
        when(marketingActivityService.selectTMarketingActivityList(any())).thenReturn(Arrays.asList(
            activity(1L, "满 50 减 5", "mall", "reduce", new BigDecimal("5.00"), new BigDecimal("50.00")),
            activity(2L, "满 50 减 8", "mall", "reduce", new BigDecimal("8.00"), new BigDecimal("50.00"))
        ));
        when(memberService.getMemberDiscountRate(7L)).thenReturn(new BigDecimal("0.90"));

        MarketingPreviewResult result = engine.previewOrder(7L, Collections.singletonList(orderItem(1001L, 2L)));

        assertEquals(new BigDecimal("60.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("8.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("5.20"), result.getMemberDiscount());
        assertEquals(new BigDecimal("10.00"), result.getFreightAmount());
        assertEquals(new BigDecimal("56.80"), result.getPayAmount());
        assertEquals("满 50 减 8", result.getActivitySummary());
    }

    @Test
    void previewOrderShouldRemoveFreightWhenFreeShippingActivityMatches()
    {
        when(productService.selectTProductByProductIds(any())).thenReturn(Arrays.asList(
            product(1001L, 11L, "招牌拿铁", "latte.png", new BigDecimal("30.00"), 99L)
        ));
        when(marketingActivityService.selectTMarketingActivityList(any())).thenReturn(Arrays.asList(
            activity(3L, "满 50 包邮", "mall", "free_shipping", BigDecimal.ZERO, new BigDecimal("50.00"))
        ));

        MarketingPreviewResult result = engine.previewOrder(7L, Collections.singletonList(orderItem(1001L, 2L)));

        assertEquals(new BigDecimal("60.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("0.00"), result.getFreightAmount());
        assertEquals(new BigDecimal("60.00"), result.getPayAmount());
        assertEquals("满 50 包邮", result.getActivitySummary());
    }

    @Test
    void previewOrderShouldCreateGiftItemWhenGiftActivityMatches()
    {
        when(productService.selectTProductByProductIds(any())).thenReturn(Arrays.asList(
            product(1001L, 11L, "招牌拿铁", "latte.png", new BigDecimal("30.00"), 99L)
        ));
        when(productService.selectTProductByProductId(2001L)).thenReturn(
            product(2001L, 12L, "赠品曲奇", "cookie.png", new BigDecimal("12.00"), 10L)
        );
        when(marketingActivityService.selectTMarketingActivityList(any())).thenReturn(Arrays.asList(
            giftActivity(4L, "满 50 赠曲奇", "mall", 2001L, 1L)
        ));

        MarketingPreviewResult result = engine.previewOrder(7L, Collections.singletonList(orderItem(1001L, 2L)));

        assertEquals(new BigDecimal("60.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("70.00"), result.getPayAmount());
        assertEquals(1, result.getGiftItems().size());
        assertEquals("赠品曲奇", result.getGiftItems().get(0).getProductName());
        assertEquals(Boolean.TRUE, result.getGiftItems().get(0).getGiftItem());
        assertEquals("满 50 赠曲奇", result.getActivitySummary());
    }

    @Test
    void previewOrderShouldApplyNewUserOnlyActivityOnlyWhenUserHasNoCompletedOrders()
    {
        when(productService.selectTProductByProductIds(any())).thenReturn(Arrays.asList(
            product(1001L, 11L, "招牌拿铁", "latte.png", new BigDecimal("30.00"), 99L)
        ));
        TMarketingActivity newUserActivity = activity(
            5L, "新客立减", "mall", "reduce", new BigDecimal("10.00"), new BigDecimal("30.00")
        );
        newUserActivity.setConditionNewUserOnly(1);
        when(marketingActivityService.selectTMarketingActivityList(any())).thenReturn(Arrays.asList(newUserActivity));

        when(orderService.countCompletedOrdersByUserId(7L)).thenReturn(0);
        MarketingPreviewResult newUserResult = engine.previewOrder(7L, Collections.singletonList(orderItem(1001L, 1L)));

        when(orderService.countCompletedOrdersByUserId(8L)).thenReturn(1);
        MarketingPreviewResult oldUserResult = engine.previewOrder(8L, Collections.singletonList(orderItem(1001L, 1L)));

        assertEquals(new BigDecimal("10.00"), newUserResult.getDiscountAmount());
        assertEquals("新客立减", newUserResult.getActivitySummary());
        assertEquals(new BigDecimal("0.00"), oldUserResult.getDiscountAmount());
        assertEquals("", oldUserResult.getActivitySummary());
    }

    @Test
    void previewScanOrderShouldUseScanActivitiesAndIgnoreFreight()
    {
        when(scanProductService.selectScanProductById(3001L)).thenReturn(
            scanProduct(3001L, 31L, new BigDecimal("18.00"))
        );
        when(marketingActivityService.selectTMarketingActivityList(any())).thenReturn(Arrays.asList(
            activity(6L, "商城满减", "mall", "reduce", new BigDecimal("8.00"), new BigDecimal("30.00")),
            activity(7L, "点单满减", "scan", "reduce", new BigDecimal("5.00"), new BigDecimal("30.00"))
        ));

        MarketingPreviewResult result = engine.previewScanOrder(7L, Collections.singletonList(scanCart(3001L, 2, new BigDecimal("18.00"))));

        assertEquals(new BigDecimal("36.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("5.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("0.00"), result.getFreightAmount());
        assertEquals(new BigDecimal("31.00"), result.getPayAmount());
        assertEquals("点单满减", result.getActivitySummary());
    }

    @Test
    void applyOrderMarketingShouldWritePreviewAmountsBackToOrder()
    {
        when(productService.selectTProductByProductIds(any())).thenReturn(Arrays.asList(
            product(1001L, 11L, "招牌拿铁", "latte.png", new BigDecimal("30.00"), 99L)
        ));
        when(marketingActivityService.selectTMarketingActivityList(any())).thenReturn(Arrays.asList(
            activity(8L, "满 50 减 8", "mall", "reduce", new BigDecimal("8.00"), new BigDecimal("50.00"))
        ));

        TOrder order = new TOrder();
        order.setUserId(7L);

        MarketingPreviewResult result = engine.applyOrderMarketing(order, Collections.singletonList(orderItem(1001L, 2L)));

        assertEquals(result.getTotalAmount(), order.getTotalAmount());
        assertEquals(result.getPayAmount(), order.getPayAmount());
        assertEquals(result.getDiscountAmount(), order.getDiscountAmount());
        assertEquals(result.getFreightAmount(), order.getFreightAmount());
        assertEquals(result.getActivitySummary(), order.getActivitySummary());
    }

    @Test
    void enrichProductShouldSetSalePriceAndActivityTag()
    {
        TProduct product = product(1001L, 11L, "招牌拿铁", "latte.png", new BigDecimal("30.00"), 99L);
        when(marketingActivityService.selectTMarketingActivityList(any())).thenReturn(Arrays.asList(
            activity(9L, "单品立减", "mall", "reduce", new BigDecimal("6.00"), new BigDecimal("20.00"))
        ));

        engine.enrichProduct(product, 7L);

        assertEquals(new BigDecimal("30.00"), product.getOriginalPrice());
        assertEquals(new BigDecimal("24.00"), product.getSalePrice());
        assertEquals("单品立减", product.getActivityTag());
        assertTrue(product.getActivityTags().contains("单品立减"));
    }

    private TOrderItem orderItem(Long productId, Long quantity)
    {
        TOrderItem item = new TOrderItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private ScanCart scanCart(Long productId, Integer quantity, BigDecimal price)
    {
        ScanCart cart = new ScanCart();
        cart.setProductId(productId);
        cart.setQuantity(quantity);
        cart.setPrice(price);
        return cart;
    }

    private TProduct product(Long productId, Long categoryId, String name, String image, BigDecimal price, Long stock)
    {
        TProduct product = new TProduct();
        product.setProductId(productId);
        product.setCategoryId(categoryId);
        product.setProductName(name);
        product.setImageUrl(image);
        product.setPrice(price);
        product.setStock(stock);
        product.setStatus(1);
        return product;
    }

    private ScanProduct scanProduct(Long productId, Long categoryId, BigDecimal price)
    {
        ScanProduct product = new ScanProduct();
        product.setProductId(productId);
        product.setCategoryId(categoryId);
        product.setPrice(price);
        product.setStatus(1);
        return product;
    }

    private TMarketingActivity activity(Long id, String title, String targetType, String effectMode, BigDecimal effectValue, BigDecimal minAmount)
    {
        TMarketingActivity activity = new TMarketingActivity();
        activity.setActivityId(id);
        activity.setTitle(title);
        activity.setTargetType(targetType);
        activity.setScopeType(0);
        activity.setStatus(1);
        activity.setStartTime(new Date(System.currentTimeMillis() - 1000L));
        activity.setEndTime(new Date(System.currentTimeMillis() + 86400000L));
        activity.setConditionMinAmount(minAmount);
        activity.setConditionMinQuantity(1L);
        activity.setConditionNewUserOnly(0);
        activity.setEffectMode(effectMode);
        activity.setEffectValue(effectValue);
        activity.setShippingBaseFreight(new BigDecimal("10.00"));
        return activity;
    }

    private TMarketingActivity giftActivity(Long id, String title, String targetType, Long giftProductId, Long giftQuantity)
    {
        TMarketingActivity activity = activity(id, title, targetType, "gift", BigDecimal.ZERO, new BigDecimal("50.00"));
        activity.setGiftProductId(giftProductId);
        activity.setGiftQuantity(giftQuantity);
        return activity;
    }
}
