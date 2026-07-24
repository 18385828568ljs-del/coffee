package com.ruoyi.project.coffee.product.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.coffee.product.mapper.TProductMapper;
import com.ruoyi.project.coffee.product.mapper.TProductImageMapper;
import com.ruoyi.project.coffee.cart.service.ITCartService;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.domain.TProductImage;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.common.utils.text.Convert;

/**
 * 商品Service业务层处理
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Service
public class TProductServiceImpl implements ITProductService 
{
    private static final int MAX_PRODUCT_IMAGE_COUNT = 6;

    @Autowired
    private TProductMapper tProductMapper;

    @Autowired
    private TProductImageMapper tProductImageMapper;

    @Autowired
    private ITCartService tCartService;

    /**
     * 查询商品
     * 
     * @param productId 商品主键
     * @return 商品
     */
    @Override
    public TProduct selectTProductByProductId(Long productId)
    {
        TProduct product = tProductMapper.selectTProductByProductId(productId);
        attachProductImages(product);
        return product;
    }

    /**
     * 查询商品列表
     * 
     * @param tProduct 商品
     * @return 商品
     */
    @Override
    public List<TProduct> selectTProductList(TProduct tProduct)
    {
        List<TProduct> products = tProductMapper.selectTProductList(tProduct);
        attachProductImages(products);
        return products;
    }

    /**
     * 新增商品
     * 
     * @param tProduct 商品
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTProduct(TProduct tProduct)
    {
        tProduct.setCreateTime(DateUtils.getNowDate());
        normalizeProductImages(tProduct);
        int rows = tProductMapper.insertTProduct(tProduct);
        syncProductImages(tProduct);
        return rows;
    }

    /**
     * 修改商品
     * 
     * @param tProduct 商品
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTProduct(TProduct tProduct)
    {
        tProduct.setUpdateTime(DateUtils.getNowDate());
        normalizeProductImages(tProduct);
        int rows = tProductMapper.updateTProduct(tProduct);
        syncProductImages(tProduct);
        return rows;
    }

    @Override
    public int decreaseStock(Long productId, Long quantity)
    {
        return tProductMapper.decreaseStock(productId, quantity);
    }

    @Override
    public int increaseStock(Long productId, Long quantity)
    {
        return tProductMapper.increaseStock(productId, quantity);
    }

    @Override
    public List<TProduct> selectTProductByProductIds(List<Long> productIds)
    {
        if (productIds == null || productIds.isEmpty())
        {
            return java.util.Collections.emptyList();
        }
        List<TProduct> products = tProductMapper.selectTProductByProductIds(productIds);
        attachProductImages(products);
        return products;
    }

    /**
     * 批量删除商品
     *
     * @param productIds 需要删除的商品主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTProductByProductIds(String productIds)
    {
        tCartService.deleteTCartByProductIds(Convert.toLongArray(productIds));
        tProductImageMapper.deleteTProductImageByProductIds(Convert.toLongArray(productIds));
        return tProductMapper.deleteTProductByProductIds(Convert.toStrArray(productIds));
    }

    /**
     * 删除商品信息
     * 
     * @param productId 商品主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTProductByProductId(Long productId)
    {
        tCartService.deleteTCartByProductIds(new Long[] { productId });
        tProductImageMapper.deleteTProductImageByProductId(productId);
        return tProductMapper.deleteTProductByProductId(productId);
    }

    private void attachProductImages(List<TProduct> products)
    {
        if (products == null || products.isEmpty())
        {
            return;
        }
        List<Long> productIds = products.stream()
            .filter(product -> product != null && product.getProductId() != null)
            .map(TProduct::getProductId)
            .collect(Collectors.toList());
        if (productIds.isEmpty())
        {
            products.forEach(this::attachProductImages);
            return;
        }

        List<TProductImage> images;
        try
        {
            images = tProductImageMapper.selectTProductImageByProductIds(productIds);
        }
        catch (DataAccessException e)
        {
            products.forEach(product -> attachProductImages(product, null));
            return;
        }
        if (images == null)
        {
            images = java.util.Collections.emptyList();
        }
        Map<Long, List<TProductImage>> imageMap = images.stream()
            .collect(Collectors.groupingBy(TProductImage::getProductId));
        products.forEach(product -> attachProductImages(product, imageMap.get(product.getProductId())));
    }

    private void attachProductImages(TProduct product)
    {
        if (product == null)
        {
            return;
        }
        List<TProductImage> images;
        try
        {
            images = tProductImageMapper.selectTProductImageByProductId(product.getProductId());
        }
        catch (DataAccessException e)
        {
            images = null;
        }
        attachProductImages(product, images);
    }

    private void attachProductImages(TProduct product, List<TProductImage> images)
    {
        if (product == null)
        {
            return;
        }
        List<String> imageUrls = new ArrayList<>();
        if (images != null)
        {
            for (TProductImage image : images)
            {
                if (image != null && image.getImageUrl() != null && !"".equals(image.getImageUrl().trim()))
                {
                    imageUrls.add(image.getImageUrl().trim());
                }
            }
        }
        if (imageUrls.isEmpty() && product.getImageUrl() != null && !"".equals(product.getImageUrl().trim()))
        {
            imageUrls.add(product.getImageUrl().trim());
        }
        product.setImageUrls(imageUrls);
        if (!imageUrls.isEmpty())
        {
            product.setImageUrl(imageUrls.get(0));
        }
    }

    private void normalizeProductImages(TProduct product)
    {
        if (product == null)
        {
            return;
        }
        LinkedHashSet<String> normalizedSet = new LinkedHashSet<>();
        if (product.getImageUrls() != null)
        {
            for (String imageUrl : product.getImageUrls())
            {
                addNormalizedImageUrl(normalizedSet, imageUrl);
            }
        }
        if (normalizedSet.isEmpty())
        {
            addNormalizedImageUrl(normalizedSet, product.getImageUrl());
        }

        List<String> normalized = new ArrayList<>(normalizedSet);
        if (normalized.size() > MAX_PRODUCT_IMAGE_COUNT)
        {
            normalized = normalized.subList(0, MAX_PRODUCT_IMAGE_COUNT);
        }
        product.setImageUrls(normalized);
        product.setImageUrl(normalized.isEmpty() ? null : normalized.get(0));
    }

    private void addNormalizedImageUrl(LinkedHashSet<String> imageUrls, String imageUrl)
    {
        if (imageUrl == null)
        {
            return;
        }
        String normalized = imageUrl.trim();
        if (!"".equals(normalized) && imageUrls.size() < MAX_PRODUCT_IMAGE_COUNT)
        {
            imageUrls.add(normalized);
        }
    }

    private void syncProductImages(TProduct product)
    {
        if (product == null || product.getProductId() == null)
        {
            return;
        }
        tProductImageMapper.deleteTProductImageByProductId(product.getProductId());
        if (product.getImageUrls() == null || product.getImageUrls().isEmpty())
        {
            return;
        }
        List<TProductImage> images = new ArrayList<>();
        for (int i = 0; i < product.getImageUrls().size(); i++)
        {
            TProductImage image = new TProductImage();
            image.setProductId(product.getProductId());
            image.setImageUrl(product.getImageUrls().get(i));
            image.setSortOrder(i);
            image.setIsMain(i == 0 ? 1 : 0);
            image.setCreateBy(product.getUpdateBy() != null ? product.getUpdateBy() : product.getCreateBy());
            image.setCreateTime(DateUtils.getNowDate());
            images.add(image);
        }
        tProductImageMapper.batchInsertTProductImage(images);
    }
}
