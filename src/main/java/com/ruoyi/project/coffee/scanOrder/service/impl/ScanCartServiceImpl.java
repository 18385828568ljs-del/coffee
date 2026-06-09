package com.ruoyi.project.coffee.scanOrder.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanCartMapper;
import com.ruoyi.project.coffee.scanOrder.service.IScanCartService;

/**
 * 扫码点单-购物车Service实现
 */
@Service
public class ScanCartServiceImpl implements IScanCartService
{
    @Autowired
    private ScanCartMapper scanCartMapper;

    @Override
    public ScanCart selectScanCartById(Long id)
    {
        return scanCartMapper.selectScanCartById(id);
    }

    @Override
    public List<ScanCart> selectScanCartList(ScanCart query)
    {
        return scanCartMapper.selectScanCartList(query);
    }

    @Override
    public ScanCart addOrIncrease(ScanCart cart)
    {
        if (cart.getQuantity() == null || cart.getQuantity() < 1)
        {
            cart.setQuantity(1);
        }
        if (cart.getPrice() == null)
        {
            cart.setPrice(BigDecimal.ZERO);
        }
        if (cart.getStatus() == null)
        {
            cart.setStatus(1);
        }
        if (cart.getSelected() == null)
        {
            cart.setSelected(1);
        }
        if (cart.getDelFlag() == null)
        {
            cart.setDelFlag(0);
        }

        List<ScanCart> matches = scanCartMapper.selectMatchingCart(cart);
        Date now = DateUtils.getNowDate();

        if (matches != null && !matches.isEmpty())
        {
            ScanCart exist = matches.get(0);
            int newQuantity = (exist.getQuantity() == null ? 0 : exist.getQuantity()) + cart.getQuantity();
            ScanCart patch = new ScanCart();
            patch.setId(exist.getId());
            patch.setQuantity(newQuantity);
            // 价格可能因规格加价略有变化,以最新加购时的单价为准
            patch.setPrice(cart.getPrice());
            patch.setSelected(1);
            patch.setStatus(1);
            patch.setUpdateTime(now);
            scanCartMapper.updateScanCart(patch);
            return scanCartMapper.selectScanCartById(exist.getId());
        }

        cart.setCreateTime(now);
        cart.setUpdateTime(now);
        scanCartMapper.insertScanCart(cart);
        return scanCartMapper.selectScanCartById(cart.getId());
    }

    @Override
    public int updateQuantity(Long id, Integer quantity)
    {
        if (id == null)
        {
            return 0;
        }
        if (quantity == null || quantity <= 0)
        {
            return scanCartMapper.logicDeleteById(id);
        }
        ScanCart patch = new ScanCart();
        patch.setId(id);
        patch.setQuantity(quantity);
        patch.setUpdateTime(DateUtils.getNowDate());
        return scanCartMapper.updateScanCart(patch);
    }

    @Override
    public int logicDeleteById(Long id)
    {
        if (id == null)
        {
            return 0;
        }
        return scanCartMapper.logicDeleteById(id);
    }

    @Override
    public int logicDeleteByOwnerAndTable(ScanCart query)
    {
        if (query == null)
        {
            return 0;
        }
        boolean ownerMissing = query.getUserId() == null
            && (query.getOpenid() == null || query.getOpenid().trim().isEmpty());
        if (ownerMissing)
        {
            return 0;
        }
        return scanCartMapper.logicDeleteByOwnerAndTable(query);
    }
}
