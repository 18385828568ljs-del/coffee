package com.ruoyi.project.coffee.scanOrder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanTableQrcodeMapper;
import com.ruoyi.project.coffee.scanOrder.service.impl.ScanTableQrcodeServiceImpl;
import com.ruoyi.project.coffee.scanOrder.wx.WxaCodeService;

class ScanTableQrcodeServiceImplTest
{
    @Mock
    private ScanTableQrcodeMapper scanTableQrcodeMapper;

    @Mock
    private WxaCodeService wxaCodeService;

    private ScanTableQrcodeServiceImpl service;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new ScanTableQrcodeServiceImpl();
        ReflectionTestUtils.setField(service, "scanTableQrcodeMapper", scanTableQrcodeMapper);
        ReflectionTestUtils.setField(service, "wxaCodeService", wxaCodeService);
    }

    @Test
    void selectByShopAndTableShouldTrimTableNoAndDelegate()
    {
        ScanTableQrcode qrcode = qrcode(1L, "A01", "dine_in");
        when(scanTableQrcodeMapper.selectByShopAndTable(any(ScanTableQrcode.class))).thenReturn(qrcode);

        ScanTableQrcode result = service.selectByShopAndTable(2L, "  A01  ");

        assertSame(qrcode, result);
        ArgumentCaptor<ScanTableQrcode> captor = ArgumentCaptor.forClass(ScanTableQrcode.class);
        verify(scanTableQrcodeMapper).selectByShopAndTable(captor.capture());
        assertEquals(2L, captor.getValue().getShopId());
        assertEquals("A01", captor.getValue().getTableNo());
    }

    @Test
    void selectByShopAndTableShouldReturnNullWhenTableNoIsBlank()
    {
        ScanTableQrcode result = service.selectByShopAndTable(2L, "  ");

        assertEquals(null, result);
        verify(scanTableQrcodeMapper, never()).selectByShopAndTable(any(ScanTableQrcode.class));
    }

    @Test
    void selectByIdShouldReturnNullWhenIdMissing()
    {
        assertEquals(null, service.selectById(null));
        verify(scanTableQrcodeMapper, never()).selectById(any());
    }

    @Test
    void selectListShouldUseEmptyQueryWhenNull()
    {
        ScanTableQrcode qrcode = qrcode(1L, "A01", "dine_in");
        when(scanTableQrcodeMapper.selectList(any(ScanTableQrcode.class))).thenReturn(Collections.singletonList(qrcode));

        List<ScanTableQrcode> result = service.selectList(null);

        assertEquals(1, result.size());
        verify(scanTableQrcodeMapper).selectList(any(ScanTableQrcode.class));
    }

    @Test
    void insertShouldNormalizeDefaultsAndWriteCreateTime()
    {
        ScanTableQrcode qrcode = qrcode(null, "  A01  ", " ");
        qrcode.setStatus(null);
        when(wxaCodeService.generateWxaCode(eq("pages/scan/menu"), any(String.class), any(String.class)))
                .thenReturn("https://qr.example.com/a01");
        when(scanTableQrcodeMapper.selectByShopAndTable(any(ScanTableQrcode.class))).thenReturn(null);
        when(scanTableQrcodeMapper.insert(qrcode)).thenReturn(1);

        int rows = service.insertScanTableQrcode(qrcode);

        assertEquals(1, rows);
        assertEquals(1L, qrcode.getShopId());
        assertEquals("A01", qrcode.getTableNo());
        assertEquals("dine_in", qrcode.getScene());
        assertEquals(1, qrcode.getStatus());
        assertNotNull(qrcode.getCreateTime());
        assertNotNull(qrcode.getUpdateTime());
        verify(scanTableQrcodeMapper).insert(qrcode);
    }

    @Test
    void updateShouldNormalizeDefaultsAndWriteUpdateTime()
    {
        ScanTableQrcode qrcode = qrcode(null, "  A02  ", null);
        qrcode.setStatus(null);
        when(scanTableQrcodeMapper.update(qrcode)).thenReturn(1);

        int rows = service.updateScanTableQrcode(qrcode);

        assertEquals(1, rows);
        assertEquals(1L, qrcode.getShopId());
        assertEquals("A02", qrcode.getTableNo());
        assertEquals("dine_in", qrcode.getScene());
        assertEquals(1, qrcode.getStatus());
        assertNotNull(qrcode.getUpdateTime());
        verify(scanTableQrcodeMapper).update(qrcode);
    }

    @Test
    void deleteByIdsShouldSplitAndIgnoreBlankEntries()
    {
        when(scanTableQrcodeMapper.deleteById(10L)).thenReturn(1);
        when(scanTableQrcodeMapper.deleteById(20L)).thenReturn(1);

        int rows = service.deleteScanTableQrcodeByIds("10, ,20");

        assertEquals(2, rows);
        verify(scanTableQrcodeMapper).deleteById(10L);
        verify(scanTableQrcodeMapper).deleteById(20L);
    }

    @Test
    void generateOneShouldCreateNewRecordWhenMissing()
    {
        when(wxaCodeService.generateWxaCode(eq("pages/scan/menu"), any(String.class), any(String.class)))
                .thenReturn("https://qr.example.com/a01");
        when(scanTableQrcodeMapper.selectByShopAndTable(any(ScanTableQrcode.class))).thenReturn(null);
        ScanTableQrcode saved = qrcode(100L, "A01", "dine_in");
        when(scanTableQrcodeMapper.selectById(100L)).thenReturn(saved);
        when(scanTableQrcodeMapper.insert(any(ScanTableQrcode.class))).thenAnswer(invocation -> {
            ScanTableQrcode arg = invocation.getArgument(0);
            arg.setTableId(100L);
            return 1;
        });

        ScanTableQrcode result = service.generateOne(2L, "门店A", " A01 ", " ");

        assertSame(saved, result);
        ArgumentCaptor<ScanTableQrcode> captor = ArgumentCaptor.forClass(ScanTableQrcode.class);
        verify(scanTableQrcodeMapper).insert(captor.capture());
        assertEquals(2L, captor.getValue().getShopId());
        assertEquals("A01", captor.getValue().getTableNo());
        assertEquals("dine_in", captor.getValue().getScene());
        assertEquals("https://qr.example.com/a01", captor.getValue().getQrUrl());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    void generateOneShouldUpdateExistingRecordAndReuseTableId()
    {
        ScanTableQrcode existing = qrcode(100L, "A01", "dine_in");
        when(wxaCodeService.generateWxaCode(eq("pages/scan/menu"), any(String.class), any(String.class)))
                .thenReturn("https://qr.example.com/a01");
        when(scanTableQrcodeMapper.selectByShopAndTable(any(ScanTableQrcode.class))).thenReturn(existing);
        when(scanTableQrcodeMapper.selectById(100L)).thenReturn(existing);
        when(scanTableQrcodeMapper.update(any(ScanTableQrcode.class))).thenReturn(1);

        ScanTableQrcode result = service.generateOne(2L, "新门店", "A01", "takeout");

        assertSame(existing, result);
        ArgumentCaptor<ScanTableQrcode> captor = ArgumentCaptor.forClass(ScanTableQrcode.class);
        verify(scanTableQrcodeMapper).update(captor.capture());
        assertEquals(100L, captor.getValue().getTableId());
        assertEquals("新门店", captor.getValue().getShopName());
        assertEquals("takeout", captor.getValue().getScene());
        assertEquals("https://qr.example.com/a01", captor.getValue().getQrUrl());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    void generateOneShouldRejectBlankTableNo()
    {
        ServiceException exception = org.junit.jupiter.api.Assertions.assertThrows(ServiceException.class,
                () -> service.generateOne(2L, "门店A", "  ", "dine_in"));

        assertEquals("桌号不能为空", exception.getMessage());
        verify(wxaCodeService, never()).generateWxaCode(any(), any(), any());
    }

    @Test
    void batchGenerateShouldDeduplicateTablesAndKeepOrder()
    {
        ScanTableQrcode first = qrcode(101L, "A01", "dine_in");
        ScanTableQrcode second = qrcode(102L, "A02", "dine_in");
        when(wxaCodeService.generateWxaCode(eq("pages/scan/menu"), any(String.class), any(String.class)))
                .thenReturn("https://qr.example.com/a01", "https://qr.example.com/a02");
        when(scanTableQrcodeMapper.selectByShopAndTable(any(ScanTableQrcode.class))).thenReturn(null);
        when(scanTableQrcodeMapper.insert(any(ScanTableQrcode.class))).thenAnswer(invocation -> {
            ScanTableQrcode arg = invocation.getArgument(0);
            if ("A01".equals(arg.getTableNo()))
            {
                arg.setTableId(101L);
            }
            else
            {
                arg.setTableId(102L);
            }
            return 1;
        });
        when(scanTableQrcodeMapper.selectById(101L)).thenReturn(first);
        when(scanTableQrcodeMapper.selectById(102L)).thenReturn(second);

        List<ScanTableQrcode> results = service.batchGenerate(2L, "门店A", Arrays.asList("A01", "A02", "A01"), " ");

        assertEquals(2, results.size());
        assertSame(first, results.get(0));
        assertSame(second, results.get(1));
        verify(wxaCodeService, org.mockito.Mockito.times(2)).generateWxaCode(eq("pages/scan/menu"), any(String.class),
                any(String.class));
    }

    @Test
    void batchGenerateShouldReturnFailureRecordWhenGenerationThrows()
    {
        when(wxaCodeService.generateWxaCode(eq("pages/scan/menu"), any(String.class), any(String.class)))
                .thenThrow(new RuntimeException("boom"));

        List<ScanTableQrcode> results = service.batchGenerate(2L, "门店A", Collections.singletonList("A01"), " ");

        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getShopId());
        assertEquals("A01", results.get(0).getTableNo());
        assertEquals("生成失败: boom", results.get(0).getRemark());
    }

    @Test
    void deleteByIdShouldReturnZeroWhenIdMissing()
    {
        assertEquals(0, service.deleteById(null));
        verify(scanTableQrcodeMapper, never()).deleteById(any());
    }

    private static ScanTableQrcode qrcode(Long tableId, String tableNo, String scene)
    {
        ScanTableQrcode qrcode = new ScanTableQrcode();
        qrcode.setTableId(tableId);
        qrcode.setShopId(1L);
        qrcode.setShopName("门店A");
        qrcode.setTableNo(tableNo);
        qrcode.setScene(scene);
        return qrcode;
    }
}
