package com.ruoyi.project.coffee.item.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import com.ruoyi.project.coffee.item.mapper.TOrderItemMapper;
import com.ruoyi.project.coffee.item.service.impl.TOrderItemServiceImpl;

class TOrderItemServiceImplTest
{
    @Mock
    private TOrderItemMapper tOrderItemMapper;

    private TOrderItemServiceImpl tOrderItemService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        tOrderItemService = new TOrderItemServiceImpl();
        ReflectionTestUtils.setField(tOrderItemService, "tOrderItemMapper", tOrderItemMapper);
    }

    @Test
    void selectByIdShouldDelegateToMapper()
    {
        TOrderItem item = item(1L);
        when(tOrderItemMapper.selectTOrderItemByItemId(1L)).thenReturn(item);

        TOrderItem result = tOrderItemService.selectTOrderItemByItemId(1L);

        assertSame(item, result);
        verify(tOrderItemMapper).selectTOrderItemByItemId(1L);
    }

    @Test
    void selectListShouldDelegateToMapper()
    {
        TOrderItem query = item(null);
        List<TOrderItem> items = Collections.singletonList(item(1L));
        when(tOrderItemMapper.selectTOrderItemList(query)).thenReturn(items);

        List<TOrderItem> result = tOrderItemService.selectTOrderItemList(query);

        assertSame(items, result);
        verify(tOrderItemMapper).selectTOrderItemList(query);
    }

    @Test
    void selectListByOrderIdsShouldDelegateToMapper()
    {
        List<Long> orderIds = Arrays.asList(1L, 2L);
        List<TOrderItem> items = Collections.singletonList(item(1L));
        when(tOrderItemMapper.selectTOrderItemListByOrderIds(orderIds)).thenReturn(items);

        List<TOrderItem> result = tOrderItemService.selectTOrderItemListByOrderIds(orderIds);

        assertSame(items, result);
        verify(tOrderItemMapper).selectTOrderItemListByOrderIds(orderIds);
    }

    @Test
    void insertShouldDelegateToMapper()
    {
        TOrderItem item = item(null);
        when(tOrderItemMapper.insertTOrderItem(item)).thenReturn(1);

        int rows = tOrderItemService.insertTOrderItem(item);

        assertEquals(1, rows);
        verify(tOrderItemMapper).insertTOrderItem(item);
    }

    @Test
    void updateShouldDelegateToMapper()
    {
        TOrderItem item = item(1L);
        when(tOrderItemMapper.updateTOrderItem(item)).thenReturn(1);

        int rows = tOrderItemService.updateTOrderItem(item);

        assertEquals(1, rows);
        verify(tOrderItemMapper).updateTOrderItem(item);
    }

    @Test
    void deleteByIdsShouldSplitAndDelegate()
    {
        when(tOrderItemMapper.deleteTOrderItemByItemIds(any(String[].class))).thenReturn(2);

        int rows = tOrderItemService.deleteTOrderItemByItemIds("10,20");

        assertEquals(2, rows);
        verify(tOrderItemMapper).deleteTOrderItemByItemIds(new String[] {"10", "20"});
    }

    @Test
    void deleteByItemIdShouldDelegate()
    {
        when(tOrderItemMapper.deleteTOrderItemByItemId(anyLong())).thenReturn(1);

        int rows = tOrderItemService.deleteTOrderItemByItemId(3L);

        assertEquals(1, rows);
        verify(tOrderItemMapper).deleteTOrderItemByItemId(3L);
    }

    @Test
    void deleteByOrderIdShouldDelegate()
    {
        when(tOrderItemMapper.deleteTOrderItemByOrderId(anyLong())).thenReturn(1);

        int rows = tOrderItemService.deleteTOrderItemByOrderId(3L);

        assertEquals(1, rows);
        verify(tOrderItemMapper).deleteTOrderItemByOrderId(3L);
    }

    @Test
    void deleteByOrderIdsShouldSplitAndDelegate()
    {
        when(tOrderItemMapper.deleteTOrderItemByOrderIds(any(String[].class))).thenReturn(2);

        int rows = tOrderItemService.deleteTOrderItemByOrderIds("10,20");

        assertEquals(2, rows);
        verify(tOrderItemMapper).deleteTOrderItemByOrderIds(new String[] {"10", "20"});
    }

    private static TOrderItem item(Long itemId)
    {
        TOrderItem item = new TOrderItem();
        item.setItemId(itemId);
        return item;
    }
}
