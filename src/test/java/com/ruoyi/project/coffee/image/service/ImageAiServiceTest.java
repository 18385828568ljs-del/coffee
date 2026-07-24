package com.ruoyi.project.coffee.image.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ruoyi.project.coffee.image.client.ImageGenerationClient;
import com.ruoyi.project.coffee.image.config.ImageAiProperties;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;
import com.ruoyi.project.common.storage.FileStorageService;

class ImageAiServiceTest
{
    @Mock
    private ImageAiProperties properties;

    @Mock
    private ImageGenerationClient imageGenerationClient;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ITProductService productService;

    @Mock
    private IScanProductService scanProductService;

    @Mock
    private ImageSourceDownloader imageSourceDownloader;

    private ImageAiService imageAiService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        imageAiService = new ImageAiService(properties, imageGenerationClient, fileStorageService, productService,
            scanProductService, imageSourceDownloader);
    }

    @Test
    void applyMallImageShouldReplaceMainImageAndKeepDetailImages()
    {
        TProduct product = new TProduct();
        product.setProductId(1001L);
        product.setImageUrl("https://example.com/old-cover.png");
        product.setImageUrls(Arrays.asList(
            "https://example.com/old-cover.png",
            "https://example.com/detail-1.png",
            "https://example.com/detail-2.png"));
        when(productService.selectTProductByProductId(1001L)).thenReturn(product);
        when(productService.updateTProduct(product)).thenReturn(1);

        int rows = imageAiService.apply(ImageAiService.PRODUCT_TYPE_MALL, 1001L,
            "https://example.com/new-cover.png");

        assertEquals(1, rows);
        verify(productService).updateTProduct(argThat(updated ->
            "https://example.com/new-cover.png".equals(updated.getImageUrl())
                && updated.getImageUrls().equals(Arrays.asList(
                    "https://example.com/new-cover.png",
                    "https://example.com/detail-1.png",
                    "https://example.com/detail-2.png"))));
    }
}
