package com.ruoyi.project.coffee.item.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.coffee.item.mapper.TOrderItemMapper;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import com.ruoyi.project.coffee.item.service.ITOrderItemService;
import com.ruoyi.common.utils.text.Convert;

/**
 * 订单明细Service业务层处理
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Service
public class TOrderItemServiceImpl implements ITOrderItemService 
{
    @Autowired
    private TOrderItemMapper tOrderItemMapper;

    /**
     * 查询订单明细
     * 
     * @param itemId 订单明细主键
     * @return 订单明细
     */
    @Override
    public TOrderItem selectTOrderItemByItemId(Long itemId)
    {
        return tOrderItemMapper.selectTOrderItemByItemId(itemId);
    }

    /**
     * 查询订单明细列表
     * 
     * @param tOrderItem 订单明细
     * @return 订单明细
     */
    @Override
    public List<TOrderItem> selectTOrderItemList(TOrderItem tOrderItem)
    {
        return tOrderItemMapper.selectTOrderItemList(tOrderItem);
    }

    @Override
    public List<TOrderItem> selectTOrderItemListByOrderIds(List<Long> orderIds)
    {
        return tOrderItemMapper.selectTOrderItemListByOrderIds(orderIds);
    }

    /**
     * 新增订单明细
     * 
     * @param tOrderItem 订单明细
     * @return 结果
     */
    @Override
    public int insertTOrderItem(TOrderItem tOrderItem)
    {
        return tOrderItemMapper.insertTOrderItem(tOrderItem);
    }

    /**
     * 修改订单明细
     * 
     * @param tOrderItem 订单明细
     * @return 结果
     */
    @Override
    public int updateTOrderItem(TOrderItem tOrderItem)
    {
        return tOrderItemMapper.updateTOrderItem(tOrderItem);
    }

    /**
     * 批量删除订单明细
     * 
     * @param itemIds 需要删除的订单明细主键
     * @return 结果
     */
    @Override
    public int deleteTOrderItemByItemIds(String itemIds)
    {
        return tOrderItemMapper.deleteTOrderItemByItemIds(Convert.toStrArray(itemIds));
    }

    /**
     * 删除订单明细信息
     * 
     * @param itemId 订单明细主键
     * @return 结果
     */
    @Override
    public int deleteTOrderItemByItemId(Long itemId)
    {
        return tOrderItemMapper.deleteTOrderItemByItemId(itemId);
    }

    @Override
    public int deleteTOrderItemByOrderId(Long orderId)
    {
        return tOrderItemMapper.deleteTOrderItemByOrderId(orderId);
    }

    @Override
    public int deleteTOrderItemByOrderIds(String orderIds)
    {
        return tOrderItemMapper.deleteTOrderItemByOrderIds(Convert.toStrArray(orderIds));
    }
}
