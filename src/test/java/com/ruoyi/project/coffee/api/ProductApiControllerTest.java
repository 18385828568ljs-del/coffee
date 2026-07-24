package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.auth.WxUserTokenService;
import com.ruoyi.project.coffee.category.domain.TCategory;
import com.ruoyi.project.coffee.category.service.ITCategoryService;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

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

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new ProductApiController();
        ReflectionTestUtils.setField(controller, "productService", productService);
        ReflectionTestUtils.setField(controller, "categoryService", categoryService);
        ReflectionTestUtils.setField(controller, "marketingActivityEngine", marketingActivityEngine);
        ReflectionTestUtils.setField(controller, "wxUserTokenService", wxUserTokenService);
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
    }
}
