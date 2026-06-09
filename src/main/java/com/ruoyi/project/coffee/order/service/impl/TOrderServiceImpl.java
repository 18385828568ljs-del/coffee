package com.ruoyi.project.coffee.order.service.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.coffee.item.mapper.TOrderItemMapper;
import com.ruoyi.project.coffee.order.mapper.TOrderMapper;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.order.service.ITOrderService;
import com.ruoyi.common.utils.text.Convert;

/**
 * 订单Service业务层处理
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Service
public class TOrderServiceImpl implements ITOrderService 
{
    @Autowired
    private TOrderMapper tOrderMapper;

    @Autowired
    private TOrderItemMapper tOrderItemMapper;

    /**
     * 查询订单
     * 
     * @param orderId 订单主键
     * @return 订单
     */
    @Override
    public TOrder selectTOrderByOrderId(Long orderId)
    {
        return tOrderMapper.selectTOrderByOrderId(orderId);
    }

    /**
     * 查询订单列表
     * 
     * @param tOrder 订单
     * @return 订单
     */
    @Override
    public List<TOrder> selectTOrderList(TOrder tOrder)
    {
        return tOrderMapper.selectTOrderList(tOrder);
    }

    /**
     * 新增订单
     * 
     * @param tOrder 订单
     * @return 结果
     */
    @Override
    public int insertTOrder(TOrder tOrder)
    {
        tOrder.setCreateTime(DateUtils.getNowDate());
        return tOrderMapper.insertTOrder(tOrder);
    }

    /**
     * 修改订单
     * 
     * @param tOrder 订单
     * @return 结果
     */
    @Override
    public int updateTOrder(TOrder tOrder)
    {
        return tOrderMapper.updateTOrder(tOrder);
    }

    @Override
    public int changeOrderStatus(Long orderId, Integer oldStatus, Integer newStatus, Date payTime,
                                 Date shipTime, Date finishTime, Date cancelTime, String expressNo,
                                 String remark)
    {
        return tOrderMapper.updateOrderStatus(orderId, oldStatus, newStatus, payTime, shipTime,
            finishTime, cancelTime, expressNo, remark);
    }

    /**
     * 订单发货
     *
     * @param tOrder 订单
     * @return 结果
     */
    @Override
    public int shipOrder(TOrder tOrder)
    {
        Date shipTime = tOrder.getShipTime() == null ? DateUtils.getNowDate() : tOrder.getShipTime();
        return tOrderMapper.updateOrderStatus(tOrder.getOrderId(), 1, 2, null, shipTime, null, null,
            tOrder.getExpressNo(), tOrder.getRemark());
    }

    /**
     * 统计用户已完成订单数量
     *
     * @param userId 用户ID
     * @return 订单数量
     */
    @Override
    public int countCompletedOrdersByUserId(Long userId)
    {
        return tOrderMapper.countCompletedOrdersByUserId(userId);
    }

    /**
     * 批量删除订单
     *
     * @param orderIds 需要删除的订单主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTOrderByOrderIds(String orderIds)
    {
        String[] orderIdArray = Convert.toStrArray(orderIds);
        tOrderItemMapper.deleteTOrderItemByOrderIds(orderIdArray);
        return tOrderMapper.deleteTOrderByOrderIds(orderIdArray);
    }

    /**
     * 删除订单信息
     * 
     * @param orderId 订单主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTOrderByOrderId(Long orderId)
    {
        tOrderItemMapper.deleteTOrderItemByOrderId(orderId);
        return tOrderMapper.deleteTOrderByOrderId(orderId);
    }
}
