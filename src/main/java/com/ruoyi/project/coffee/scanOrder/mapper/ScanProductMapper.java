package com.ruoyi.project.coffee.scanOrder.mapper;

import java.util.List;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;

/**
 * 扫码点单-商品Mapper接口
 */
public interface ScanProductMapper
{
    ScanProduct selectScanProductById(Long productId);

    List<ScanProduct> selectScanProductList(ScanProduct scanProduct);

    int countScanProductByCategoryIds(String[] categoryIds);

    int insertScanProduct(ScanProduct scanProduct);

    int updateScanProduct(ScanProduct scanProduct);

    int deleteScanProductByIds(String[] productIds);
}
