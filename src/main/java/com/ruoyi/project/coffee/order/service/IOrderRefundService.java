package com.ruoyi.project.coffee.order.service;

/**
 * 商城订单退款服务接口。
 */
public interface IOrderRefundService
{
    boolean applyRefund(Long orderId, Long userId, String refundReason);

    boolean reviewRefund(Long orderId, boolean approved, String rejectReason);

    boolean executeRefund(Long orderId);
}
