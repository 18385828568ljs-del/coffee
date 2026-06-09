package com.ruoyi.project.coffee.wallet.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
     * 创建充值订单（待支付）
     */
    @Transactional
    public TRechargeRecord createRecharge(Long userId, Long templateId, BigDecimal payAmount)
    {
        // 验证充值模板
        TRechargeTemplate template = null;
        if (templateId != null)
        {
            template = walletMapper.selectTemplateById(templateId);
        }

        BigDecimal giftAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = payAmount;
        if (template != null)
        {
            // 使用模板的金额（以模板为准，防止前端篡改）
            payAmount = template.getPayAmount();
            giftAmount = template.getGiftAmount();
            totalAmount = template.getTotalAmount();
        }

        // 确保钱包存在
        getOrCreateWallet(userId);

        // 生成充值单号
        String rechargeNo = "RC" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        TRechargeRecord record = new TRechargeRecord();
        record.setUserId(userId);
        record.setRechargeNo(rechargeNo);
        record.setPayAmount(payAmount);
        record.setGiftAmount(giftAmount);
        record.setTotalAmount(totalAmount);
        record.setTemplateId(templateId);
        record.setPayType("wechat");
        record.setStatus(0); // 待支付
        walletMapper.insertRechargeRecord(record);

        return record;
    }

    /**
     * 确认充值入账（模拟支付回调）
     * 真实场景下此方法由微信支付回调触发,通过签名校验确保来源可信。
     * 模拟环境下由前端调用,需在调用前校验单号归属(由调用方传入 userId)。
     *
     * @param rechargeNo 充值单号
     * @param userId     调用方用户 id; 不为 null 时校验单号是否归属此用户; null 表示由可信链路调用(如真实支付回调)
     */
    @Transactional
    public void confirmRecharge(String rechargeNo, Long userId)
    {
        TRechargeRecord record = walletMapper.selectRechargeByNo(rechargeNo);
        if (record == null)
        {
            throw new ServiceException("充值记录不存在");
        }
        if (userId != null && !userId.equals(record.getUserId()))
        {
            // 不暴露记录存在性,统一返回相同文案
            throw new ServiceException("充值记录不存在");
        }
        if (record.getStatus() != 0)
        {
            // 已处理过，幂等返回
            return;
        }

        // 乐观锁: 仅当 DB 当前 status=0 时才置为 1, 防止并发回调多次入账
        Date payTime = new Date();
        int marked = walletMapper.markRechargePaid(record.getRecordId(), payTime);
        if (marked == 0)
        {
            // 其他线程已抢先入账, 当前调用幂等返回
            return;
        }
        record.setStatus(1);
        record.setPayTime(payTime);

        // 获取当前钱包余额（入账前）
        TWallet wallet = getOrCreateWallet(record.getUserId());
        BigDecimal balanceBefore = wallet.getBalance();

        // 入账：实付金额 + 赠送金额
        BigDecimal totalCredit = record.getTotalAmount();
        walletMapper.addBalance(record.getUserId(), totalCredit);

        // 更新累计充值和赠送统计
        // 注意：此处只更新 totalRecharge 和 totalGift，将 balance/totalConsumed/frozenAmount 设为 null
        // 避免 updateWallet 的条件更新覆盖掉 addBalance 原子操作后的真实余额
        wallet.setBalance(null);
        wallet.setTotalConsumed(null);
        wallet.setFrozenAmount(null);
        wallet.setTotalRecharge(wallet.getTotalRecharge().add(record.getPayAmount()));
        wallet.setTotalGift(wallet.getTotalGift().add(record.getGiftAmount()));
        walletMapper.updateWallet(wallet);

        // 记录流水
        boolean hasGift = record.getGiftAmount().compareTo(BigDecimal.ZERO) > 0;

        if (!hasGift)
        {
            // 没有赠送，只记一条充值入账流水
            TWalletLog log = new TWalletLog();
            log.setUserId(record.getUserId());
            log.setType(1); // 充值入账
            log.setAmount(totalCredit);
            log.setBalanceBefore(balanceBefore);
            log.setBalanceAfter(balanceBefore.add(totalCredit));
            log.setRelatedOrderNo(rechargeNo);
            log.setRemark("充值 ¥" + record.getPayAmount().toPlainString());
            walletMapper.insertWalletLog(log);
        }
        else
        {
            // 有赠送时拆两条流水：充值实付 + 赠送到账，合计 = totalAmount
            BigDecimal afterPay = balanceBefore.add(record.getPayAmount());

            // 第一条：充值实付金额入账
            TWalletLog payLog = new TWalletLog();
            payLog.setUserId(record.getUserId());
            payLog.setType(1); // 充值入账
            payLog.setAmount(record.getPayAmount());
            payLog.setBalanceBefore(balanceBefore);
            payLog.setBalanceAfter(afterPay);
            payLog.setRelatedOrderNo(rechargeNo);
            payLog.setRemark("充值 ¥" + record.getPayAmount().toPlainString());
            walletMapper.insertWalletLog(payLog);

            // 第二条：赠送金额到账
            TWalletLog giftLog = new TWalletLog();
            giftLog.setUserId(record.getUserId());
            giftLog.setType(4); // 赠送到账
            giftLog.setAmount(record.getGiftAmount());
            giftLog.setBalanceBefore(afterPay);
            giftLog.setBalanceAfter(afterPay.add(record.getGiftAmount()));
            giftLog.setRelatedOrderNo(rechargeNo);
            giftLog.setRemark("充值赠送 ¥" + record.getGiftAmount().toPlainString());
            walletMapper.insertWalletLog(giftLog);
        }
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
}
