package com.ruoyi.project.coffee.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.address.domain.TAddress;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.address.service.ITAddressService;

/**
 * 小程序地址接口
 */
@RestController
@RequestMapping("/api/address")
public class AddressApiController extends BaseController
{
    @Autowired
    private ITAddressService addressService;

    @GetMapping("/list")
    public AjaxResult getAddressList()
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        TAddress query = new TAddress();
        query.setUserId(userId);
        List<TAddress> list = addressService.selectTAddressList(query);
        return AjaxResult.success(list);
    }

    @GetMapping("/default")
    public AjaxResult getDefaultAddress()
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        TAddress query = new TAddress();
        query.setUserId(userId);
        query.setIsDefault(1);
        List<TAddress> list = addressService.selectTAddressList(query);
        if (list != null && !list.isEmpty())
        {
            return AjaxResult.success(list.get(0));
        }
        return AjaxResult.error("未设置默认地址");
    }

    @GetMapping("/{addressId}")
    public AjaxResult getAddressDetail(@PathVariable Long addressId)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        TAddress address = addressService.selectTAddressByAddressId(addressId);
        if (address == null || !userId.equals(address.getUserId()))
        {
            return AjaxResult.error("地址不存在");
        }
        return AjaxResult.success(address);
    }

    @PostMapping("/add")
    public AjaxResult addAddress(@RequestBody TAddress address)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("用户信息缺失");
        }
        address.setUserId(userId);

        resetDefaultAddress(address);
        return toAjax(addressService.insertTAddress(address));
    }

    @PutMapping("/update")
    public AjaxResult updateAddress(@RequestBody TAddress address)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (address.getAddressId() == null || userId == null)
        {
            return AjaxResult.error("地址参数不完整");
        }
        address.setUserId(userId);

        TAddress current = addressService.selectTAddressByAddressId(address.getAddressId());
        if (current == null || !userId.equals(current.getUserId()))
        {
            return AjaxResult.error("地址不存在");
        }

        resetDefaultAddress(address);
        return toAjax(addressService.updateTAddress(address));
    }

    @DeleteMapping("/{addressId}")
    public AjaxResult deleteAddress(@PathVariable Long addressId)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        TAddress address = addressService.selectTAddressByAddressId(addressId);
        if (address == null || !userId.equals(address.getUserId()))
        {
            return AjaxResult.error("地址不存在");
        }
        return toAjax(addressService.deleteTAddressByAddressId(addressId));
    }

    @PutMapping("/setDefault/{addressId}")
    public AjaxResult setDefaultAddress(@PathVariable Long addressId)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        TAddress targetAddress = addressService.selectTAddressByAddressId(addressId);
        if (targetAddress == null || !userId.equals(targetAddress.getUserId()))
        {
            return AjaxResult.error("地址不存在");
        }

        TAddress query = new TAddress();
        query.setUserId(userId);
        query.setIsDefault(1);
        List<TAddress> list = addressService.selectTAddressList(query);
        for (TAddress item : list)
        {
            item.setIsDefault(0);
            addressService.updateTAddress(item);
        }

        targetAddress.setIsDefault(1);
        return toAjax(addressService.updateTAddress(targetAddress));
    }

    private void resetDefaultAddress(TAddress address)
    {
        if (!Integer.valueOf(1).equals(address.getIsDefault()))
        {
            return;
        }

        TAddress query = new TAddress();
        query.setUserId(address.getUserId());
        query.setIsDefault(1);
        List<TAddress> list = addressService.selectTAddressList(query);
        for (TAddress item : list)
        {
            if (address.getAddressId() == null || !item.getAddressId().equals(address.getAddressId()))
            {
                item.setIsDefault(0);
                addressService.updateTAddress(item);
            }
        }
    }
}
