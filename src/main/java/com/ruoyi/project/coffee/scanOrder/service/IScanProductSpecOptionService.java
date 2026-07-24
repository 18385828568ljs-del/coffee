package com.ruoyi.project.coffee.scanOrder.service;

import java.util.List;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;

/**
 * 扫码点单-规格选项Service接口
 */
public interface IScanProductSpecOptionService
{
    List<ScanProductSpecOption> selectOptionListByProductId(Long productId);

    List<ScanProductSpecOption> selectOptionListBySpecId(Long specId);

    List<ScanProductSpecOption> selectOptionListByIds(List<Long> optionIds);
}
