package com.ruoyi.project.coffee.scanOrder.service;

import java.util.List;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;

/**
 * 扫码点单-桌台二维码Service接口
 */
public interface IScanTableQrcodeService
{
    ScanTableQrcode selectByTableNo(String tableNo);

    ScanTableQrcode selectById(Long tableId);

    List<ScanTableQrcode> selectList(ScanTableQrcode query);

    int insertScanTableQrcode(ScanTableQrcode scanTableQrcode);

    int updateScanTableQrcode(ScanTableQrcode scanTableQrcode);

    int deleteScanTableQrcodeByIds(String ids);

    /**
     * 生成或重生成单个桌台的小程序码
     *
     * @param tableNo  桌号
     * @param scene    可选;为空时默认 dine_in
     * @return 落库后的最新记录
     */
    ScanTableQrcode generateOne(String tableNo, String scene);

    /**
     * 批量生成
     *
     * @param tableNos  桌号列表
     * @param scene     可选场景
     * @return 受影响的桌台记录列表(按入参顺序,失败项的 qrUrl 为空)
     */
    List<ScanTableQrcode> batchGenerate(List<String> tableNos, String scene);

    int deleteById(Long tableId);
}
