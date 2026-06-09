package com.ruoyi.project.coffee.product.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.coffee.product.mapper.TProductMapper;
import com.ruoyi.project.coffee.cart.service.ITCartService;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.common.utils.text.Convert;

/**
 * 商品Service业务层处理
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Service
public class TProductServiceImpl implements ITProductService 
{
    @Autowired
    private TProductMapper tProductMapper;

    @Autowired
    private ITCartService tCartService;

    /**
     * 查询商品
     * 
     * @param productId 商品主键
     * @return 商品
     */
    @Override
    public TProduct selectTProductByProductId(Long productId)
    {
        return tProductMapper.selectTProductByProductId(productId);
    }

    /**
     * 查询商品列表
     * 
     * @param tProduct 商品
     * @return 商品
     */
    @Override
    public List<TProduct> selectTProductList(TProduct tProduct)
    {
        return tProductMapper.selectTProductList(tProduct);
    }

    /**
     * 新增商品
     * 
     * @param tProduct 商品
     * @return 结果
     */
    @Override
    public int insertTProduct(TProduct tProduct)
    {
        tProduct.setCreateTime(DateUtils.getNowDate());
        return tProductMapper.insertTProduct(tProduct);
    }

    /**
     * 修改商品
     * 
     * @param tProduct 商品
     * @return 结果
     */
    @Override
    public int updateTProduct(TProduct tProduct)
    {
        tProduct.setUpdateTime(DateUtils.getNowDate());
        return tProductMapper.updateTProduct(tProduct);
    }

    @Override
    public int decreaseStock(Long productId, Long quantity)
    {
        return tProductMapper.decreaseStock(productId, quantity);
    }

    @Override
    public int increaseStock(Long productId, Long quantity)
    {
        return tProductMapper.increaseStock(productId, quantity);
    }

    @Override
    public List<TProduct> selectTProductByProductIds(List<Long> productIds)
    {
        if (productIds == null || productIds.isEmpty())
        {
            return java.util.Collections.emptyList();
        }
        return tProductMapper.selectTProductByProductIds(productIds);
    }

    /**
     * 批量删除商品
     *
     * @param productIds 需要删除的商品主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTProductByProductIds(String productIds)
    {
        tCartService.deleteTCartByProductIds(Convert.toLongArray(productIds));
        return tProductMapper.deleteTProductByProductIds(Convert.toStrArray(productIds));
    }

    /**
     * 删除商品信息
     * 
     * @param productId 商品主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTProductByProductId(Long productId)
    {
        tCartService.deleteTCartByProductIds(new Long[] { productId });
        return tProductMapper.deleteTProductByProductId(productId);
    }
}
