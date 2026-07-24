package com.ruoyi.project.coffee.cart.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.coffee.cart.mapper.TCartMapper;
import com.ruoyi.project.coffee.cart.domain.TCart;
import com.ruoyi.project.coffee.cart.service.ITCartService;
import com.ruoyi.common.utils.text.Convert;

/**
 * 购物车Service业务层处理
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Service
public class TCartServiceImpl implements ITCartService 
{
    @Autowired
    private TCartMapper tCartMapper;

    /**
     * 查询购物车
     * 
     * @param cartId 购物车主键
     * @return 购物车
     */
    @Override
    public TCart selectTCartByCartId(Long cartId)
    {
        return tCartMapper.selectTCartByCartId(cartId);
    }

    /**
     * 查询购物车列表
     * 
     * @param tCart 购物车
     * @return 购物车
     */
    @Override
    public List<TCart> selectTCartList(TCart tCart)
    {
        return tCartMapper.selectTCartList(tCart);
    }

    @Override
    public int deleteInvalidTCartByUserId(Long userId)
    {
        return tCartMapper.deleteInvalidTCartByUserId(userId);
    }

    /**
     * 新增购物车
     * 
     * @param tCart 购物车
     * @return 结果
     */
    @Override
    public int insertTCart(TCart tCart)
    {
        tCart.setCreateTime(DateUtils.getNowDate());
        return tCartMapper.insertTCart(tCart);
    }

    /**
     * 修改购物车
     * 
     * @param tCart 购物车
     * @return 结果
     */
    @Override
    public int updateTCart(TCart tCart)
    {
        tCart.setUpdateTime(DateUtils.getNowDate());
        return tCartMapper.updateTCart(tCart);
    }

    /**
     * 批量删除购物车
     * 
     * @param cartIds 需要删除的购物车主键
     * @return 结果
     */
    @Override
    public int deleteTCartByCartIds(String cartIds)
    {
        return tCartMapper.deleteTCartByCartIds(Convert.toStrArray(cartIds));
    }

    /**
     * 删除购物车信息
     * 
     * @param cartId 购物车主键
     * @return 结果
     */
    @Override
    public int deleteTCartByCartId(Long cartId)
    {
        return tCartMapper.deleteTCartByCartId(cartId);
    }

    @Override
    public int deleteTCartByProductIds(Long[] productIds)
    {
        if (productIds == null || productIds.length == 0)
        {
            return 0;
        }
        return tCartMapper.deleteTCartByProductIds(productIds);
    }
}
