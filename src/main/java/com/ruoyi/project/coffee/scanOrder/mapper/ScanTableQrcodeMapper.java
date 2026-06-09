package com.ruoyi.project.coffee.scanOrder.mapper;

import java.util.List;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;

/**
 * 扫码点单-桌台二维码Mapper接口
 */
public interface ScanTableQrcodeMapper
{
    ScanTableQrcode selectByShopAndTable(ScanTableQrcode query);

    ScanTableQrcode selectById(Long tableId);

    List<ScanTableQrcode> selectList(ScanTableQrcode query);

    int insert(ScanTableQrcode record);

    int update(ScanTableQrcode record);

    int deleteById(Long tableId);
}
