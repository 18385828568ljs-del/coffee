package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
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
        product.setImageUrl("latte.jpg");
        product.setPrice(new BigDecimal("12.00"));
        product.setStatus(1);
        ScanProductSpecOption option = new ScanProductSpecOption();
        option.setOptionId(2L);
        option.setExtraPrice(new BigDecimal("1.50"));
        ScanCart input = new ScanCart();
        input.setProductId(5L);
        input.setQuantity(2);
        input.setShopId(null);
        input.setTableNo(" A01 ");
        input.setSpecJson("[2]");
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
        assertEquals(1L, cart.getShopId());
        assertEquals("A01", cart.getTableNo());
        assertEquals("拿铁", cart.getProductName());
        assertEquals("latte.jpg", cart.getProductImage());
        assertEquals(new BigDecimal("13.50"), cart.getPrice());
        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals(saved, result.get(AjaxResult.DATA_TAG));
        verify(userBehaviorEventService).recordFirstCartAdd(18L, UserBehaviorEventService.SCENE_SCAN,
            5L, null, 9L);
    }

    @Test
    void getCartListBuildsQuantityAndAmountSummary()
    {
        ScanCart first = cart(2, "9.50");
        ScanCart second = cart(1, "12.00");
        when(scanCartService.selectScanCartList(any(ScanCart.class))).thenReturn(Arrays.asList(first, second));

        AjaxResult result = controller.getCartList(null, null, "A01");

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

    private ScanCart cart(Integer quantity, String price)
    {
        ScanCart cart = new ScanCart();
        cart.setQuantity(quantity);
        cart.setPrice(new BigDecimal(price));
        return cart;
    }

    private void bindUser(Long userId)
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(userId);
        user.setOpenid("openid-" + userId);
        WxUserAuthContext.setCurrentUser(user);
    }
}

