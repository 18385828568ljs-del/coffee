package com.ruoyi.project.coffee.order.mapper;

import java.util.List;
import java.util.Date;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.order.domain.TOrder;

/**
 * 订单Mapper接口
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
public interface TOrderMapper 
{
    /**
     * 查询订单
     * 
     * @param orderId 订单主键
     * @return 订单
     */
    public TOrder selectTOrderByOrderId(Long orderId);

    /**
     * 查询订单列表
     * 
     * @param tOrder 订单
     * @return 订单集合
     */
    public List<TOrder> selectTOrderList(TOrder tOrder);

    /**
     * 新增订单
     * 
     * @param tOrder 订单
     * @return 结果
     */
    public int insertTOrder(TOrder tOrder);

    /**
     * 修改订单
     * 
     * @param tOrder 订单
     * @return 结果
     */
    public int updateTOrder(TOrder tOrder);

    /**
     * 按预期状态更新订单状态，避免重复流转
     *
     * @param orderId 订单ID
     * @param oldStatus 预期原状态
     * @param newStatus 新状态
     * @param payTime 支付时间
     * @param shipTime 发货时间
     * @param finishTime 完成时间
     * @param cancelTime 取消时间
     * @param expressNo 物流单号
     * @param remark 备注
     * @return 结果
     */
    public int updateOrderStatus(@Param("orderId") Long orderId,
                                 @Param("oldStatus") Integer oldStatus,
                                 @Param("newStatus") Integer newStatus,
                                 @Param("payTime") Date payTime,
                                 @Param("shipTime") Date shipTime,
                                 @Param("finishTime") Date finishTime,
                                 @Param("cancelTime") Date cancelTime,
                                 @Param("expressNo") String expressNo,
                                 @Param("remark") String remark);

    /**
     * 统计用户已完成订单数量（用于判断是否新用户）
     *
     * @param userId 用户ID
     * @return 订单数量
     */
    public int countCompletedOrdersByUserId(@Param("userId") Long userId);

    /**
     * 删除订单
     *
     * @param orderId 订单主键
     * @return 结果
     */
    public int deleteTOrderByOrderId(Long orderId);

    /**
     * 批量删除订单
     * 
     * @param orderIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTOrderByOrderIds(String[] orderIds);
}
