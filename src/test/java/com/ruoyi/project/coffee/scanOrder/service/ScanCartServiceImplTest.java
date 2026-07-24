package com.ruoyi.project.coffee.scanOrder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanCartMapper;
import com.ruoyi.project.coffee.scanOrder.service.impl.ScanCartServiceImpl;

class ScanCartServiceImplTest
{
    @Mock
    private ScanCartMapper scanCartMapper;

    private ScanCartServiceImpl scanCartService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        scanCartService = new ScanCartServiceImpl();
        ReflectionTestUtils.setField(scanCartService, "scanCartMapper", scanCartMapper);
    }

    @Test
    void addOrIncreaseShouldNormalizeDefaultsAndInsertWhenNoMatchingCart()
    {
        ScanCart cart = cart(null, null, null);
        ScanCart saved = cart(100L, 1, "0.00");
        when(scanCartMapper.selectMatchingCart(cart)).thenReturn(Collections.<ScanCart>emptyList());
        when(scanCartMapper.insertScanCart(cart)).thenAnswer(invocation -> {
            cart.setId(100L);
            return 1;
        });
        when(scanCartMapper.selectScanCartById(100L)).thenReturn(saved);

        ScanCart result = scanCartService.addOrIncrease(cart);

        assertSame(saved, result);
        assertEquals(1, cart.getQuantity());
        assertEquals(0, BigDecimal.ZERO.compareTo(cart.getPrice()));
        assertEquals(1, cart.getStatus());
        assertEquals(1, cart.getSelected());
        assertEquals(0, cart.getDelFlag());
        assertNotNull(cart.getCreateTime());
        assertNotNull(cart.getUpdateTime());
        verify(scanCartMapper).insertScanCart(cart);
        verify(scanCartMapper).selectScanCartById(100L);
    }

    @Test
    void addOrIncreaseShouldMergeWithFirstMatchingCartAndUseLatestUnitPrice()
    {
        ScanCart request = cart(null, 2, "12.50");
        ScanCart existing = cart(100L, 3, "10.00");
        ScanCart saved = cart(100L, 5, "12.50");
        when(scanCartMapper.selectMatchingCart(request)).thenReturn(Arrays.asList(existing, cart(101L, 1, "10.00")));
        when(scanCartMapper.selectScanCartById(100L)).thenReturn(saved);

        ScanCart result = scanCartService.addOrIncrease(request);

        assertSame(saved, result);
        ArgumentCaptor<ScanCart> patchCaptor = ArgumentCaptor.forClass(ScanCart.class);
        verify(scanCartMapper).updateScanCart(patchCaptor.capture());
        ScanCart patch = patchCaptor.getValue();
        assertEquals(100L, patch.getId());
        assertEquals(5, patch.getQuantity());
        assertEquals(0, new BigDecimal("12.50").compareTo(patch.getPrice()));
        assertEquals(1, patch.getSelected());
        assertEquals(1, patch.getStatus());
        assertNotNull(patch.getUpdateTime());
        verify(scanCartMapper, never()).insertScanCart(any(ScanCart.class));
    }

    @Test
    void updateQuantityShouldReturnZeroWhenIdMissing()
    {
        int rows = scanCartService.updateQuantity(null, 3);

        assertEquals(0, rows);
        verify(scanCartMapper, never()).updateScanCart(any(ScanCart.class));
        verify(scanCartMapper, never()).logicDeleteById(any());
    }

    @Test
    void updateQuantityShouldLogicDeleteWhenQuantityIsNullOrNotPositive()
    {
        when(scanCartMapper.logicDeleteById(100L)).thenReturn(1);

        int rows = scanCartService.updateQuantity(100L, 0);

        assertEquals(1, rows);
        verify(scanCartMapper).logicDeleteById(100L);
        verify(scanCartMapper, never()).updateScanCart(any(ScanCart.class));
    }

    @Test
    void updateQuantityShouldPatchQuantityAndUpdateTime()
    {
        when(scanCartMapper.updateScanCart(any(ScanCart.class))).thenReturn(1);

        int rows = scanCartService.updateQuantity(100L, 4);

        assertEquals(1, rows);
        ArgumentCaptor<ScanCart> patchCaptor = ArgumentCaptor.forClass(ScanCart.class);
        verify(scanCartMapper).updateScanCart(patchCaptor.capture());
        ScanCart patch = patchCaptor.getValue();
        assertEquals(100L, patch.getId());
        assertEquals(4, patch.getQuantity());
        assertNotNull(patch.getUpdateTime());
        verify(scanCartMapper, never()).logicDeleteById(any());
    }

    @Test
    void logicDeleteByIdShouldReturnZeroWhenIdMissing()
    {
        int rows = scanCartService.logicDeleteById(null);

        assertEquals(0, rows);
        verify(scanCartMapper, never()).logicDeleteById(any());
    }

    @Test
    void logicDeleteByOwnerAndTableShouldRejectNullQuery()
    {
        int rows = scanCartService.logicDeleteByOwnerAndTable(null);

        assertEquals(0, rows);
        verify(scanCartMapper, never()).logicDeleteByOwnerAndTable(any(ScanCart.class));
    }

    @Test
    void logicDeleteByOwnerAndTableShouldRejectMissingUserAndBlankOpenid()
    {
        ScanCart query = new ScanCart();
        query.setOpenid("  ");

        int rows = scanCartService.logicDeleteByOwnerAndTable(query);

        assertEquals(0, rows);
        verify(scanCartMapper, never()).logicDeleteByOwnerAndTable(any(ScanCart.class));
    }

    @Test
    void logicDeleteByOwnerAndTableShouldAllowLoggedInUserWithoutOpenid()
    {
        ScanCart query = new ScanCart();
        query.setUserId(7L);
        when(scanCartMapper.logicDeleteByOwnerAndTable(query)).thenReturn(2);

        int rows = scanCartService.logicDeleteByOwnerAndTable(query);

        assertEquals(2, rows);
        verify(scanCartMapper).logicDeleteByOwnerAndTable(query);
    }

    @Test
    void logicDeleteByOwnerAndTableShouldAllowOpenidOwnerWithoutUserId()
    {
        ScanCart query = new ScanCart();
        query.setOpenid("openid-1");
        when(scanCartMapper.logicDeleteByOwnerAndTable(query)).thenReturn(1);

        int rows = scanCartService.logicDeleteByOwnerAndTable(query);

        assertEquals(1, rows);
        verify(scanCartMapper).logicDeleteByOwnerAndTable(query);
    }

    private static ScanCart cart(Long id, Integer quantity, String price)
    {
        ScanCart cart = new ScanCart();
        cart.setId(id);
        cart.setQuantity(quantity);
        if (price != null)
        {
            cart.setPrice(new BigDecimal(price));
        }
        return cart;
    }
}
