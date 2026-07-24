package com.ruoyi.project.coffee.scanOrder.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderItem;

/**
 * 扫码点单-订单明细Mapper接口
 */
public interface ScanOrderItemMapper
{
    List<ScanOrderItem> selectItemsByOrderId(Long orderId);

    List<ScanOrderItem> selectItemsByOrderIds(@Param("orderIds") List<Long> orderIds);

    int insertScanOrderItem(ScanOrderItem item);

    int batchInsertScanOrderItem(List<ScanOrderItem> items);
}
