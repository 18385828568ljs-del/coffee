package com.ruoyi.project.coffee.scanOrder.service;

import java.util.List;
import com.ruoyi.project.coffee.activity.domain.MarketingPreviewResult;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;

/**
 * 扫码点单-订单Service接口
 */
public interface IScanOrderService
{
    /**
     * 根据当前桌台购物车结算下单。
     */
    ScanOrder createOrderFromCart(Long userId, String openid, Long shopId,
                                  String tableNo, String remark, String payType);

    /**
     * 计算当前购物车应用营销活动 + 会员折扣后的金额拆分(不写库)
     */
    MarketingPreviewResult previewOrderFromCart(Long userId, String openid, Long shopId, String tableNo);

    ScanOrder selectScanOrderById(Long orderId);

    ScanOrder selectScanOrderWithItems(Long orderId);

    List<ScanOrder> selectScanOrderList(ScanOrder scanOrder);

    List<ScanOrder> selectMyOrderList(Long userId);

    /** 模拟支付:0(待支付) → 1(已支付/制作中) */
    int payOrder(Long orderId, String payType);

    /** 取消:0(待支付) → 3(已取消) */
    int cancelOrder(Long orderId);
}
