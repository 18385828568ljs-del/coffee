package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;
import com.ruoyi.project.coffee.scanOrder.service.IScanTableQrcodeService;

class ScanTableQrcodeApiControllerTest
{
    @Mock
    private IScanTableQrcodeService scanTableQrcodeService;

    @Captor
    private ArgumentCaptor<List<String>> tableNosCaptor;

    private ScanTableQrcodeApiController controller;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new ScanTableQrcodeApiController();
        ReflectionTestUtils.setField(controller, "scanTableQrcodeService", scanTableQrcodeService);
    }

    @Test
    void generateShouldRejectEmptyBody()
    {
        AjaxResult result = controller.generate(null);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("请求参数不能为空", result.get(AjaxResult.MSG_TAG));
        verify(scanTableQrcodeService, never()).generateOne(any(Long.class), anyString(), anyString(), anyString());
    }

    @Test
    void generateShouldRejectBlankTableNo()
    {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("tableNo", " ");

        AjaxResult result = controller.generate(body);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("桌号不能为空", result.get(AjaxResult.MSG_TAG));
        verify(scanTableQrcodeService, never()).generateOne(any(Long.class), anyString(), anyString(), anyString());
    }

    @Test
    void generateShouldDelegateParsedParamsToService()
    {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("shopId", "2");
        body.put("shopName", "门店A");
        body.put("tableNo", "A01");
        body.put("scene", "dine_in");
        ScanTableQrcode qrcode = new ScanTableQrcode();
        when(scanTableQrcodeService.generateOne(2L, "门店A", "A01", "dine_in")).thenReturn(qrcode);

        AjaxResult result = controller.generate(body);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(qrcode, result.get(AjaxResult.DATA_TAG));
        verify(scanTableQrcodeService).generateOne(2L, "门店A", "A01", "dine_in");
    }

    @Test
    void batchGenerateShouldRejectWhenNoTablesProvided()
    {
        AjaxResult result = controller.batchGenerate(new HashMap<String, Object>());

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("请提供 tableNos 数组,或 prefix+start+end 区间", result.get(AjaxResult.MSG_TAG));
        verify(scanTableQrcodeService, never()).batchGenerate(any(Long.class), anyString(), anyList(), anyString());
    }

    @Test
    void batchGenerateShouldTrimTableNosAndIgnoreBlankEntries()
    {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("shopId", 3);
        body.put("shopName", "门店B");
        body.put("scene", "takeout");
        body.put("tableNos", Arrays.asList(" A01 ", "", null, "A02"));
        List<ScanTableQrcode> qrcodes = Arrays.asList(new ScanTableQrcode(), new ScanTableQrcode());
        when(scanTableQrcodeService.batchGenerate(any(Long.class), anyString(), anyList(), anyString())).thenReturn(qrcodes);

        AjaxResult result = controller.batchGenerate(body);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(qrcodes, result.get(AjaxResult.DATA_TAG));
        verify(scanTableQrcodeService).batchGenerate(org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq("门店B"), tableNosCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("takeout"));
        assertEquals(Arrays.asList("A01", "A02"), tableNosCaptor.getValue());
    }

    @Test
    void batchGenerateShouldSupportRangeMode()
    {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("shopId", 4L);
        body.put("prefix", "B");
        body.put("start", "1");
        body.put("end", "3");
        body.put("digits", "2");
        when(scanTableQrcodeService.batchGenerate(any(Long.class), anyString(), anyList(), anyString()))
                .thenReturn(Arrays.asList(new ScanTableQrcode(), new ScanTableQrcode(), new ScanTableQrcode()));

        AjaxResult result = controller.batchGenerate(body);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        verify(scanTableQrcodeService).batchGenerate(org.mockito.ArgumentMatchers.eq(4L),
                org.mockito.ArgumentMatchers.isNull(), tableNosCaptor.capture(), org.mockito.ArgumentMatchers.isNull());
        assertEquals(Arrays.asList("B01", "B02", "B03"), tableNosCaptor.getValue());
    }

    @Test
    void listShouldBuildQueryAndReturnServiceResult()
    {
        List<ScanTableQrcode> qrcodes = Arrays.asList(new ScanTableQrcode());
        when(scanTableQrcodeService.selectList(any(ScanTableQrcode.class))).thenReturn(qrcodes);

        AjaxResult result = controller.list(5L, "", 1);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(qrcodes, result.get(AjaxResult.DATA_TAG));
        ArgumentCaptor<ScanTableQrcode> queryCaptor = ArgumentCaptor.forClass(ScanTableQrcode.class);
        verify(scanTableQrcodeService).selectList(queryCaptor.capture());
        assertEquals(5L, queryCaptor.getValue().getShopId());
        assertEquals(null, queryCaptor.getValue().getTableNo());
        assertEquals(1, queryCaptor.getValue().getStatus());
    }

    @Test
    void deleteShouldRejectMissingRecord()
    {
        when(scanTableQrcodeService.selectById(8L)).thenReturn(null);

        AjaxResult result = controller.delete(8L);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("记录不存在", result.get(AjaxResult.MSG_TAG));
        verify(scanTableQrcodeService, never()).deleteById(8L);
    }
}
