package com.ruoyi.project.coffee.product.service;

import java.util.List;
import com.ruoyi.project.coffee.product.domain.TProduct;

/**
 * 商品Service接口
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
public interface ITProductService 
{
    /**
     * 查询商品
     * 
     * @param productId 商品主键
     * @return 商品
     */
    public TProduct selectTProductByProductId(Long productId);

    /**
     * 查询商品列表
     * 
     * @param tProduct 商品
     * @return 商品集合
     */
    public List<TProduct> selectTProductList(TProduct tProduct);

    /**
     * 新增商品
     * 
     * @param tProduct 商品
     * @return 结果
     */
    public int insertTProduct(TProduct tProduct);

    /**
     * 修改商品
     * 
     * @param tProduct 商品
     * @return 结果
     */
    public int updateTProduct(TProduct tProduct);

    /**
     * 原子扣减库存
     *
     * @param productId 商品主键
     * @param quantity 扣减数量
     * @return 结果
     */
    public int decreaseStock(Long productId, Long quantity);

    /**
     * 回补库存
     *
     * @param productId 商品主键
     * @param quantity 回补数量
     * @return 结果
     */
    public int increaseStock(Long productId, Long quantity);

    /**
     * 批量查询商品
     *
     * @param productIds 商品ID集合
     * @return 商品集合
     */
    public List<TProduct> selectTProductByProductIds(List<Long> productIds);

    /**
     * 批量删除商品
     *
     * @param productIds 需要删除的商品主键集合
     * @return 结果
     */
    public int deleteTProductByProductIds(String productIds);

    /**
     * 删除商品信息
     * 
     * @param productId 商品主键
     * @return 结果
     */
    public int deleteTProductByProductId(Long productId);
}
