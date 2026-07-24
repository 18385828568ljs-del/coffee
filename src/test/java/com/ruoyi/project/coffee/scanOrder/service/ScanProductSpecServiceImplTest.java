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
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpec;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecMapper;
import com.ruoyi.project.coffee.scanOrder.service.impl.ScanProductSpecServiceImpl;

class ScanProductSpecServiceImplTest
{
    @Mock
    private ScanProductSpecMapper scanProductSpecMapper;

    private ScanProductSpecServiceImpl scanProductSpecService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        scanProductSpecService = new ScanProductSpecServiceImpl();
        ReflectionTestUtils.setField(scanProductSpecService, "scanProductSpecMapper", scanProductSpecMapper);
    }

    @Test
    void selectSpecListByProductIdShouldDelegateToMapper()
    {
        List<ScanProductSpec> specs = Collections.singletonList(new ScanProductSpec());
        when(scanProductSpecMapper.selectSpecListByProductId(7L)).thenReturn(specs);

        List<ScanProductSpec> result = scanProductSpecService.selectSpecListByProductId(7L);

        assertSame(specs, result);
        verify(scanProductSpecMapper).selectSpecListByProductId(7L);
    }
}
