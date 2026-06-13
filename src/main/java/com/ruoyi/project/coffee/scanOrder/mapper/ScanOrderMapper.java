package com.ruoyi.project.coffee.scanOrder.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;

/**
 * 扫码点单-订单Mapper接口
 */
public interface ScanOrderMapper
{
    ScanOrder selectScanOrderById(Long orderId);

    ScanOrder selectScanOrderByOrderNo(String orderNo);

    List<ScanOrder> selectScanOrderList(ScanOrder query);

    Integer selectMaxPickupNoToday(@Param("shopId") Long shopId);

    int countUnfinishedPaidOrders(@Param("shopId") Long shopId);

    int insertScanOrder(ScanOrder scanOrder);

    /** 状态流转:乐观更新,仅当 oldStatus 匹配时才更新 */
    int updateScanOrderStatus(@Param("orderId") Long orderId,
                              @Param("oldStatus") Integer oldStatus,
                              @Param("newStatus") Integer newStatus,
                              @Param("payTime") Date payTime,
                              @Param("acceptTime") Date acceptTime,
                              @Param("makingTime") Date makingTime,
                              @Param("callTime") Date callTime,
                              @Param("finishTime") Date finishTime,
                              @Param("cancelTime") Date cancelTime,
                              @Param("pickupNo") String pickupNo,
                              @Param("estimatedWaitMinutes") Integer estimatedWaitMinutes,
                              @Param("payType") String payType);

    int markUrged(@Param("orderId") Long orderId, @Param("lastUrgeTime") Date lastUrgeTime);
}
