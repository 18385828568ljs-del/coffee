package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.auth.WxUserTokenService;
import com.ruoyi.project.coffee.behavior.service.UserBehaviorEventService;
import com.ruoyi.project.coffee.category.domain.TCategory;
import com.ruoyi.project.coffee.category.service.ITCategoryService;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ProductApiControllerTest
{
    private ProductApiController controller;

    @Mock
    private ITProductService productService;

    @Mock
    private ITCategoryService categoryService;

    @Mock
    private MarketingActivityEngine marketingActivityEngine;

    @Mock
    private WxUserTokenService wxUserTokenService;

    @Mock
    private UserBehaviorEventService userBehaviorEventService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new ProductApiController();
        ReflectionTestUtils.setField(controller, "productService", productService);
        ReflectionTestUtils.setField(controller, "categoryService", categoryService);
        ReflectionTestUtils.setField(controller, "marketingActivityEngine", marketingActivityEngine);
        ReflectionTestUtils.setField(controller, "wxUserTokenService", wxUserTokenService);
        ReflectionTestUtils.setField(controller, "userBehaviorEventService", userBehaviorEventService);
    }

    @AfterEach
    void tearDown()
    {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getCategoryListReturnsServiceData()
    {
        TCategory category = new TCategory();
        category.setCategoryId(1L);
        category.setCategoryName("咖啡豆");
        List<TCategory> categories = Arrays.asList(category);
        when(categoryService.selectTCategoryList(any(TCategory.class))).thenReturn(categories);

        AjaxResult result = controller.getCategoryList();

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(categories, result.get(AjaxResult.DATA_TAG));
    }

    @Test
    void getProductDetailRejectsMissingProduct()
    {
        when(productService.selectTProductByProductId(99L)).thenReturn(null);

        AjaxResult result = controller.getProductDetail(99L, new MockHttpServletRequest());

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("Product not found", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void getProductDetailNormalizesEmptyStockAndEnrichesProduct()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("source", UserBehaviorEventService.SOURCE_CATEGORY);
        TProduct product = new TProduct();
        product.setProductId(3L);
        product.setStatus(1);
        product.setStock(-5L);
        product.setImageUrl("cover.png");
        product.setImageUrls(Arrays.asList("cover.png", "detail.png"));
        when(productService.selectTProductByProductId(3L)).thenReturn(product);
        when(wxUserTokenService.resolveUserId(request)).thenReturn(7L);

        AjaxResult result = controller.getProductDetail(3L, request);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(product, result.get(AjaxResult.DATA_TAG));
        assertEquals(0L, product.getStock());
        assertEquals(Arrays.asList("cover.png", "detail.png"), ((TProduct) result.get(AjaxResult.DATA_TAG)).getImageUrls());
        verify(marketingActivityEngine).enrichProduct(product, 7L);
        verify(userBehaviorEventService).recordProductView(7L, UserBehaviorEventService.SCENE_MALL, 3L, null,
            UserBehaviorEventService.SOURCE_CATEGORY);
    }

    @Test
    void searchProductsRecordsAuthenticatedSearch()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(wxUserTokenService.resolveUserId(request)).thenReturn(7L);
        when(productService.selectTProductList(any(TProduct.class))).thenReturn(Collections.emptyList());

        controller.searchProducts("  拿铁  ", request);

        verify(userBehaviorEventService).recordSearch(7L, UserBehaviorEventService.SCENE_MALL, "拿铁");
    }

    @Test
    void internalProductLookupDoesNotRecordSearch()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("trackBehavior", "false");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(wxUserTokenService.resolveUserId(request)).thenReturn(7L);
        when(productService.selectTProductList(any(TProduct.class))).thenReturn(Collections.emptyList());

        controller.searchProducts("拿铁", request);

        verify(userBehaviorEventService, never()).recordSearch(any(), any(), any());
    }
}
