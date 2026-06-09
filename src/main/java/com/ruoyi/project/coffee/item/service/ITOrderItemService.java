package com.ruoyi.project.coffee.item.service;

import java.util.List;
import com.ruoyi.project.coffee.item.domain.TOrderItem;

/**
 * 订单明细Service接口
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
public interface ITOrderItemService 
{
    /**
     * 查询订单明细
     * 
     * @param itemId 订单明细主键
     * @return 订单明细
     */
    public TOrderItem selectTOrderItemByItemId(Long itemId);

    /**
     * 查询订单明细列表
     * 
     * @param tOrderItem 订单明细
     * @return 订单明细集合
     */
    public List<TOrderItem> selectTOrderItemList(TOrderItem tOrderItem);

    /**
     * 按订单ID批量查询订单明细
     *
     * @param orderIds 订单ID集合
     * @return 订单明细集合
     */
    public List<TOrderItem> selectTOrderItemListByOrderIds(List<Long> orderIds);

    /**
     * 新增订单明细
     * 
     * @param tOrderItem 订单明细
     * @return 结果
     */
    public int insertTOrderItem(TOrderItem tOrderItem);

    /**
     * 修改订单明细
     * 
     * @param tOrderItem 订单明细
     * @return 结果
     */
    public int updateTOrderItem(TOrderItem tOrderItem);

    /**
     * 批量删除订单明细
     * 
     * @param itemIds 需要删除的订单明细主键集合
     * @return 结果
     */
    public int deleteTOrderItemByItemIds(String itemIds);

    /**
     * 删除订单明细信息
     * 
     * @param itemId 订单明细主键
     * @return 结果
     */
    public int deleteTOrderItemByItemId(Long itemId);

    /**
     * 删除订单对应的全部明细
     *
     * @param orderId 订单ID
     * @return 结果
     */
    public int deleteTOrderItemByOrderId(Long orderId);

    /**
     * 批量删除订单对应的全部明细
     *
     * @param orderIds 订单ID字符串
     * @return 结果
     */
    public int deleteTOrderItemByOrderIds(String orderIds);
}
