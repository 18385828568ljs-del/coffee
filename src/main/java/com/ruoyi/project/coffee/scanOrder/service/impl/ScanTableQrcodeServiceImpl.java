package com.ruoyi.project.coffee.scanOrder.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.text.Convert;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanTableQrcodeMapper;
import com.ruoyi.project.coffee.scanOrder.service.IScanTableQrcodeService;
import com.ruoyi.project.coffee.scanOrder.wx.WxaCodeService;

/**
 * 扫码点单-桌台二维码Service实现
 */
@Service
public class ScanTableQrcodeServiceImpl implements IScanTableQrcodeService
{
    private static final Logger log = LoggerFactory.getLogger(ScanTableQrcodeServiceImpl.class);

    private static final long DEFAULT_SHOP_ID = 1L;
    private static final String DEFAULT_SCENE = "dine_in";
    private static final String SCAN_PAGE = "pages/scan/menu";

    @Autowired
    private ScanTableQrcodeMapper scanTableQrcodeMapper;

    @Autowired
    private WxaCodeService wxaCodeService;

    @Override
    public ScanTableQrcode selectByShopAndTable(Long shopId, String tableNo)
    {
        if (StringUtils.isEmpty(tableNo))
        {
            return null;
        }
        ScanTableQrcode query = new ScanTableQrcode();
        query.setShopId(shopId);
        query.setTableNo(tableNo.trim());
        return scanTableQrcodeMapper.selectByShopAndTable(query);
    }

    @Override
    public ScanTableQrcode selectById(Long tableId)
    {
        if (tableId == null) return null;
        return scanTableQrcodeMapper.selectById(tableId);
    }

    @Override
    public List<ScanTableQrcode> selectList(ScanTableQrcode query)
    {
        return scanTableQrcodeMapper.selectList(query == null ? new ScanTableQrcode() : query);
    }

    @Override
    public int insertScanTableQrcode(ScanTableQrcode scanTableQrcode)
    {
        normalizeScanTable(scanTableQrcode, true);
        return scanTableQrcodeMapper.insert(scanTableQrcode);
    }

    @Override
    public int updateScanTableQrcode(ScanTableQrcode scanTableQrcode)
    {
        normalizeScanTable(scanTableQrcode, false);
        return scanTableQrcodeMapper.update(scanTableQrcode);
    }

    @Override
    public int deleteScanTableQrcodeByIds(String ids)
    {
        int rows = 0;
        for (String id : Convert.toStrArray(ids))
        {
            if (StringUtils.isEmpty(id))
            {
                continue;
            }
            rows += scanTableQrcodeMapper.deleteById(Long.valueOf(id));
        }
        return rows;
    }

    @Override
    public ScanTableQrcode generateOne(Long shopId, String shopName, String tableNo, String scene)
    {
        if (StringUtils.isEmpty(tableNo))
        {
            throw new ServiceException("桌号不能为空");
        }
        Long resolvedShopId = shopId == null ? DEFAULT_SHOP_ID : shopId;
        String trimTableNo = tableNo.trim();
        String resolvedScene = StringUtils.isEmpty(scene) ? DEFAULT_SCENE : scene.trim();

        // scene 参数固定结构: shopId=X&tableNo=Y,微信侧上限 32 字节
        String wxScene = "shopId=" + resolvedShopId + "&tableNo=" + trimTableNo;
        String fileName = "shop" + resolvedShopId + "_" + trimTableNo + "_" + System.currentTimeMillis();

        String qrUrl = wxaCodeService.generateWxaCode(SCAN_PAGE, wxScene, fileName);

        Date now = DateUtils.getNowDate();
        ScanTableQrcode existing = selectByShopAndTable(resolvedShopId, trimTableNo);
        if (existing != null)
        {
            ScanTableQrcode patch = new ScanTableQrcode();
            patch.setTableId(existing.getTableId());
            if (StringUtils.isNotEmpty(shopName)) patch.setShopName(shopName);
            patch.setScene(resolvedScene);
            patch.setQrUrl(qrUrl);
            patch.setStatus(1);
            patch.setUpdateTime(now);
            scanTableQrcodeMapper.update(patch);
            return scanTableQrcodeMapper.selectById(existing.getTableId());
        }

        ScanTableQrcode record = new ScanTableQrcode();
        record.setShopId(resolvedShopId);
        record.setShopName(StringUtils.isEmpty(shopName) ? null : shopName);
        record.setTableNo(trimTableNo);
        record.setScene(resolvedScene);
        record.setQrUrl(qrUrl);
        record.setStatus(1);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        scanTableQrcodeMapper.insert(record);
        return scanTableQrcodeMapper.selectById(record.getTableId());
    }

    @Override
    public List<ScanTableQrcode> batchGenerate(Long shopId, String shopName, List<String> tableNos, String scene)
    {
        if (tableNos == null || tableNos.isEmpty())
        {
            return Collections.emptyList();
        }
        // 去重保序
        Map<String, Boolean> seen = new LinkedHashMap<String, Boolean>();
        for (String t : tableNos)
        {
            if (StringUtils.isNotEmpty(t))
            {
                seen.put(t.trim(), Boolean.TRUE);
            }
        }
        List<ScanTableQrcode> results = new ArrayList<ScanTableQrcode>(seen.size());
        for (String tableNo : seen.keySet())
        {
            try
            {
                results.add(generateOne(shopId, shopName, tableNo, scene));
            }
            catch (Exception e)
            {
                log.error("批量生成桌号 {} 二维码失败: {}", tableNo, e.getMessage());
                ScanTableQrcode failed = new ScanTableQrcode();
                failed.setShopId(shopId == null ? DEFAULT_SHOP_ID : shopId);
                failed.setTableNo(tableNo);
                failed.setRemark("生成失败: " + e.getMessage());
                results.add(failed);
            }
        }
        return results;
    }

    @Override
    public int deleteById(Long tableId)
    {
        if (tableId == null) return 0;
        return scanTableQrcodeMapper.deleteById(tableId);
    }

    private void normalizeScanTable(ScanTableQrcode scanTableQrcode, boolean creating)
    {
        if (scanTableQrcode == null)
        {
            throw new ServiceException("桌台信息不能为空");
        }
        if (scanTableQrcode.getShopId() == null)
        {
            scanTableQrcode.setShopId(DEFAULT_SHOP_ID);
        }
        String tableNo = StringUtils.trimToNull(scanTableQrcode.getTableNo());
        if (tableNo == null)
        {
            throw new ServiceException("桌号不能为空");
        }
        scanTableQrcode.setTableNo(tableNo);
        String scene = StringUtils.trimToNull(scanTableQrcode.getScene());
        scanTableQrcode.setScene(scene == null ? DEFAULT_SCENE : scene);
        if (scanTableQrcode.getStatus() == null)
        {
            scanTableQrcode.setStatus(1);
        }
        Date now = DateUtils.getNowDate();
        if (creating)
        {
            scanTableQrcode.setCreateTime(now);
        }
        scanTableQrcode.setUpdateTime(now);
    }
}
