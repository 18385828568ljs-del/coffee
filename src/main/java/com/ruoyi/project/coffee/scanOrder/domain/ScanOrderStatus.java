package com.ruoyi.project.coffee.scanOrder.domain;

/**
 * 扫码点单订单状态常量。
 */
public final class ScanOrderStatus
{
    private ScanOrderStatus()
    {
    }

    /** 待支付 */
    public static final int PENDING_PAY = 0;

    /** 状态 1 已不再使用,支付后直接进入制作中 */
    public static final int UNUSED = 1;

    /** 制作中 */
    public static final int MAKING = 2;

    /** 待取餐 */
    public static final int WAIT_PICKUP = 3;

    /** 已完成 */
    public static final int COMPLETED = 4;

    /** 已取消 */
    public static final int CANCELLED = 5;

    public static boolean isPaid(Integer status)
    {
        return status != null && status >= MAKING && status <= COMPLETED;
    }

    public static boolean isUnfinishedPaid(Integer status)
    {
        return status != null && status >= MAKING && status <= WAIT_PICKUP;
    }
}
