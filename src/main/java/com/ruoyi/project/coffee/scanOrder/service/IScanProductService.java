package com.ruoyi.project.coffee.scanOrder.service;

import java.math.BigDecimal;
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

    /** 按当前商品和规格配置计算扫码点单单价。 */
    BigDecimal calculatePriceBySpecJson(Long productId, String specJson);

    /** 为快捷加购生成必选规格的默认选择。 */
    String buildDefaultSpecJson(Long productId);

    /** 根据服务端规格配置生成规格展示文本。 */
    String buildSpecText(Long productId, String specJson);

    int insertScanProduct(ScanProduct scanProduct);

    int updateScanProduct(ScanProduct scanProduct);

    int deleteScanProductByIds(String ids);
}
