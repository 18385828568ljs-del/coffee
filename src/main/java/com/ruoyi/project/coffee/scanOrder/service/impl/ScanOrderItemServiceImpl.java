package com.ruoyi.project.coffee.scanOrder.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderItem;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanOrderItemMapper;
import com.ruoyi.project.coffee.scanOrder.service.IScanOrderItemService;

/**
 * 扫码点单-订单明细Service实现
 */
@Service
public class ScanOrderItemServiceImpl implements IScanOrderItemService
{
    @Autowired
    private ScanOrderItemMapper scanOrderItemMapper;

    @Override
    public List<ScanOrderItem> selectItemsByOrderId(Long orderId)
    {
        return scanOrderItemMapper.selectItemsByOrderId(orderId);
    }
}
