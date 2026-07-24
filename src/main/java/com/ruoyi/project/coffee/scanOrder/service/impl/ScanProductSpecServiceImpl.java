package com.ruoyi.project.coffee.scanOrder.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpec;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecMapper;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductSpecService;

/**
 * 扫码点单-商品规格组Service实现
 */
@Service
public class ScanProductSpecServiceImpl implements IScanProductSpecService
{
    @Autowired
    private ScanProductSpecMapper scanProductSpecMapper;

    @Override
    public List<ScanProductSpec> selectSpecListByProductId(Long productId)
    {
        return scanProductSpecMapper.selectSpecListByProductId(productId);
    }
}
