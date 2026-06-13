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

    /** 支付:0(待支付) → 2(制作中) */
    int payOrder(Long orderId, String payType);

    /** 用户取消:0(待支付) → 5(已取消) */
    int cancelOrder(Long orderId);

    /** 商家叫号:2(制作中) → 3(待取餐) */
    int callOrder(Long orderId);

    /** 完成订单:3(待取餐) → 4(已完成) */
    int completeOrder(Long orderId);

    /** 商家取消:2(制作中) → 5(已取消) */
    int cancelPaidOrder(Long orderId);

    /** 用户催单 */
    int urgeOrder(Long orderId, Long userId);
}
