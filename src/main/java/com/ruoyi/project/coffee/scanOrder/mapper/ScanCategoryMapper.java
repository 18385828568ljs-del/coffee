package com.ruoyi.project.coffee.scanOrder.mapper;

import java.util.List;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCategory;

/**
 * 扫码点单-分类Mapper接口
 */
public interface ScanCategoryMapper
{
    ScanCategory selectScanCategoryById(Long categoryId);

    List<ScanCategory> selectScanCategoryList(ScanCategory scanCategory);

    int insertScanCategory(ScanCategory scanCategory);

    int updateScanCategory(ScanCategory scanCategory);

    int deleteScanCategoryByIds(String[] categoryIds);
}
