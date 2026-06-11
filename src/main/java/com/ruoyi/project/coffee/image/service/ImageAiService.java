package com.ruoyi.project.coffee.image.service;

import java.io.IOException;
import java.util.Base64;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.image.client.ImageGenerationClient;
import com.ruoyi.project.coffee.image.config.ImageAiProperties;
import com.ruoyi.project.coffee.image.domain.ImageAiGenerateResult;
import com.ruoyi.project.coffee.image.domain.ImageGenerationRequest;
import com.ruoyi.project.coffee.image.domain.ImageGenerationResult;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;
import com.ruoyi.project.common.storage.FileStorageService;
import com.ruoyi.project.common.storage.StoredFileInfo;

@Service
public class ImageAiService
{
    public static final String PRODUCT_TYPE_MALL = "mall";

    public static final String PRODUCT_TYPE_SCAN = "scan";

    private final ImageAiProperties properties;

    private final ImageGenerationClient imageGenerationClient;

    private final FileStorageService fileStorageService;

    private final ITProductService productService;

    private final IScanProductService scanProductService;

    private final ImageSourceDownloader imageSourceDownloader;

    public ImageAiService(ImageAiProperties properties, ImageGenerationClient imageGenerationClient,
        FileStorageService fileStorageService, ITProductService productService, IScanProductService scanProductService,
        ImageSourceDownloader imageSourceDownloader)
    {
        this.properties = properties;
        this.imageGenerationClient = imageGenerationClient;
        this.fileStorageService = fileStorageService;
        this.productService = productService;
        this.scanProductService = scanProductService;
        this.imageSourceDownloader = imageSourceDownloader;
    }

    public ImageAiGenerateResult generate(String productType, Long productId, String imageUrl)
    {
        return generate(productType, productId, imageUrl, null);
    }

    public ImageAiGenerateResult generate(String productType, Long productId, String imageUrl, String prompt)
    {
        validateProductType(productType);
        validateProductId(productId);
        validateImageUrl(imageUrl, "请先上传商品主图");

        try
        {
            byte[] sourceImage = imageSourceDownloader.download(imageUrl);
            if (sourceImage == null || sourceImage.length == 0)
            {
                throw new ServiceException("原图下载失败");
            }
            if (sourceImage.length > properties.getMaxSourceImageBytes())
            {
                throw new ServiceException("原图过大，请上传不超过 10MB 的图片");
            }

            String sourceDataUrl = "data:" + detectMimeType(imageUrl) + ";base64,"
                + Base64.getEncoder().encodeToString(sourceImage);
            ImageGenerationResult generationResult = imageGenerationClient.generate(
                new ImageGenerationRequest(sourceDataUrl, prompt));
            byte[] generatedBytes = Base64.getDecoder().decode(generationResult.getImageBase64());
            StoredFileInfo storedFile = fileStorageService.upload(new ByteArrayMultipartFile(
                "file",
                "ai-generated.png",
                "image/png",
                generatedBytes));
            return new ImageAiGenerateResult(imageUrl, storedFile.getUrl());
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (IllegalStateException e)
        {
            throw new ServiceException(e.getMessage());
        }
        catch (IOException e)
        {
            throw new ServiceException("图片处理失败：" + e.getMessage());
        }
        catch (RuntimeException e)
        {
            throw new ServiceException("AI图片生成失败：" + e.getMessage());
        }
    }

    public int apply(String productType, Long productId, String generatedUrl)
    {
        validateProductType(productType);
        validateProductId(productId);
        validateImageUrl(generatedUrl, "生成图地址不能为空");

        if (PRODUCT_TYPE_MALL.equals(productType))
        {
            TProduct product = new TProduct();
            product.setProductId(productId);
            product.setImageUrl(generatedUrl);
            return productService.updateTProduct(product);
        }

        ScanProduct existing = scanProductService.selectScanProductById(productId);
        if (existing == null)
        {
            throw new ServiceException("点单商品不存在");
        }
        existing.setImageUrl(generatedUrl);
        return scanProductService.updateScanProduct(existing);
    }

    public void validateProductType(String productType)
    {
        if (!PRODUCT_TYPE_MALL.equals(productType) && !PRODUCT_TYPE_SCAN.equals(productType))
        {
            throw new ServiceException("商品类型不正确");
        }
    }

    private void validateProductId(Long productId)
    {
        if (productId == null)
        {
            throw new ServiceException("商品ID不能为空");
        }
    }

    private void validateImageUrl(String imageUrl, String message)
    {
        if (StringUtils.isEmpty(imageUrl))
        {
            throw new ServiceException(message);
        }
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://"))
        {
            throw new ServiceException("图片地址必须是 http 或 https 链接");
        }
    }

    private String detectMimeType(String imageUrl)
    {
        String normalized = imageUrl == null ? "" : imageUrl.toLowerCase();
        if (normalized.contains(".jpg") || normalized.contains(".jpeg"))
        {
            return "image/jpeg";
        }
        if (normalized.contains(".webp"))
        {
            return "image/webp";
        }
        if (normalized.contains(".gif"))
        {
            return "image/gif";
        }
        return "image/png";
    }
}
