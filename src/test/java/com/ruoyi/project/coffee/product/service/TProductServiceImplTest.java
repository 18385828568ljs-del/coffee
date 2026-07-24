package com.ruoyi.project.coffee.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruoyi.project.coffee.cart.service.ITCartService;
import com.ruoyi.project.coffee.product.domain.TProductImage;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.mapper.TProductImageMapper;
import com.ruoyi.project.coffee.product.mapper.TProductMapper;
import com.ruoyi.project.coffee.product.service.impl.TProductServiceImpl;
import java.util.Arrays;
import java.util.List;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.util.ReflectionTestUtils;

class TProductServiceImplTest
{
    private TProductServiceImpl productService;

    @Mock
    private TProductMapper productMapper;

    @Mock
    private TProductImageMapper productImageMapper;

    @Mock
    private ITCartService cartService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        productService = new TProductServiceImpl();
        ReflectionTestUtils.setField(productService, "tProductMapper", productMapper);
        ReflectionTestUtils.setField(productService, "tProductImageMapper", productImageMapper);
        ReflectionTestUtils.setField(productService, "tCartService", cartService);
    }

    @Test
    void insertProductShouldFillCreateTimeBeforePersisting()
    {
        TProduct product = new TProduct();
        when(productMapper.insertTProduct(product)).thenReturn(1);

        int rows = productService.insertTProduct(product);

        assertEquals(1, rows);
        assertNotNull(product.getCreateTime());
        verify(productMapper).insertTProduct(product);
    }

    @Test
    void insertProductShouldSyncOrderedImagesAndUseFirstImageAsMain()
    {
        TProduct product = new TProduct();
        product.setImageUrl("legacy-cover.png");
        product.setImageUrls(Arrays.asList(" detail-1.png ", "detail-2.png", "detail-1.png", "", "detail-3.png"));
        when(productMapper.insertTProduct(product)).thenAnswer(invocation -> {
            product.setProductId(1001L);
            return 1;
        });

        int rows = productService.insertTProduct(product);

        assertEquals(1, rows);
        assertEquals("detail-1.png", product.getImageUrl());
        verify(productImageMapper).deleteTProductImageByProductId(1001L);
        verify(productImageMapper).batchInsertTProductImage(org.mockito.ArgumentMatchers.argThat(images -> {
            if (images.size() != 3) {
                return false;
            }
            TProductImage first = images.get(0);
            TProductImage second = images.get(1);
            TProductImage third = images.get(2);
            return Long.valueOf(1001L).equals(first.getProductId())
                && "detail-1.png".equals(first.getImageUrl())
                && Integer.valueOf(0).equals(first.getSortOrder())
                && Integer.valueOf(1).equals(first.getIsMain())
                && "detail-2.png".equals(second.getImageUrl())
                && Integer.valueOf(1).equals(second.getSortOrder())
                && Integer.valueOf(0).equals(second.getIsMain())
                && "detail-3.png".equals(third.getImageUrl())
                && Integer.valueOf(2).equals(third.getSortOrder());
        }));
    }

    @Test
    void updateProductShouldFillUpdateTimeBeforePersisting()
    {
        TProduct product = new TProduct();
        product.setProductId(1001L);
        when(productMapper.updateTProduct(product)).thenReturn(1);

        int rows = productService.updateTProduct(product);

        assertEquals(1, rows);
        assertNotNull(product.getUpdateTime());
        verify(productMapper).updateTProduct(product);
    }

    @Test
    void selectProductShouldAttachOrderedImagesAndFallbackToMainImage()
    {
        TProduct product = new TProduct();
        product.setProductId(1001L);
        product.setImageUrl("cover.png");
        when(productMapper.selectTProductByProductId(1001L)).thenReturn(product);
        when(productImageMapper.selectTProductImageByProductId(1001L)).thenReturn(Arrays.asList(
            productImage(1001L, "cover.png", 0, 1),
            productImage(1001L, "detail.png", 1, 0)
        ));

        TProduct result = productService.selectTProductByProductId(1001L);

        assertEquals(Arrays.asList("cover.png", "detail.png"), result.getImageUrls());
        assertEquals("cover.png", result.getImageUrl());

        TProduct fallbackProduct = new TProduct();
        fallbackProduct.setProductId(1002L);
        fallbackProduct.setImageUrl("legacy.png");
        when(productMapper.selectTProductByProductId(1002L)).thenReturn(fallbackProduct);
        when(productImageMapper.selectTProductImageByProductId(1002L)).thenReturn(Arrays.asList());

        TProduct fallback = productService.selectTProductByProductId(1002L);

        assertEquals(Arrays.asList("legacy.png"), fallback.getImageUrls());
    }

    @Test
    void selectProductsShouldFallbackToLegacyImageWhenProductImageTableIsMissing()
    {
        TProduct product = new TProduct();
        product.setProductId(1001L);
        product.setImageUrl("legacy.png");
        when(productMapper.selectTProductList(any(TProduct.class))).thenReturn(Arrays.asList(product));
        when(productImageMapper.selectTProductImageByProductIds(Arrays.asList(1001L))).thenThrow(
            new BadSqlGrammarException("select", "select * from t_product_image",
                new SQLException("Table 'ruoyi.t_product_image' doesn't exist"))
        );

        List<TProduct> products = productService.selectTProductList(new TProduct());

        assertEquals(1, products.size());
        assertEquals(Arrays.asList("legacy.png"), products.get(0).getImageUrls());
    }

    @Test
    void stockOperationsShouldDelegateToMapper()
    {
        when(productMapper.decreaseStock(1001L, 2L)).thenReturn(1);
        when(productMapper.increaseStock(1001L, 2L)).thenReturn(1);

        assertEquals(1, productService.decreaseStock(1001L, 2L));
        assertEquals(1, productService.increaseStock(1001L, 2L));

        verify(productMapper).decreaseStock(1001L, 2L);
        verify(productMapper).increaseStock(1001L, 2L);
    }

    @Test
    void selectByProductIdsShouldReturnEmptyListWhenIdsMissing()
    {
        assertTrue(productService.selectTProductByProductIds(null).isEmpty());
        assertTrue(productService.selectTProductByProductIds(Arrays.asList()).isEmpty());

        verifyNoInteractions(productMapper);
    }

    @Test
    void selectByProductIdsShouldQueryMapperWhenIdsPresent()
    {
        List<Long> ids = Arrays.asList(1001L, 1002L);
        when(productMapper.selectTProductByProductIds(ids)).thenReturn(Arrays.asList(new TProduct(), new TProduct()));

        List<TProduct> products = productService.selectTProductByProductIds(ids);

        assertEquals(2, products.size());
        verify(productMapper).selectTProductByProductIds(ids);
    }

    @Test
    void deleteProductsShouldClearCartBeforeDeletingProducts()
    {
        when(productMapper.deleteTProductByProductIds(any())).thenReturn(2);

        int rows = productService.deleteTProductByProductIds("1001,1002");

        assertEquals(2, rows);
        verify(cartService).deleteTCartByProductIds(new Long[] {1001L, 1002L});
        verify(productImageMapper).deleteTProductImageByProductIds(new Long[] {1001L, 1002L});
        verify(productMapper).deleteTProductByProductIds(new String[] {"1001", "1002"});
    }

    @Test
    void deleteSingleProductShouldClearCartBeforeDeletingProduct()
    {
        when(productMapper.deleteTProductByProductId(1001L)).thenReturn(1);

        int rows = productService.deleteTProductByProductId(1001L);

        assertEquals(1, rows);
        verify(cartService).deleteTCartByProductIds(new Long[] {1001L});
        verify(productImageMapper).deleteTProductImageByProductId(1001L);
        verify(productMapper).deleteTProductByProductId(1001L);
    }

    private TProductImage productImage(Long productId, String imageUrl, Integer sortOrder, Integer isMain)
    {
        TProductImage image = new TProductImage();
        image.setProductId(productId);
        image.setImageUrl(imageUrl);
        image.setSortOrder(sortOrder);
        image.setIsMain(isMain);
        return image;
    }
}
