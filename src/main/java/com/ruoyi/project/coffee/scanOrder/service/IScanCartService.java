package com.ruoyi.project.coffee.scanOrder.service;

import java.util.List;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;

/**
 * 扫码点单-购物车Service接口
 */
public interface IScanCartService
{
    ScanCart selectScanCartById(Long id);

    List<ScanCart> selectScanCartList(ScanCart query);

    /**
     * 加入购物车,如同用户/openid + 同门店 + 同桌号 + 同商品 + 同规格已存在,则数量累加。
     * @return 最终落库的购物车记录(含 id)
     */
    ScanCart addOrIncrease(ScanCart cart);

    /** 修改数量;quantity <= 0 时软删除。返回受影响行数(软删/更新都计入)。 */
    int updateQuantity(Long id, Integer quantity);

    int logicDeleteById(Long id);

    int logicDeleteByOwnerAndTable(ScanCart query);
}
