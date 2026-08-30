package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.auth.WxUserTokenService;
import com.ruoyi.project.coffee.behavior.service.UserBehaviorEventService;
import com.ruoyi.project.coffee.category.domain.TCategory;
import com.ruoyi.project.coffee.category.service.ITCategoryService;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.profile.service.ProductRecommendationService;
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

    @Mock
    private ProductRecommendationService productRecommendationService;

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
        ReflectionTestUtils.setField(controller, "productRecommendationService", productRecommendationService);
        when(productRecommendationService.recommendMall(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
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
    void productListPaginatesAfterRecommendationSorting()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("pageNum", "2");
        request.addParameter("pageSize", "1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        TProduct first = product(1L);
        TProduct second = product(2L);
        TProduct third = product(3L);
        when(productService.selectTProductList(any(TProduct.class))).thenReturn(Arrays.asList(first, second, third));

        TableDataInfo result = controller.getProductList(new TProduct(), request);

        assertEquals(3L, result.getTotal());
        assertSame(second, result.getRows().get(0));
    }

    @Test
    void productListExcludesOutOfStockProductsBeforeRecommendation()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        TProduct available = product(1L);
        TProduct unavailable = product(2L);
        unavailable.setStock(0L);
        when(productService.selectTProductList(any(TProduct.class)))
            .thenReturn(Arrays.asList(available, unavailable));

        TableDataInfo result = controller.getProductList(new TProduct(), request);

        assertEquals(1L, result.getTotal());
        assertSame(available, result.getRows().get(0));
    }

    private TProduct product(Long productId)
    {
        TProduct product = new TProduct();
        product.setProductId(productId);
        product.setStatus(1);
        product.setStock(10L);
        return product;
    }
}
