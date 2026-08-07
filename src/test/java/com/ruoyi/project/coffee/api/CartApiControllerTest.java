package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.behavior.service.UserBehaviorEventService;
import com.ruoyi.project.coffee.cart.domain.TCart;
import com.ruoyi.project.coffee.cart.service.ITCartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class CartApiControllerTest
{
    private CartApiController controller;

    @Mock
    private ITCartService cartService;

    @Mock
    private UserBehaviorEventService userBehaviorEventService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new CartApiController();
        ReflectionTestUtils.setField(controller, "cartService", cartService);
        ReflectionTestUtils.setField(controller, "userBehaviorEventService", userBehaviorEventService);
        bindUser(10L);
    }

    @AfterEach
    void tearDown()
    {
        WxUserAuthContext.clear();
    }

    @Test
    void addToCartRejectsMissingRequiredFields()
    {
        TCart cart = new TCart();
        cart.setQuantity(1L);

        AjaxResult result = controller.addToCart(cart);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("购物车参数不完整", result.get(AjaxResult.MSG_TAG));
        verify(cartService, never()).insertTCart(any(TCart.class));
    }

    @Test
    void addToCartMergesExistingSameProductAndSpec()
    {
        TCart incoming = new TCart();
        incoming.setProductId(5L);
        incoming.setQuantity(2L);
        incoming.setSpec(null);
        incoming.setBehaviorSource(UserBehaviorEventService.SOURCE_CATEGORY);
        TCart existing = new TCart();
        existing.setCartId(20L);
        existing.setUserId(10L);
        existing.setProductId(5L);
        existing.setCategoryId(6L);
        existing.setSpec("");
        existing.setQuantity(3L);
        when(cartService.selectTCartList(any(TCart.class))).thenReturn(Arrays.asList(existing));
        when(cartService.updateTCart(existing)).thenReturn(1);

        AjaxResult result = controller.addToCart(incoming);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals("已更新购物车数量", result.get(AjaxResult.MSG_TAG));
        assertEquals(5L, existing.getQuantity());
        verify(cartService).updateTCart(existing);
        verify(cartService, never()).insertTCart(any(TCart.class));
        verify(userBehaviorEventService).recordFirstCartAdd(10L, UserBehaviorEventService.SCENE_MALL,
            5L, 6L, 20L, UserBehaviorEventService.SOURCE_CATEGORY);
    }

    @Test
    void addToCartInsertsWhenNoExistingItem()
    {
        TCart incoming = new TCart();
        incoming.setProductId(5L);
        incoming.setQuantity(2L);
        incoming.setCartId(30L);
        incoming.setBehaviorSource(UserBehaviorEventService.SOURCE_CATEGORY);
        TCart saved = new TCart();
        saved.setCartId(30L);
        saved.setProductId(5L);
        saved.setCategoryId(6L);
        when(cartService.insertTCart(incoming)).thenReturn(1);
        when(cartService.selectTCartList(any(TCart.class))).thenReturn(Collections.emptyList());
        when(cartService.selectTCartByCartId(30L)).thenReturn(saved);

        AjaxResult result = controller.addToCart(incoming);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals(10L, incoming.getUserId());
        assertEquals("", incoming.getSpec());
        verify(cartService).insertTCart(incoming);
        verify(userBehaviorEventService).recordFirstCartAdd(10L, UserBehaviorEventService.SCENE_MALL,
            5L, 6L, 30L, UserBehaviorEventService.SOURCE_CATEGORY);
    }

    @Test
    void deleteCartRejectsOtherUsersRecord()
    {
        TCart existing = new TCart();
        existing.setCartId(2L);
        existing.setUserId(99L);
        when(cartService.selectTCartByCartId(2L)).thenReturn(existing);

        AjaxResult result = controller.deleteCart(2L);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("购物车记录不存在", result.get(AjaxResult.MSG_TAG));
        verify(cartService, never()).deleteTCartByCartId(2L);
    }

    @Test
    void deleteCartRecordsRemoveAfterSuccessfulDelete()
    {
        TCart existing = new TCart();
        existing.setCartId(2L);
        existing.setUserId(10L);
        existing.setProductId(5L);
        existing.setCategoryId(6L);
        when(cartService.selectTCartByCartId(2L)).thenReturn(existing);
        when(cartService.deleteTCartByCartId(2L)).thenReturn(1);

        AjaxResult result = controller.deleteCart(2L);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        verify(userBehaviorEventService).recordCartRemove(10L, UserBehaviorEventService.SCENE_MALL,
            5L, 6L, 2L);
    }

    @Test
    void clearCartDeletesCurrentUsersItems()
    {
        TCart first = new TCart();
        first.setCartId(1L);
        first.setProductId(5L);
        first.setCategoryId(6L);
        TCart second = new TCart();
        second.setCartId(2L);
        second.setProductId(7L);
        second.setCategoryId(8L);
        when(cartService.selectTCartList(any(TCart.class))).thenReturn(Arrays.asList(first, second));
        when(cartService.deleteTCartByCartId(1L)).thenReturn(1);
        when(cartService.deleteTCartByCartId(2L)).thenReturn(1);

        AjaxResult result = controller.clearCart(999L);

        ArgumentCaptor<TCart> captor = ArgumentCaptor.forClass(TCart.class);
        verify(cartService).selectTCartList(captor.capture());
        assertEquals(10L, captor.getValue().getUserId());
        verify(cartService).deleteTCartByCartId(1L);
        verify(cartService).deleteTCartByCartId(2L);
        verify(userBehaviorEventService).recordCartRemove(10L, UserBehaviorEventService.SCENE_MALL,
            5L, 6L, 1L);
        verify(userBehaviorEventService).recordCartRemove(10L, UserBehaviorEventService.SCENE_MALL,
            7L, 8L, 2L);
        assertEquals(0, result.get(AjaxResult.CODE_TAG));
    }

    private void bindUser(Long userId)
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(userId);
        user.setOpenid("openid-" + userId);
        WxUserAuthContext.setCurrentUser(user);
    }
}

