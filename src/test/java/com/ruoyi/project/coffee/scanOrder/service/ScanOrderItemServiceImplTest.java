package com.ruoyi.project.coffee.scanOrder.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderItem;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanOrderItemMapper;
import com.ruoyi.project.coffee.scanOrder.service.impl.ScanOrderItemServiceImpl;

class ScanOrderItemServiceImplTest
{
    @Mock
    private ScanOrderItemMapper scanOrderItemMapper;

    private ScanOrderItemServiceImpl scanOrderItemService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        scanOrderItemService = new ScanOrderItemServiceImpl();
        ReflectionTestUtils.setField(scanOrderItemService, "scanOrderItemMapper", scanOrderItemMapper);
    }

    @Test
    void selectItemsByOrderIdShouldDelegateToMapper()
    {
        List<ScanOrderItem> items = Collections.singletonList(new ScanOrderItem());
        when(scanOrderItemMapper.selectItemsByOrderId(9L)).thenReturn(items);

        List<ScanOrderItem> result = scanOrderItemService.selectItemsByOrderId(9L);

        assertSame(items, result);
        verify(scanOrderItemMapper).selectItemsByOrderId(9L);
    }
}
