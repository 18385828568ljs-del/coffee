package com.ruoyi.project.coffee.category.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.category.domain.TCategory;

/**
 * 商品分类Mapper接口
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
public interface TCategoryMapper 
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
     * 校验分类名称是否重复
     *
     * @param categoryName 分类名称
     * @param excludeCategoryId 排除的分类ID
     * @return 数量
     */
    public int countByCategoryName(@Param("categoryName") String categoryName, @Param("excludeCategoryId") Long excludeCategoryId);

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
     * 删除商品分类
     * 
     * @param categoryId 商品分类主键
     * @return 结果
     */
    public int deleteTCategoryByCategoryId(Long categoryId);

    /**
     * 批量删除商品分类
     * 
     * @param categoryIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTCategoryByCategoryIds(String[] categoryIds);
}
