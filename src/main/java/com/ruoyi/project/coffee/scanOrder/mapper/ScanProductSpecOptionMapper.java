package com.ruoyi.project.coffee.scanOrder.mapper;

import java.util.List;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;

/**
 * 扫码点单-规格选项Mapper接口
 */
public interface ScanProductSpecOptionMapper
{
    List<ScanProductSpecOption> selectOptionListByProductId(Long productId);

    List<ScanProductSpecOption> selectOptionListBySpecId(Long specId);

    List<ScanProductSpecOption> selectOptionListByIds(List<Long> optionIds);

    int deleteOptionByProductIds(String[] productIds);
}
