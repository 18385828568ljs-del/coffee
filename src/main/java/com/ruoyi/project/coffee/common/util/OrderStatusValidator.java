package com.ruoyi.project.coffee.common.util;

import com.ruoyi.common.exception.ServiceException;

/**
 * 订单状态校验工具类
 *
 * 统一处理订单状态流转的前置校验,确保状态机正确
 */
public class OrderStatusValidator
{
    // 订单状态常量
    public static final int STATUS_PENDING = 0;     // 待支付
    public static final int STATUS_PAID = 1;        // 已支付
    public static final int STATUS_SHIPPED = 2;     // 已发货(商城订单)
    public static final int STATUS_COMPLETED = 3;   // 已完成
    public static final int STATUS_CANCELLED = 4;   // 已取消

    /**
     * 校验订单是否可以取消
     *
     * @param status 订单状态
     * @throws ServiceException 当状态不允许取消时抛出
     */
    public static void validateForCancel(Integer status)
    {
        if (status == null)
        {
            throw new ServiceException("订单状态异常");
        }
        if (status != STATUS_PENDING)
        {
            if (status == STATUS_CANCELLED)
            {
                throw new ServiceException("订单已取消");
            }
            throw new ServiceException("只能取消待支付订单");
        }
    }

    /**
     * 校验订单是否可以支付
     *
     * @param status 订单状态
     * @throws ServiceException 当状态不允许支付时抛出
     */
    public static void validateForPay(Integer status)
    {
        if (status == null)
        {
            throw new ServiceException("订单状态异常");
        }
        if (status == STATUS_PAID || status == STATUS_SHIPPED || status == STATUS_COMPLETED)
        {
            throw new ServiceException("订单已支付");
        }
        if (status == STATUS_CANCELLED)
        {
            throw new ServiceException("订单已取消");
        }
        if (status != STATUS_PENDING)
        {
            throw new ServiceException("仅未支付订单可以支付");
        }
    }

    /**
     * 校验订单是否可以确认收货(仅商城订单)
     *
     * @param status 订单状态
     * @throws ServiceException 当状态不允许确认收货时抛出
     */
    public static void validateForConfirm(Integer status)
    {
        if (status == null || status != STATUS_SHIPPED)
        {
            throw new ServiceException("只能确认已发货订单");
        }
    }

    /**
     * 检查订单状态是否为指定值
     *
     * @param actual 实际状态
     * @param expected 期望状态
     * @param errorMessage 不匹配时的错误信息
     */
    public static void requireStatus(Integer actual, int expected, String errorMessage)
    {
        if (actual == null || actual != expected)
        {
            throw new ServiceException(errorMessage);
        }
    }
}
