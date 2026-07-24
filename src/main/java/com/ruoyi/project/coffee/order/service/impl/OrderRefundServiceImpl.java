package com.ruoyi.project.coffee.order.service.impl;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.member.service.MemberService;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.order.mapper.TOrderMapper;
import com.ruoyi.project.coffee.order.service.IOrderRefundService;
import com.ruoyi.project.coffee.payment.service.PaymentLogService;
import com.ruoyi.project.coffee.wallet.service.WalletService;

/**
 * 商城订单退款服务实现。
 */
@Service
public class OrderRefundServiceImpl implements IOrderRefundService
{
    @Autowired
    private TOrderMapper orderMapper;

    @Autowired
    private WalletService walletService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private PaymentLogService paymentLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyRefund(Long orderId, Long userId, String refundReason)
    {
        TOrder order = getOrder(orderId);
        if (!userId.equals(order.getUserId()))
        {
            throw new ServiceException("无权操作此订单");
        }
        if (!isPaidStatus(order.getStatus()))
        {
            throw new ServiceException("订单状态不支持退款");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() == 1)
        {
            throw new ServiceException("订单已在退款流程中");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() == 2)
        {
            throw new ServiceException("订单已退款");
        }

        order.setRefundStatus(1);
        order.setRefundReason(refundReason);
        order.setRefundRejectReason("");
        order.setRefundApplyTime(DateUtils.getNowDate());
        return orderMapper.updateTOrder(order) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reviewRefund(Long orderId, boolean approved, String rejectReason)
    {
        TOrder order = getOrder(orderId);
        if (order.getRefundStatus() == null || order.getRefundStatus() != 1)
        {
            throw new ServiceException("订单不在退款审核状态");
        }

        if (approved)
        {
            return executeRefund(orderId);
        }

        order.setRefundStatus(3);
        order.setRefundRejectReason(StringUtils.isEmpty(rejectReason) ? "商家拒绝退款" : rejectReason);
        return orderMapper.updateTOrder(order) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeRefund(Long orderId)
    {
        TOrder order = getOrder(orderId);
        if (order.getRefundStatus() != null && order.getRefundStatus() == 2)
        {
            return true;
        }
        if (!isPaidStatus(order.getStatus()))
        {
            throw new ServiceException("订单状态不支持退款");
        }

        BigDecimal refundAmount = order.getPayAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("退款金额异常");
        }

        String payType = StringUtils.isEmpty(order.getPayType()) ? "balance" : order.getPayType();
        try
        {
            if (!"balance".equals(payType))
            {
                throw new ServiceException("不支持的支付方式: " + payType);
            }

            walletService.refund(order.getUserId(), refundAmount, "MALL_ORDER_REFUND",
                order.getOrderNo(), "商城订单退款");
            memberService.subtractSpending(order.getUserId(), refundAmount);
            paymentLogService.recordRefund("MALL_ORDER", order.getOrderNo(), order.getUserId(),
                "balance", "balance", refundAmount, "SUCCESS");

            order.setRefundStatus(2);
            order.setRefundTime(DateUtils.getNowDate());
            order.setRefundAmount(refundAmount);
            return orderMapper.updateTOrder(order) > 0;
        }
        catch (Exception e)
        {
            order.setRefundStatus(3);
            order.setRefundRejectReason(e.getMessage());
            orderMapper.updateTOrder(order);

            paymentLogService.recordRefund("MALL_ORDER", order.getOrderNo(), order.getUserId(),
                payType, payType, refundAmount, "FAILED");

            throw new ServiceException("退款失败: " + e.getMessage());
        }
    }

    private TOrder getOrder(Long orderId)
    {
        TOrder order = orderMapper.selectTOrderByOrderId(orderId);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }
        return order;
    }

    private boolean isPaidStatus(Integer status)
    {
        return status != null && status >= 1 && status <= 3;
    }
}
