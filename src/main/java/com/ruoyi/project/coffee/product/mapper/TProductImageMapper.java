package com.ruoyi.project.coffee.product.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.product.domain.TProductImage;

/**
 * 商品图片Mapper接口
 */
public interface TProductImageMapper
{
    public List<TProductImage> selectTProductImageByProductId(Long productId);

    public List<TProductImage> selectTProductImageByProductIds(@Param("productIds") List<Long> productIds);

    public int batchInsertTProductImage(@Param("images") List<TProductImage> images);

    public int deleteTProductImageByProductId(Long productId);

    public int deleteTProductImageByProductIds(Long[] productIds);
}
