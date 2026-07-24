package com.ruoyi.project.coffee.category.service;

import java.util.List;
import com.ruoyi.project.coffee.category.domain.TCategory;

/**
 * 商品分类Service接口
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
public interface ITCategoryService 
{
    /**
     * 查询商品分类
     * 
     * @param categoryId 商品分类主键
     * @return 商品分类
     */
    public TCategory selectTCategoryByCategoryId(Long categoryId);

    /**
     * 查询商品分类列表
     * 
     * @param tCategory 商品分类
     * @return 商品分类集合
     */
    public List<TCategory> selectTCategoryList(TCategory tCategory);

    /**
     * 新增商品分类
     * 
     * @param tCategory 商品分类
     * @return 结果
     */
    public int insertTCategory(TCategory tCategory);

    /**
     * 修改商品分类
     * 
     * @param tCategory 商品分类
     * @return 结果
     */
    public int updateTCategory(TCategory tCategory);

    /**
     * 批量删除商品分类
     * 
     * @param categoryIds 需要删除的商品分类主键集合
     * @return 结果
     */
    public int deleteTCategoryByCategoryIds(String categoryIds);

    /**
     * 删除商品分类信息
     * 
     * @param categoryId 商品分类主键
     * @return 结果
     */
    public int deleteTCategoryByCategoryId(Long categoryId);
}
