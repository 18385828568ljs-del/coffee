package com.ruoyi.project.coffee.cart.service;

import java.util.List;
import com.ruoyi.project.coffee.cart.domain.TCart;

/**
 * 购物车Service接口
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
public interface ITCartService 
{
    /**
     * 查询购物车
     * 
     * @param cartId 购物车主键
     * @return 购物车
     */
    public TCart selectTCartByCartId(Long cartId);

    /**
     * 查询购物车列表
     * 
     * @param tCart 购物车
     * @return 购物车集合
     */
    public List<TCart> selectTCartList(TCart tCart);

    /**
     * 清理指定用户购物车中已失效的商品记录。
     *
     * @param userId 用户ID
     * @return 清理数量
     */
    public int deleteInvalidTCartByUserId(Long userId);

    /**
     * 新增购物车
     * 
     * @param tCart 购物车
     * @return 结果
     */
    public int insertTCart(TCart tCart);

    /**
     * 修改购物车
     * 
     * @param tCart 购物车
     * @return 结果
     */
    public int updateTCart(TCart tCart);

    /**
     * 批量删除购物车
     * 
     * @param cartIds 需要删除的购物车主键集合
     * @return 结果
     */
    public int deleteTCartByCartIds(String cartIds);

    /**
     * 删除购物车信息
     * 
     * @param cartId 购物车主键
     * @return 结果
     */
    public int deleteTCartByCartId(Long cartId);

    /**
     * 按商品ID批量删除购物车记录。
     *
     * @param productIds 商品ID集合
     * @return 结果
     */
    public int deleteTCartByProductIds(Long[] productIds);
}
