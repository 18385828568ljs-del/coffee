package com.ruoyi.project.coffee.category.service.impl;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.text.Convert;
import com.ruoyi.project.coffee.category.domain.TCategory;
import com.ruoyi.project.coffee.category.mapper.TCategoryMapper;
import com.ruoyi.project.coffee.category.service.ITCategoryService;
import com.ruoyi.project.coffee.product.mapper.TProductMapper;

/**
 * Product category service.
 *
 * @author 闃垮崪 QQ932696181
 * @date 2026-03-12
 */
@Service
public class TCategoryServiceImpl implements ITCategoryService
{
    @Autowired
    private TCategoryMapper tCategoryMapper;

    @Autowired
    private TProductMapper tProductMapper;

    @Override
    public TCategory selectTCategoryByCategoryId(Long categoryId)
    {
        return tCategoryMapper.selectTCategoryByCategoryId(categoryId);
    }

    @Override
    public List<TCategory> selectTCategoryList(TCategory tCategory)
    {
        return tCategoryMapper.selectTCategoryList(tCategory);
    }

    @Override
    public int insertTCategory(TCategory tCategory)
    {
        validateCategoryName(tCategory);
        tCategory.setCreateTime(DateUtils.getNowDate());
        return tCategoryMapper.insertTCategory(tCategory);
    }

    @Override
    public int updateTCategory(TCategory tCategory)
    {
        validateCategoryName(tCategory);
        tCategory.setUpdateTime(DateUtils.getNowDate());
        return tCategoryMapper.updateTCategory(tCategory);
    }

    @Override
    public int deleteTCategoryByCategoryIds(String categoryIds)
    {
        String[] categoryIdArray = Convert.toStrArray(categoryIds);
        validateCategoryCanDelete(categoryIdArray);
        return tCategoryMapper.deleteTCategoryByCategoryIds(categoryIdArray);
    }

    @Override
    public int deleteTCategoryByCategoryId(Long categoryId)
    {
        validateCategoryCanDelete(new String[] { String.valueOf(categoryId) });
        return tCategoryMapper.deleteTCategoryByCategoryId(categoryId);
    }

    private void validateCategoryName(TCategory tCategory)
    {
        String categoryName = StringUtils.trimToNull(tCategory.getCategoryName());
        if (categoryName == null)
        {
            throw new ServiceException("\u5206\u7c7b\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        tCategory.setCategoryName(categoryName);
        if (tCategoryMapper.countByCategoryName(categoryName, tCategory.getCategoryId()) > 0)
        {
            throw new ServiceException("\u5206\u7c7b\u540d\u79f0\u5df2\u5b58\u5728\uff0c\u8bf7\u91cd\u65b0\u586b\u5199");
        }
    }

    private void validateCategoryCanDelete(String[] categoryIds)
    {
        if (categoryIds == null || categoryIds.length == 0)
        {
            return;
        }
        if (tProductMapper.countProductByCategoryIds(categoryIds) > 0)
        {
            throw new ServiceException("\u8be5\u5206\u7c7b\u4e0b\u4ecd\u6709\u5546\u54c1\uff0c\u4e0d\u80fd\u5220\u9664");
        }
    }
}
