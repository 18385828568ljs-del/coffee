package com.ruoyi.project.coffee.address.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.coffee.address.mapper.TAddressMapper;
import com.ruoyi.project.coffee.address.domain.TAddress;
import com.ruoyi.project.coffee.address.service.ITAddressService;
import com.ruoyi.common.utils.text.Convert;

/**
 * 收货地址Service业务层处理
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Service
public class TAddressServiceImpl implements ITAddressService 
{
    @Autowired
    private TAddressMapper tAddressMapper;

    /**
     * 查询收货地址
     * 
     * @param addressId 收货地址主键
     * @return 收货地址
     */
    @Override
    public TAddress selectTAddressByAddressId(Long addressId)
    {
        return tAddressMapper.selectTAddressByAddressId(addressId);
    }

    /**
     * 查询收货地址列表
     * 
     * @param tAddress 收货地址
     * @return 收货地址
     */
    @Override
    public List<TAddress> selectTAddressList(TAddress tAddress)
    {
        return tAddressMapper.selectTAddressList(tAddress);
    }

    /**
     * 新增收货地址
     * 
     * @param tAddress 收货地址
     * @return 结果
     */
    @Override
    public int insertTAddress(TAddress tAddress)
    {
        tAddress.setCreateTime(DateUtils.getNowDate());
        return tAddressMapper.insertTAddress(tAddress);
    }

    /**
     * 修改收货地址
     * 
     * @param tAddress 收货地址
     * @return 结果
     */
    @Override
    public int updateTAddress(TAddress tAddress)
    {
        tAddress.setUpdateTime(DateUtils.getNowDate());
        return tAddressMapper.updateTAddress(tAddress);
    }

    /**
     * 批量删除收货地址
     * 
     * @param addressIds 需要删除的收货地址主键
     * @return 结果
     */
    @Override
    public int deleteTAddressByAddressIds(String addressIds)
    {
        return tAddressMapper.deleteTAddressByAddressIds(Convert.toStrArray(addressIds));
    }

    /**
     * 删除收货地址信息
     * 
     * @param addressId 收货地址主键
     * @return 结果
     */
    @Override
    public int deleteTAddressByAddressId(Long addressId)
    {
        return tAddressMapper.deleteTAddressByAddressId(addressId);
    }
}
