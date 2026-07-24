package com.ruoyi.project.coffee.scanOrder.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.payment.service.PaymentLogService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderStatus;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanOrderMapper;
import com.ruoyi.project.coffee.scanOrder.service.IScanOrderRefundService;
import com.ruoyi.project.coffee.wallet.service.WalletService;
import com.ruoyi.project.coffee.member.service.MemberService;

/**
 * 扫码点单退款服务实现
 */
@Service
public class ScanOrderRefundServiceImpl implements IScanOrderRefundService
{
    @Autowired
    private ScanOrderMapper scanOrderMapper;

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
        ScanOrder order = scanOrderMapper.selectScanOrderById(orderId);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }

        if (!userId.equals(order.getUserId()))
        {
            throw new ServiceException("无权操作此订单");
        }

        // 只有已支付的订单才能退款
        if (!ScanOrderStatus.isPaid(order.getStatus()))
        {
            throw new ServiceException("订单状态不支持退款");
        }

        // 如果已经是退款中或已退款，不能重复申请；拒绝/失败后允许用户重新提交。
        if (order.getRefundStatus() != null && order.getRefundStatus() == 1)
        {
            throw new ServiceException("订单已在退款流程中");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() == 2)
        {
            throw new ServiceException("订单已退款");
        }

        // 更新退款状态为"退款中"
        order.setRefundStatus(1);  // 1-退款中
        order.setRefundReason(refundReason);
        order.setRefundRejectReason("");
        order.setRefundApplyTime(DateUtils.getNowDate());
        order.setUpdateTime(DateUtils.getNowDate());

        int affected = scanOrderMapper.updateScanOrder(order);
        return affected > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reviewRefund(Long orderId, boolean approved, String rejectReason)
    {
        ScanOrder order = scanOrderMapper.selectScanOrderById(orderId);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }

        if (order.getRefundStatus() == null || order.getRefundStatus() != 1)
        {
            throw new ServiceException("订单不在退款审核状态");
        }

        if (approved)
        {
            // 审核通过，执行退款
            return executeRefund(orderId);
        }
        else
        {
            // 审核拒绝
            order.setRefundStatus(3);  // 3-退款失败
            order.setRefundRejectReason(rejectReason);
            order.setUpdateTime(DateUtils.getNowDate());
            int affected = scanOrderMapper.updateScanOrder(order);
            return affected > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeRefund(Long orderId)
    {
        ScanOrder order = scanOrderMapper.selectScanOrderById(orderId);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }

        if (order.getRefundStatus() != null && order.getRefundStatus() == 2)
        {
            // 已退款，避免重复
            return true;
        }

        String payType = order.getPayType();
        BigDecimal refundAmount = order.getPayAmount();

        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("退款金额异常");
        }

        try
        {
            if (!"balance".equals(payType))
            {
                throw new ServiceException("不支持的支付方式: " + payType);
            }

            walletService.refund(order.getUserId(), refundAmount, "SCAN_ORDER_REFUND",
                order.getOrderNo(), "扫码点单退款");
            memberService.subtractSpending(order.getUserId(), refundAmount);
            paymentLogService.recordRefund("SCAN_ORDER", order.getOrderNo(), order.getUserId(),
                "balance", "balance", refundAmount, "SUCCESS");

            // 更新订单退款状态
            order.setRefundStatus(2);  // 2-已退款
            order.setRefundTime(DateUtils.getNowDate());
            order.setRefundAmount(refundAmount);
            order.setUpdateTime(DateUtils.getNowDate());

            int affected = scanOrderMapper.updateScanOrder(order);
            return affected > 0;
        }
        catch (Exception e)
        {
            // 退款失败
            order.setRefundStatus(3);  // 3-退款失败
            order.setRefundRejectReason(e.getMessage());
            order.setUpdateTime(DateUtils.getNowDate());
            scanOrderMapper.updateScanOrder(order);

            // 记录失败日志
            paymentLogService.recordRefund("SCAN_ORDER", order.getOrderNo(), order.getUserId(),
                payType, payType, refundAmount, "FAILED");

            throw new ServiceException("退款失败: " + e.getMessage());
        }
    }
}
