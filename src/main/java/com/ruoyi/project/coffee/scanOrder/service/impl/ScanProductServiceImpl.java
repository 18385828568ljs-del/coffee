package com.ruoyi.project.coffee.scanOrder.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.text.Convert;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpec;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecOptionMapper;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;

/**
 * 扫码点单-商品Service实现
 */
@Service
public class ScanProductServiceImpl implements IScanProductService
{
    @Autowired
    private ScanProductMapper scanProductMapper;

    @Autowired
    private ScanProductSpecMapper scanProductSpecMapper;

    @Autowired
    private ScanProductSpecOptionMapper scanProductSpecOptionMapper;

    @Override
    public ScanProduct selectScanProductById(Long productId)
    {
        return scanProductMapper.selectScanProductById(productId);
    }

    @Override
    public List<ScanProduct> selectScanProductList(ScanProduct scanProduct)
    {
        return scanProductMapper.selectScanProductList(scanProduct);
    }

    @Override
    public ScanProduct selectScanProductWithSpecs(Long productId)
    {
        ScanProduct product = scanProductMapper.selectScanProductById(productId);
        if (product == null)
        {
            return null;
        }

        List<ScanProductSpec> specs = scanProductSpecMapper.selectSpecListByProductId(productId);
        if (specs == null || specs.isEmpty())
        {
            product.setSpecs(new ArrayList<ScanProductSpec>());
            return product;
        }

        List<ScanProductSpecOption> allOptions = scanProductSpecOptionMapper.selectOptionListByProductId(productId);
        Map<Long, List<ScanProductSpecOption>> optionsBySpec = new HashMap<Long, List<ScanProductSpecOption>>();
        if (allOptions != null)
        {
            for (ScanProductSpecOption option : allOptions)
            {
                if (option == null || option.getSpecId() == null)
                {
                    continue;
                }
                List<ScanProductSpecOption> bucket = optionsBySpec.get(option.getSpecId());
                if (bucket == null)
                {
                    bucket = new ArrayList<ScanProductSpecOption>();
                    optionsBySpec.put(option.getSpecId(), bucket);
                }
                bucket.add(option);
            }
        }

        for (ScanProductSpec spec : specs)
        {
            List<ScanProductSpecOption> bucket = optionsBySpec.get(spec.getSpecId());
            spec.setOptions(bucket == null ? new ArrayList<ScanProductSpecOption>() : bucket);
        }

        product.setSpecs(specs);
        return product;
    }

    @Override
    public int insertScanProduct(ScanProduct scanProduct)
    {
        if (scanProduct.getStatus() == null)
        {
            scanProduct.setStatus(1);
        }
        if (scanProduct.getSortOrder() == null)
        {
            scanProduct.setSortOrder(0);
        }
        if (scanProduct.getMonthSales() == null)
        {
            scanProduct.setMonthSales(0);
        }
        scanProduct.setCreateTime(DateUtils.getNowDate());
        return scanProductMapper.insertScanProduct(scanProduct);
    }

    @Override
    public int updateScanProduct(ScanProduct scanProduct)
    {
        if (scanProduct.getStatus() == null)
        {
            scanProduct.setStatus(1);
        }
        if (scanProduct.getSortOrder() == null)
        {
            scanProduct.setSortOrder(0);
        }
        if (scanProduct.getMonthSales() == null)
        {
            scanProduct.setMonthSales(0);
        }
        scanProduct.setUpdateTime(DateUtils.getNowDate());
        return scanProductMapper.updateScanProduct(scanProduct);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteScanProductByIds(String ids)
    {
        String[] productIds = Convert.toStrArray(ids);
        scanProductSpecOptionMapper.deleteOptionByProductIds(productIds);
        scanProductSpecMapper.deleteSpecByProductIds(productIds);
        return scanProductMapper.deleteScanProductByIds(productIds);
    }
}
