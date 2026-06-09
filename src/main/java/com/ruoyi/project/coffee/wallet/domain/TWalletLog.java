package com.ruoyi.project.coffee.wallet.domain;

import java.math.BigDecimal;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 余额流水对象 t_wallet_log
 *
 * 注：remark 字段复用父类 BaseEntity 的 remark，不再重复声明
 */
public class TWalletLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long logId;
    private Long userId;
    private Integer type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String relatedOrderNo;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getRelatedOrderNo() { return relatedOrderNo; }
    public void setRelatedOrderNo(String relatedOrderNo) { this.relatedOrderNo = relatedOrderNo; }
}
