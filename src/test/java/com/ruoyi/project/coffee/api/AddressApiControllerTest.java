package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.coffee.address.domain.TAddress;
import com.ruoyi.project.coffee.address.service.ITAddressService;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class AddressApiControllerTest
{
    private AddressApiController controller;

    @Mock
    private ITAddressService addressService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new AddressApiController();
        ReflectionTestUtils.setField(controller, "addressService", addressService);
        bindUser(10L);
    }

    @AfterEach
    void tearDown()
    {
        WxUserAuthContext.clear();
    }

    @Test
    void getAddressListShouldQueryCurrentUserOnly()
    {
        when(addressService.selectTAddressList(any(TAddress.class))).thenReturn(Collections.emptyList());

        AjaxResult result = controller.getAddressList();

        ArgumentCaptor<TAddress> captor = ArgumentCaptor.forClass(TAddress.class);
        verify(addressService).selectTAddressList(captor.capture());
        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals(Long.valueOf(10L), captor.getValue().getUserId());
    }

    @Test
    void addAddressShouldRejectMissingLogin()
    {
        WxUserAuthContext.clear();

        AjaxResult result = controller.addAddress(new TAddress());

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("用户信息缺失", result.get(AjaxResult.MSG_TAG));
        verify(addressService, never()).insertTAddress(any(TAddress.class));
    }

    @Test
    void addDefaultAddressShouldResetExistingDefaultForCurrentUser()
    {
        TAddress incoming = address(null, 0L, 1);
        TAddress oldDefault = address(3L, 10L, 1);
        when(addressService.selectTAddressList(any(TAddress.class))).thenReturn(Arrays.asList(oldDefault));
        when(addressService.insertTAddress(incoming)).thenReturn(1);

        AjaxResult result = controller.addAddress(incoming);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals(Long.valueOf(10L), incoming.getUserId());
        assertEquals(Integer.valueOf(0), oldDefault.getIsDefault());
        verify(addressService).updateTAddress(oldDefault);
        verify(addressService).insertTAddress(incoming);
    }

    @Test
    void updateAddressShouldRejectOtherUsersAddress()
    {
        TAddress incoming = address(8L, null, 0);
        TAddress existing = address(8L, 99L, 0);
        when(addressService.selectTAddressByAddressId(8L)).thenReturn(existing);

        AjaxResult result = controller.updateAddress(incoming);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("地址不存在", result.get(AjaxResult.MSG_TAG));
        verify(addressService, never()).updateTAddress(any(TAddress.class));
    }

    @Test
    void setDefaultAddressShouldResetOldDefaultThenSetTarget()
    {
        TAddress target = address(8L, 10L, 0);
        TAddress oldDefault = address(5L, 10L, 1);
        when(addressService.selectTAddressByAddressId(8L)).thenReturn(target);
        when(addressService.selectTAddressList(any(TAddress.class))).thenReturn(Arrays.asList(oldDefault));
        when(addressService.updateTAddress(any(TAddress.class))).thenReturn(1);

        AjaxResult result = controller.setDefaultAddress(8L);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals(Integer.valueOf(0), oldDefault.getIsDefault());
        assertEquals(Integer.valueOf(1), target.getIsDefault());
        verify(addressService).updateTAddress(oldDefault);
        verify(addressService).updateTAddress(target);
    }

    @Test
    void deleteAddressShouldRejectOtherUsersAddress()
    {
        TAddress existing = address(9L, 99L, 0);
        when(addressService.selectTAddressByAddressId(9L)).thenReturn(existing);

        AjaxResult result = controller.deleteAddress(9L);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("地址不存在", result.get(AjaxResult.MSG_TAG));
        verify(addressService, never()).deleteTAddressByAddressId(9L);
    }

    private TAddress address(Long addressId, Long userId, Integer isDefault)
    {
        TAddress address = new TAddress();
        address.setAddressId(addressId);
        address.setUserId(userId);
        address.setReceiverName("测试用户");
        address.setReceiverPhone("13800000000");
        address.setProvince("广东省");
        address.setCity("广州市");
        address.setDistrict("天河区");
        address.setDetailAddress("测试路 1 号");
        address.setIsDefault(isDefault);
        return address;
    }

    private void bindUser(Long userId)
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(userId);
        user.setOpenid("openid-" + userId);
        WxUserAuthContext.setCurrentUser(user);
    }
}
