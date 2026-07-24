package com.ruoyi.project.coffee.address.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.address.domain.TAddress;
import com.ruoyi.project.coffee.address.mapper.TAddressMapper;
import com.ruoyi.project.coffee.address.service.impl.TAddressServiceImpl;

class TAddressServiceImplTest
{
    @Mock
    private TAddressMapper tAddressMapper;

    private TAddressServiceImpl tAddressService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        tAddressService = new TAddressServiceImpl();
        ReflectionTestUtils.setField(tAddressService, "tAddressMapper", tAddressMapper);
    }

    @Test
    void selectShouldDelegateToMapper()
    {
        TAddress address = address(1L);
        when(tAddressMapper.selectTAddressByAddressId(1L)).thenReturn(address);

        TAddress result = tAddressService.selectTAddressByAddressId(1L);

        assertSame(address, result);
        verify(tAddressMapper).selectTAddressByAddressId(1L);
    }

    @Test
    void selectListShouldDelegateToMapper()
    {
        TAddress query = address(null);
        List<TAddress> addresses = Collections.singletonList(address(1L));
        when(tAddressMapper.selectTAddressList(query)).thenReturn(addresses);

        List<TAddress> result = tAddressService.selectTAddressList(query);

        assertSame(addresses, result);
        verify(tAddressMapper).selectTAddressList(query);
    }

    @Test
    void insertShouldWriteCreateTime()
    {
        TAddress address = address(null);
        when(tAddressMapper.insertTAddress(address)).thenReturn(1);

        int rows = tAddressService.insertTAddress(address);

        assertEquals(1, rows);
        assertNotNull(address.getCreateTime());
        verify(tAddressMapper).insertTAddress(address);
    }

    @Test
    void updateShouldWriteUpdateTime()
    {
        TAddress address = address(1L);
        when(tAddressMapper.updateTAddress(address)).thenReturn(1);

        int rows = tAddressService.updateTAddress(address);

        assertEquals(1, rows);
        assertNotNull(address.getUpdateTime());
        verify(tAddressMapper).updateTAddress(address);
    }

    @Test
    void deleteByAddressIdsShouldSplitAndDelegate()
    {
        when(tAddressMapper.deleteTAddressByAddressIds(any(String[].class))).thenReturn(2);

        int rows = tAddressService.deleteTAddressByAddressIds("10,20");

        assertEquals(2, rows);
        verify(tAddressMapper).deleteTAddressByAddressIds(new String[] {"10", "20"});
    }

    @Test
    void deleteByAddressIdShouldDelegate()
    {
        when(tAddressMapper.deleteTAddressByAddressId(anyLong())).thenReturn(1);

        int rows = tAddressService.deleteTAddressByAddressId(3L);

        assertEquals(1, rows);
        verify(tAddressMapper).deleteTAddressByAddressId(3L);
    }

    private static TAddress address(Long addressId)
    {
        TAddress address = new TAddress();
        address.setAddressId(addressId);
        return address;
    }
}
