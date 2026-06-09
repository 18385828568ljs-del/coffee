package com.ruoyi.project.coffee.scanOrder.service.impl;

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecOptionMapper;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductSpecOptionService;

/**
 * 扫码点单-规格选项Service实现
 */
@Service
public class ScanProductSpecOptionServiceImpl implements IScanProductSpecOptionService
{
    @Autowired
    private ScanProductSpecOptionMapper scanProductSpecOptionMapper;

    @Override
    public List<ScanProductSpecOption> selectOptionListByProductId(Long productId)
    {
        return scanProductSpecOptionMapper.selectOptionListByProductId(productId);
    }

    @Override
    public List<ScanProductSpecOption> selectOptionListBySpecId(Long specId)
    {
        return scanProductSpecOptionMapper.selectOptionListBySpecId(specId);
    }

    @Override
    public List<ScanProductSpecOption> selectOptionListByIds(List<Long> optionIds)
    {
        if (optionIds == null || optionIds.isEmpty())
        {
            return Collections.emptyList();
        }
        return scanProductSpecOptionMapper.selectOptionListByIds(optionIds);
    }
}
