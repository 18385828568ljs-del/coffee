package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.auth.WxUserTokenService;
import com.ruoyi.project.coffee.behavior.service.UserBehaviorEventService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCategory;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;
import com.ruoyi.project.coffee.scanOrder.service.IScanCategoryService;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;
import com.ruoyi.project.coffee.scanOrder.service.IScanTableQrcodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class ScanMenuApiControllerTest
{
    private ScanMenuApiController controller;

    @Mock
    private IScanCategoryService scanCategoryService;

    @Mock
    private IScanProductService scanProductService;

    @Mock
    private IScanTableQrcodeService scanTableQrcodeService;

    @Mock
    private WxUserTokenService wxUserTokenService;

    @Mock
    private UserBehaviorEventService userBehaviorEventService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new ScanMenuApiController();
        ReflectionTestUtils.setField(controller, "scanCategoryService", scanCategoryService);
        ReflectionTestUtils.setField(controller, "scanProductService", scanProductService);
        ReflectionTestUtils.setField(controller, "scanTableQrcodeService", scanTableQrcodeService);
        ReflectionTestUtils.setField(controller, "wxUserTokenService", wxUserTokenService);
        ReflectionTestUtils.setField(controller, "userBehaviorEventService", userBehaviorEventService);
    }

    @Test
    void getCategoryListOnlyQueriesEnabledCategories()
    {
        ScanCategory category = new ScanCategory();
        category.setCategoryId(1L);
        when(scanCategoryService.selectScanCategoryList(any(ScanCategory.class))).thenReturn(Arrays.asList(category));

        AjaxResult result = controller.getCategoryList();

        ArgumentCaptor<ScanCategory> captor = ArgumentCaptor.forClass(ScanCategory.class);
        org.mockito.Mockito.verify(scanCategoryService).selectScanCategoryList(captor.capture());
        assertEquals(1, captor.getValue().getStatus());
        assertEquals(0, result.get(AjaxResult.CODE_TAG));
    }

    @Test
    void getProductDetailRejectsDownProduct()
    {
        ScanProduct product = new ScanProduct();
        product.setProductId(7L);
        product.setStatus(0);
        when(scanProductService.selectScanProductWithSpecs(7L)).thenReturn(product);

        AjaxResult result = controller.getProductDetail(7L, new MockHttpServletRequest());

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("商品已下架", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void getProductDetailRecordsCategoryView()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ScanProduct product = new ScanProduct();
        product.setProductId(7L);
        product.setCategoryId(3L);
        product.setStatus(1);
        when(scanProductService.selectScanProductWithSpecs(7L)).thenReturn(product);
        when(wxUserTokenService.resolveUserId(request)).thenReturn(18L);

        AjaxResult result = controller.getProductDetail(7L, request);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        verify(userBehaviorEventService).recordProductView(18L, UserBehaviorEventService.SCENE_SCAN,
            7L, 3L, UserBehaviorEventService.SOURCE_CATEGORY);
    }

    @Test
    void parseTableReturnsTableContext()
    {
        ScanTableQrcode table = new ScanTableQrcode();
        table.setTableId(3L);
        table.setShopId(1L);
        table.setShopName("测试门店");
        table.setTableNo("A01");
        table.setScene("shopId=1&tableNo=A01");
        table.setStatus(1);
        when(scanTableQrcodeService.selectByShopAndTable(1L, "A01")).thenReturn(table);

        AjaxResult result = controller.parseTable(null, "A01");

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        java.util.Map<?, ?> data = (java.util.Map<?, ?>) result.get(AjaxResult.DATA_TAG);
        assertEquals("测试门店", data.get("shopName"));
        assertEquals("A01", data.get("tableNo"));
        assertSame(table.getScene(), data.get("scene"));
    }
}

