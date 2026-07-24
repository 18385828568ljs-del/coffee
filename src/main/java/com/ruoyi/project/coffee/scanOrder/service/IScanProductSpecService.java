package com.ruoyi.project.coffee.scanOrder.service;

import java.util.List;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpec;

/**
 * 扫码点单-商品规格组Service接口
 */
public interface IScanProductSpecService
{
    List<ScanProductSpec> selectSpecListByProductId(Long productId);
}
