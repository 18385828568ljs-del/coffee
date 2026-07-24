package com.ruoyi.project.coffee.payment.mapper;

import java.util.List;
import com.ruoyi.project.coffee.payment.domain.PaymentLog;

/**
 * 统一支付日志Mapper接口
 */
public interface PaymentLogMapper
{
    List<PaymentLog> selectPaymentLogList(PaymentLog paymentLog);

    int insertPaymentLog(PaymentLog paymentLog);

}
