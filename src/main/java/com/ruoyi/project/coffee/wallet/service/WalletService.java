package com.ruoyi.project.coffee.wallet.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.wallet.domain.TWallet;
import com.ruoyi.project.coffee.wallet.domain.TWalletLog;
import com.ruoyi.project.coffee.wallet.domain.TRechargeRecord;
import com.ruoyi.project.coffee.wallet.domain.TRechargeTemplate;
import com.ruoyi.project.coffee.wallet.mapper.TWalletMapper;

/**
 * 钱包充值服务
 */
@Service
public class WalletService
{
    @Autowired
    private TWalletMapper walletMapper;

    /**
     * 获取或初始化钱包
     */
    public TWallet getOrCreateWallet(Long userId)
    {
        TWallet wallet = walletMapper.selectWalletByUserId(userId);
        if (wallet == null)
        {
            wallet = new TWallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setTotalRecharge(BigDecimal.ZERO);
            wallet.setTotalGift(BigDecimal.ZERO);
            wallet.setTotalConsumed(BigDecimal.ZERO);
            wallet.setFrozenAmount(BigDecimal.ZERO);
            walletMapper.insertWallet(wallet);
        }
        return wallet;
    }

    /**
     * 获取余额流水
     */
    public List<TWalletLog> getWalletLogs(Long userId)
    {
        return walletMapper.selectWalletLogList(userId);
    }

    /**
     * 获取充值模板列表
     */
    public List<TRechargeTemplate> getActiveTemplates()
    {
        return walletMapper.selectActiveTemplates();
    }

    /**
     * 余额支付扣款
     */
    @Transactional
    public void deductForOrder(Long userId, BigDecimal amount, String orderNo)
    {
        TWallet wallet = getOrCreateWallet(userId);
        BigDecimal balanceBefore = wallet.getBalance();

        if (balanceBefore.compareTo(amount) < 0)
        {
            throw new ServiceException("余额不足");
        }

        int affected = walletMapper.deductBalance(userId, amount);
        if (affected == 0)
        {
            throw new ServiceException("余额不足（并发扣款失败）");
        }

        // 记录流水
        TWalletLog log = new TWalletLog();
        log.setUserId(userId);
        log.setType(2); // 消费扣款
        log.setAmount(amount.negate()); // 负数
        log.setBalanceBefore(balanceBefore);
        log.setBalanceAfter(balanceBefore.subtract(amount));
        log.setRelatedOrderNo(orderNo);
        log.setRemark("订单支付");
        walletMapper.insertWalletLog(log);
    }

    /**
     * 获取充值记录列表
     */
    public List<TRechargeRecord> getRechargeRecords(Long userId)
    {
        return walletMapper.selectRechargeRecordList(userId);
    }

    /**
     * 消费（扣除余额）
     */
    @Transactional
    public void consume(Long userId, BigDecimal amount, String businessType, String businessNo, String remark)
    {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("消费金额必须大于0");
        }

        TWallet wallet = getOrCreateWallet(userId);
        BigDecimal balanceBefore = wallet.getBalance();

        // 乐观锁扣款
        int affected = walletMapper.consumeBalance(userId, amount);
        if (affected <= 0)
        {
            throw new ServiceException("余额不足");
        }

        // 记录流水
        TWalletLog log = new TWalletLog();
        log.setUserId(userId);
        log.setType(2); // 消费扣款
        log.setAmount(amount.negate());  // 负数表示消费
        log.setBalanceBefore(balanceBefore);
        log.setBalanceAfter(balanceBefore.subtract(amount));
        log.setRelatedOrderNo(businessNo);
        log.setRemark(remark);
        walletMapper.insertWalletLog(log);
    }

    /**
     * 退款（退回余额）
     */
    @Transactional
    public void refund(Long userId, BigDecimal amount, String businessType, String businessNo, String remark)
    {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("退款金额必须大于0");
        }

        TWallet wallet = getOrCreateWallet(userId);
        BigDecimal balanceBefore = wallet.getBalance();

        // 增加余额
        int affected = walletMapper.increaseBalance(userId, amount);
        if (affected <= 0)
        {
            throw new ServiceException("退款失败");
        }

        // 记录流水
        TWalletLog log = new TWalletLog();
        log.setUserId(userId);
        log.setType(3); // 退款到账
        log.setAmount(amount);  // 正数表示退款
        log.setBalanceBefore(balanceBefore);
        log.setBalanceAfter(balanceBefore.add(amount));
        log.setRelatedOrderNo(businessNo);
        log.setRemark(remark);
        walletMapper.insertWalletLog(log);
    }
}
