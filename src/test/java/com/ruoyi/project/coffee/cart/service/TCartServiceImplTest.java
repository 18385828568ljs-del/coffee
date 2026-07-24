package com.ruoyi.project.coffee.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.cart.domain.TCart;
import com.ruoyi.project.coffee.cart.mapper.TCartMapper;
import com.ruoyi.project.coffee.cart.service.impl.TCartServiceImpl;

class TCartServiceImplTest
{
    @Mock
    private TCartMapper cartMapper;

    private TCartServiceImpl cartService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        cartService = new TCartServiceImpl();
        ReflectionTestUtils.setField(cartService, "tCartMapper", cartMapper);
    }

    @Test
    void selectTCartByCartIdShouldDelegateToMapper()
    {
        TCart cart = cart(10L);
        when(cartMapper.selectTCartByCartId(10L)).thenReturn(cart);

        TCart result = cartService.selectTCartByCartId(10L);

        assertSame(cart, result);
    }

    @Test
    void selectTCartListShouldDelegateQueryToMapper()
    {
        TCart query = new TCart();
        query.setUserId(7L);
        List<TCart> carts = Collections.singletonList(cart(10L));
        when(cartMapper.selectTCartList(query)).thenReturn(carts);

        List<TCart> result = cartService.selectTCartList(query);

        assertSame(carts, result);
        verify(cartMapper).selectTCartList(query);
    }

    @Test
    void deleteInvalidTCartByUserIdShouldOnlyPassCurrentUserId()
    {
        when(cartMapper.deleteInvalidTCartByUserId(7L)).thenReturn(3);

        int rows = cartService.deleteInvalidTCartByUserId(7L);

        assertEquals(3, rows);
        verify(cartMapper).deleteInvalidTCartByUserId(7L);
    }

    @Test
    void insertTCartShouldSetCreateTimeBeforeInsert()
    {
        TCart cart = cart(null);
        when(cartMapper.insertTCart(cart)).thenReturn(1);

        int rows = cartService.insertTCart(cart);

        assertEquals(1, rows);
        assertNotNull(cart.getCreateTime());
        verify(cartMapper).insertTCart(cart);
    }

    @Test
    void updateTCartShouldSetUpdateTimeBeforeUpdate()
    {
        TCart cart = cart(10L);
        when(cartMapper.updateTCart(cart)).thenReturn(1);

        int rows = cartService.updateTCart(cart);

        assertEquals(1, rows);
        assertNotNull(cart.getUpdateTime());
        verify(cartMapper).updateTCart(cart);
    }

    @Test
    void deleteTCartByCartIdsShouldSplitCommaSeparatedIds()
    {
        when(cartMapper.deleteTCartByCartIds(any(String[].class))).thenReturn(2);

        int rows = cartService.deleteTCartByCartIds("10,20");

        assertEquals(2, rows);
        ArgumentCaptor<String[]> idsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(cartMapper).deleteTCartByCartIds(idsCaptor.capture());
        assertEquals(Arrays.asList("10", "20"), Arrays.asList(idsCaptor.getValue()));
    }

    @Test
    void deleteTCartByCartIdShouldDelegateSingleId()
    {
        when(cartMapper.deleteTCartByCartId(10L)).thenReturn(1);

        int rows = cartService.deleteTCartByCartId(10L);

        assertEquals(1, rows);
        verify(cartMapper).deleteTCartByCartId(10L);
    }

    @Test
    void deleteTCartByProductIdsShouldReturnZeroWhenIdsAreNull()
    {
        int rows = cartService.deleteTCartByProductIds(null);

        assertEquals(0, rows);
        verify(cartMapper, never()).deleteTCartByProductIds(any(Long[].class));
    }

    @Test
    void deleteTCartByProductIdsShouldReturnZeroWhenIdsAreEmpty()
    {
        int rows = cartService.deleteTCartByProductIds(new Long[0]);

        assertEquals(0, rows);
        verify(cartMapper, never()).deleteTCartByProductIds(any(Long[].class));
    }

    @Test
    void deleteTCartByProductIdsShouldDelegateNonEmptyIds()
    {
        Long[] productIds = new Long[] {1001L, 1002L};
        when(cartMapper.deleteTCartByProductIds(productIds)).thenReturn(4);

        int rows = cartService.deleteTCartByProductIds(productIds);

        assertEquals(4, rows);
        verify(cartMapper).deleteTCartByProductIds(productIds);
    }

    private static TCart cart(Long cartId)
    {
        TCart cart = new TCart();
        cart.setCartId(cartId);
        cart.setUserId(7L);
        cart.setProductId(1001L);
        cart.setQuantity(2L);
        return cart;
    }
}
