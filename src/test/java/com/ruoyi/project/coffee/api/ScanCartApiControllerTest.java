package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.behavior.service.UserBehaviorEventService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;
import com.ruoyi.project.coffee.scanOrder.service.IScanCartService;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductSpecOptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class ScanCartApiControllerTest
{
    private ScanCartApiController controller;

    @Mock
    private IScanCartService scanCartService;

    @Mock
    private UserBehaviorEventService userBehaviorEventService;

    @Mock
    private IScanProductService scanProductService;

    @Mock
    private IScanProductSpecOptionService scanProductSpecOptionService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new ScanCartApiController();
        ReflectionTestUtils.setField(controller, "scanCartService", scanCartService);
        ReflectionTestUtils.setField(controller, "userBehaviorEventService", userBehaviorEventService);
        ReflectionTestUtils.setField(controller, "scanProductService", scanProductService);
        ReflectionTestUtils.setField(controller, "scanProductSpecOptionService", scanProductSpecOptionService);
        bindUser(18L);
    }

    @AfterEach
    void tearDown()
    {
        WxUserAuthContext.clear();
    }

    @Test
    void addToCartRecalculatesPriceAndUsesServerProductInfo()
    {
        ScanProduct product = new ScanProduct();
        product.setProductId(5L);
        product.setProductName("拿铁");
        product.setCategoryId(4L);
        product.setImageUrl("latte.jpg");
        product.setPrice(new BigDecimal("12.00"));
        product.setStatus(1);
        ScanProductSpecOption option = new ScanProductSpecOption();
        option.setOptionId(2L);
        option.setProductId(5L);
        option.setExtraPrice(new BigDecimal("1.50"));
        ScanCart input = new ScanCart();
        input.setProductId(5L);
        input.setQuantity(2);
        input.setTableNo(" A01 ");
        input.setSpecJson("[{\"specId\":11,\"optionIds\":[2]}]");
        ScanCart saved = new ScanCart();
        saved.setId(9L);
        when(scanProductService.selectScanProductById(5L)).thenReturn(product);
        when(scanProductSpecOptionService.selectOptionListByIds(Arrays.asList(2L))).thenReturn(Arrays.asList(option));
        when(scanCartService.addOrIncrease(any(ScanCart.class))).thenReturn(saved);

        AjaxResult result = controller.addToCart(input);

        ArgumentCaptor<ScanCart> captor = ArgumentCaptor.forClass(ScanCart.class);
        verify(scanCartService).addOrIncrease(captor.capture());
        ScanCart cart = captor.getValue();
        assertEquals(18L, cart.getUserId());
        assertEquals("A01", cart.getTableNo());
        assertEquals("拿铁", cart.getProductName());
        assertEquals("latte.jpg", cart.getProductImage());
        assertEquals(new BigDecimal("13.50"), cart.getPrice());
        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals(saved, result.get(AjaxResult.DATA_TAG));
        verify(userBehaviorEventService).recordFirstCartAdd(18L, UserBehaviorEventService.SCENE_SCAN,
            5L, 4L, 9L, UserBehaviorEventService.SOURCE_CATEGORY, "[{\"specId\":11,\"optionIds\":[2]}]");
    }

    @Test
    void addToCartChargesConfiguredLargeCupExtraPrice()
    {
        ScanProduct product = new ScanProduct();
        product.setProductId(101L);
        product.setProductName("美式咖啡");
        product.setCategoryId(1L);
        product.setPrice(new BigDecimal("12.00"));
        product.setStatus(1);
        ScanProductSpecOption largeCup = new ScanProductSpecOption();
        largeCup.setOptionId(4L);
        largeCup.setProductId(101L);
        largeCup.setExtraPrice(new BigDecimal("3.00"));
        ScanCart input = new ScanCart();
        input.setProductId(101L);
        input.setQuantity(1);
        input.setSpecJson("[{\"specId\":2,\"optionIds\":[4]}]");
        when(scanProductService.selectScanProductById(101L)).thenReturn(product);
        when(scanProductSpecOptionService.selectOptionListByIds(Arrays.asList(4L)))
            .thenReturn(Arrays.asList(largeCup));
        when(scanCartService.addOrIncrease(any(ScanCart.class))).thenReturn(new ScanCart());

        controller.addToCart(input);

        ArgumentCaptor<ScanCart> captor = ArgumentCaptor.forClass(ScanCart.class);
        verify(scanCartService).addOrIncrease(captor.capture());
        assertEquals(new BigDecimal("15.00"), captor.getValue().getPrice());
    }

    @Test
    void addToCartRejectsLegacyFlatOptionIdArray()
    {
        ScanProduct product = new ScanProduct();
        product.setProductId(5L);
        product.setProductName("拿铁");
        product.setCategoryId(4L);
        product.setPrice(new BigDecimal("12.00"));
        product.setStatus(1);
        ScanCart input = new ScanCart();
        input.setProductId(5L);
        input.setSpecJson("[2]");
        when(scanProductService.selectScanProductById(5L)).thenReturn(product);

        AjaxResult result = controller.addToCart(input);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("规格参数无效", result.get(AjaxResult.MSG_TAG));
        verify(scanCartService, never()).addOrIncrease(any(ScanCart.class));
    }

    @Test
    void getCartListBuildsQuantityAndAmountSummary()
    {
        ScanCart first = cart(2, "9.50");
        ScanCart second = cart(1, "12.00");
        when(scanCartService.selectScanCartList(any(ScanCart.class))).thenReturn(Arrays.asList(first, second));

        AjaxResult result = controller.getCartList(null, "A01");

        Map<?, ?> data = (Map<?, ?>) result.get(AjaxResult.DATA_TAG);
        assertEquals(3, data.get("totalQuantity"));
        assertEquals(new BigDecimal("31.00"), data.get("totalAmount"));
    }

    @Test
    void deleteCartRejectsOtherUsersCart()
    {
        ScanCart existing = new ScanCart();
        existing.setId(8L);
        existing.setUserId(99L);
        existing.setDelFlag(0);
        when(scanCartService.selectScanCartById(8L)).thenReturn(existing);

        AjaxResult result = controller.deleteCart(8L);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("无权操作此购物车记录", result.get(AjaxResult.MSG_TAG));
        verify(scanCartService, never()).logicDeleteById(8L);
    }

    @Test
    void deleteCartRecordsRemoveAfterSuccessfulDelete()
    {
        ScanCart existing = new ScanCart();
        existing.setId(8L);
        existing.setUserId(18L);
        existing.setProductId(5L);
        existing.setDelFlag(0);
        ScanProduct product = new ScanProduct();
        product.setProductId(5L);
        product.setCategoryId(4L);
        when(scanCartService.selectScanCartById(8L)).thenReturn(existing);
        when(scanCartService.logicDeleteById(8L)).thenReturn(1);
        when(scanProductService.selectScanProductById(5L)).thenReturn(product);

        AjaxResult result = controller.deleteCart(8L);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        verify(userBehaviorEventService).recordCartRemove(18L, UserBehaviorEventService.SCENE_SCAN,
            5L, 4L, 8L);
    }

    @Test
    void updateCartToZeroRecordsRemove()
    {
        ScanCart existing = ownedCart(8L, 5L);
        ScanCart input = new ScanCart();
        input.setId(8L);
        input.setQuantity(0);
        ScanProduct product = product(5L, 4L);
        when(scanCartService.selectScanCartById(8L)).thenReturn(existing);
        when(scanCartService.updateQuantity(8L, 0)).thenReturn(1);
        when(scanProductService.selectScanProductById(5L)).thenReturn(product);

        AjaxResult result = controller.updateCart(input);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        verify(userBehaviorEventService).recordCartRemove(18L, UserBehaviorEventService.SCENE_SCAN,
            5L, 4L, 8L);
    }

    @Test
    void clearCartRecordsEveryRemovedItem()
    {
        ScanCart existing = ownedCart(8L, 5L);
        when(scanCartService.selectScanCartList(any(ScanCart.class)))
            .thenReturn(Collections.singletonList(existing));
        when(scanCartService.logicDeleteById(8L)).thenReturn(1);
        when(scanProductService.selectScanProductById(5L)).thenReturn(product(5L, 4L));

        AjaxResult result = controller.clearCart(null, "A01");

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        verify(scanCartService).logicDeleteById(8L);
        verify(userBehaviorEventService).recordCartRemove(18L, UserBehaviorEventService.SCENE_SCAN,
            5L, 4L, 8L);
    }

    @Test
    void clearCartFallsBackToBulkDeleteWhenBehaviorSnapshotFails()
    {
        when(scanCartService.selectScanCartList(any(ScanCart.class)))
            .thenThrow(new IllegalStateException("snapshot unavailable"));
        when(scanCartService.logicDeleteByOwnerAndTable(any(ScanCart.class))).thenReturn(1);

        AjaxResult result = controller.clearCart(null, "A01");

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        verify(scanCartService).logicDeleteByOwnerAndTable(any(ScanCart.class));
        verify(userBehaviorEventService, never()).recordCartRemove(any(), any(), any(), any(), any());
    }

    private ScanCart cart(Integer quantity, String price)
    {
        ScanCart cart = new ScanCart();
        cart.setQuantity(quantity);
        cart.setPrice(new BigDecimal(price));
        return cart;
    }

    private ScanCart ownedCart(Long id, Long productId)
    {
        ScanCart cart = new ScanCart();
        cart.setId(id);
        cart.setUserId(18L);
        cart.setProductId(productId);
        cart.setDelFlag(0);
        return cart;
    }

    private ScanProduct product(Long productId, Long categoryId)
    {
        ScanProduct product = new ScanProduct();
        product.setProductId(productId);
        product.setCategoryId(categoryId);
        return product;
    }

    private void bindUser(Long userId)
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(userId);
        user.setOpenid("openid-" + userId);
        WxUserAuthContext.setCurrentUser(user);
    }
}

