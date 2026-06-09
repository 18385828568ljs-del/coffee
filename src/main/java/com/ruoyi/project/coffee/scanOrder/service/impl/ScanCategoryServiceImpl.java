package com.ruoyi.project.coffee.scanOrder.service.impl;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.text.Convert;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCategory;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanCategoryMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductMapper;
import com.ruoyi.project.coffee.scanOrder.service.IScanCategoryService;

/**
 * 扫码点单-分类Service实现
 */
@Service
public class ScanCategoryServiceImpl implements IScanCategoryService
{
    @Autowired
    private ScanCategoryMapper scanCategoryMapper;

    @Autowired
    private ScanProductMapper scanProductMapper;

    @Override
    public ScanCategory selectScanCategoryById(Long categoryId)
    {
        return scanCategoryMapper.selectScanCategoryById(categoryId);
    }

    @Override
    public List<ScanCategory> selectScanCategoryList(ScanCategory scanCategory)
    {
        return scanCategoryMapper.selectScanCategoryList(scanCategory);
    }

    @Override
    public int insertScanCategory(ScanCategory scanCategory)
    {
        validateScanCategory(scanCategory);
        scanCategory.setCreateTime(DateUtils.getNowDate());
        return scanCategoryMapper.insertScanCategory(scanCategory);
    }

    @Override
    public int updateScanCategory(ScanCategory scanCategory)
    {
        validateScanCategory(scanCategory);
        scanCategory.setUpdateTime(DateUtils.getNowDate());
        return scanCategoryMapper.updateScanCategory(scanCategory);
    }

    @Override
    public int deleteScanCategoryByIds(String ids)
    {
        String[] categoryIds = Convert.toStrArray(ids);
        validateScanCategoryCanDelete(categoryIds);
        return scanCategoryMapper.deleteScanCategoryByIds(categoryIds);
    }

    private void validateScanCategory(ScanCategory scanCategory)
    {
        String categoryName = StringUtils.trimToNull(scanCategory.getCategoryName());
        if (categoryName == null)
        {
            throw new ServiceException("点单分类名称不能为空");
        }
        scanCategory.setCategoryName(categoryName);
        if (scanCategory.getStatus() == null)
        {
            scanCategory.setStatus(1);
        }
        if (scanCategory.getSortOrder() == null)
        {
            scanCategory.setSortOrder(0);
        }
    }

    private void validateScanCategoryCanDelete(String[] categoryIds)
    {
        if (categoryIds == null || categoryIds.length == 0)
        {
            return;
        }
        if (scanProductMapper.countScanProductByCategoryIds(categoryIds) > 0)
        {
            throw new ServiceException("该点单分类下仍有关联商品，不能删除");
        }
    }
}
