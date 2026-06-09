package com.ruoyi.project.coffee.scanOrder.service;

import java.util.List;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;

/**
 * 扫码点单-商品Service接口
 */
public interface IScanProductService
{
    ScanProduct selectScanProductById(Long productId);

    List<ScanProduct> selectScanProductList(ScanProduct scanProduct);

    /** 查询商品并装配规格组与规格选项 */
    ScanProduct selectScanProductWithSpecs(Long productId);

    int insertScanProduct(ScanProduct scanProduct);

    int updateScanProduct(ScanProduct scanProduct);

    int deleteScanProductByIds(String ids);
}
