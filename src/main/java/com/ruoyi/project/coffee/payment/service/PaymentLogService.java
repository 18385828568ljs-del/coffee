package com.ruoyi.project.coffee.payment.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.project.coffee.payment.domain.PaymentLog;
import com.ruoyi.project.coffee.payment.mapper.PaymentLogMapper;

/**
 * 统一支付日志Service
 */
@Service
public class PaymentLogService
{
    private static final String STATUS_PAID = "PAID";

    @Autowired
    private PaymentLogMapper paymentLogMapper;

    public List<PaymentLog> selectPaymentLogList(PaymentLog paymentLog)
    {
        return paymentLogMapper.selectPaymentLogList(paymentLog);
    }

    public void recordBalancePaid(String businessType, String businessNo, Long userId, BigDecimal amount)
    {
        PaymentLog log = baseLog(businessType, businessNo, userId, "balance", "balance", amount);
        log.setStatus(STATUS_PAID);
        paymentLogMapper.insertPaymentLog(log);
    }

    /**
     * 记录退款日志
     */
    public void recordRefund(String businessType, String businessNo, Long userId, String payType,
                            String payChannel, BigDecimal amount, String status)
    {
        PaymentLog log = baseLog(businessType, businessNo, userId, payType, payChannel, amount);
        log.setStatus("REFUND_" + status);  // REFUND_SUCCESS 或 REFUND_FAILED
        paymentLogMapper.insertPaymentLog(log);
    }

    private PaymentLog baseLog(String businessType, String businessNo, Long userId, String payType,
                               String payChannel, BigDecimal amount)
    {
        PaymentLog log = new PaymentLog();
        Date now = DateUtils.getNowDate();
        log.setBusinessType(businessType);
        log.setBusinessNo(businessNo);
        log.setUserId(userId);
        log.setPayType(payType);
        log.setPayChannel(payChannel);
        log.setAmount(amount);
        log.setCreateTime(now);
        log.setUpdateTime(now);
        return log;
    }

}
