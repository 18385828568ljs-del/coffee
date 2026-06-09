package com.ruoyi.project.coffee.wallet.domain;

import java.math.BigDecimal;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 钱包余额对象 t_wallet
 */
public class TWallet extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long walletId;
    private Long userId;
    private BigDecimal balance;
    private BigDecimal totalRecharge;
    private BigDecimal totalGift;
    private BigDecimal totalConsumed;
    private BigDecimal frozenAmount;

    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getTotalRecharge() { return totalRecharge; }
    public void setTotalRecharge(BigDecimal totalRecharge) { this.totalRecharge = totalRecharge; }
    public BigDecimal getTotalGift() { return totalGift; }
    public void setTotalGift(BigDecimal totalGift) { this.totalGift = totalGift; }
    public BigDecimal getTotalConsumed() { return totalConsumed; }
    public void setTotalConsumed(BigDecimal totalConsumed) { this.totalConsumed = totalConsumed; }
    public BigDecimal getFrozenAmount() { return frozenAmount; }
    public void setFrozenAmount(BigDecimal frozenAmount) { this.frozenAmount = frozenAmount; }
}
