package com.ruoyi.project.coffee.scanOrder.service;

import java.util.List;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderItem;

/**
 * 扫码点单-订单明细Service接口
 */
public interface IScanOrderItemService
{
    List<ScanOrderItem> selectItemsByOrderId(Long orderId);
}
