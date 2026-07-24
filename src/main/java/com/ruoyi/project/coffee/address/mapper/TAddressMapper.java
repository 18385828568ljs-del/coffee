package com.ruoyi.project.coffee.address.mapper;

import java.util.List;
import com.ruoyi.project.coffee.address.domain.TAddress;

/**
 * 收货地址Mapper接口
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
public interface TAddressMapper 
{
    /**
     * 查询收货地址
     * 
     * @param addressId 收货地址主键
     * @return 收货地址
     */
    public TAddress selectTAddressByAddressId(Long addressId);

    /**
     * 查询收货地址列表
     * 
     * @param tAddress 收货地址
     * @return 收货地址集合
     */
    public List<TAddress> selectTAddressList(TAddress tAddress);

    /**
     * 新增收货地址
     * 
     * @param tAddress 收货地址
     * @return 结果
     */
    public int insertTAddress(TAddress tAddress);

    /**
     * 修改收货地址
     * 
     * @param tAddress 收货地址
     * @return 结果
     */
    public int updateTAddress(TAddress tAddress);

    /**
     * 删除收货地址
     * 
     * @param addressId 收货地址主键
     * @return 结果
     */
    public int deleteTAddressByAddressId(Long addressId);

    /**
     * 批量删除收货地址
     * 
     * @param addressIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTAddressByAddressIds(String[] addressIds);
}
