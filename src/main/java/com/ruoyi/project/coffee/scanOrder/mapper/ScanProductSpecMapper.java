package com.ruoyi.project.coffee.scanOrder.mapper;

import java.util.List;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpec;

/**
 * 扫码点单-商品规格组Mapper接口
 */
public interface ScanProductSpecMapper
{
    List<ScanProductSpec> selectSpecListByProductId(Long productId);

    int deleteSpecByProductIds(String[] productIds);
}
