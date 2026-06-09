package com.ruoyi.project.coffee.scanOrder.mapper;

import java.util.List;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;

/**
 * 扫码点单-购物车Mapper接口
 */
public interface ScanCartMapper
{
    ScanCart selectScanCartById(Long id);

    List<ScanCart> selectScanCartList(ScanCart scanCart);

    /** 查询同用户/openid + 同门店桌号 + 同商品 + 同规格的活跃记录 */
    List<ScanCart> selectMatchingCart(ScanCart scanCart);

    int insertScanCart(ScanCart scanCart);

    int updateScanCart(ScanCart scanCart);

    /** 软删除 */
    int logicDeleteById(Long id);

    /** 软删除当前桌台全部购物车 */
    int logicDeleteByOwnerAndTable(ScanCart query);
}
