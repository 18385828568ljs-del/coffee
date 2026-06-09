package com.ruoyi.project.coffee.wallet.domain;

import java.math.BigDecimal;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 充值记录对象 t_recharge_record
 */
public class TRechargeRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long recordId;
    private Long userId;
    private String rechargeNo;
    private BigDecimal payAmount;
    private BigDecimal giftAmount;
    private BigDecimal totalAmount;
    private Long templateId;
    private String payType;
    private Integer status;
    private String transactionId;
    private java.util.Date payTime;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRechargeNo() { return rechargeNo; }
    public void setRechargeNo(String rechargeNo) { this.rechargeNo = rechargeNo; }
    public BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public BigDecimal getGiftAmount() { return giftAmount; }
    public void setGiftAmount(BigDecimal giftAmount) { this.giftAmount = giftAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public java.util.Date getPayTime() { return payTime; }
    public void setPayTime(java.util.Date payTime) { this.payTime = payTime; }
}
