package com.ruoyi.project.coffee.scanOrder.service;

/**
 * 退款服务接口
 */
public interface IScanOrderRefundService
{
    /**
     * 申请退款
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param refundReason 退款原因
     * @return 是否成功
     */
    boolean applyRefund(Long orderId, Long userId, String refundReason);

    /**
     * 管理员审核退款
     * @param orderId 订单ID
     * @param approved 是否通过
     * @param rejectReason 拒绝原因
     * @return 是否成功
     */
    boolean reviewRefund(Long orderId, boolean approved, String rejectReason);

    /**
     * 执行退款（自动）
     * @param orderId 订单ID
     * @return 是否成功
     */
    boolean executeRefund(Long orderId);
}
