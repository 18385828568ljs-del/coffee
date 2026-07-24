package com.ruoyi.project.coffee.scanOrder.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
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
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecOptionMapper;
import com.ruoyi.project.coffee.scanOrder.service.impl.ScanProductSpecOptionServiceImpl;

class ScanProductSpecOptionServiceImplTest
{
    @Mock
    private ScanProductSpecOptionMapper scanProductSpecOptionMapper;

    private ScanProductSpecOptionServiceImpl scanProductSpecOptionService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        scanProductSpecOptionService = new ScanProductSpecOptionServiceImpl();
        ReflectionTestUtils.setField(scanProductSpecOptionService, "scanProductSpecOptionMapper",
                scanProductSpecOptionMapper);
    }

    @Test
    void selectOptionListByProductIdShouldDelegateToMapper()
    {
        List<ScanProductSpecOption> options = Collections.singletonList(new ScanProductSpecOption());
        when(scanProductSpecOptionMapper.selectOptionListByProductId(7L)).thenReturn(options);

        List<ScanProductSpecOption> result = scanProductSpecOptionService.selectOptionListByProductId(7L);

        assertSame(options, result);
        verify(scanProductSpecOptionMapper).selectOptionListByProductId(7L);
    }

    @Test
    void selectOptionListBySpecIdShouldDelegateToMapper()
    {
        List<ScanProductSpecOption> options = Collections.singletonList(new ScanProductSpecOption());
        when(scanProductSpecOptionMapper.selectOptionListBySpecId(11L)).thenReturn(options);

        List<ScanProductSpecOption> result = scanProductSpecOptionService.selectOptionListBySpecId(11L);

        assertSame(options, result);
        verify(scanProductSpecOptionMapper).selectOptionListBySpecId(11L);
    }

    @Test
    void selectOptionListByIdsShouldReturnEmptyListWhenNull()
    {
        List<ScanProductSpecOption> result = scanProductSpecOptionService.selectOptionListByIds(null);

        assertSame(Collections.emptyList(), result);
        verify(scanProductSpecOptionMapper, never()).selectOptionListByIds(anyList());
    }

    @Test
    void selectOptionListByIdsShouldReturnEmptyListWhenBlank()
    {
        List<ScanProductSpecOption> result = scanProductSpecOptionService.selectOptionListByIds(Collections.emptyList());

        assertSame(Collections.emptyList(), result);
        verify(scanProductSpecOptionMapper, never()).selectOptionListByIds(anyList());
    }

    @Test
    void selectOptionListByIdsShouldDelegateToMapperWhenIdsPresent()
    {
        List<Long> optionIds = Arrays.asList(1L, 2L);
        List<ScanProductSpecOption> options = Collections.singletonList(new ScanProductSpecOption());
        when(scanProductSpecOptionMapper.selectOptionListByIds(optionIds)).thenReturn(options);

        List<ScanProductSpecOption> result = scanProductSpecOptionService.selectOptionListByIds(optionIds);

        assertSame(options, result);
        verify(scanProductSpecOptionMapper).selectOptionListByIds(optionIds);
    }
}
