package com.ruoyi.project.coffee.wallet.domain;

import java.math.BigDecimal;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 充值模板对象 t_recharge_template
 */
public class TRechargeTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long templateId;
    private BigDecimal payAmount;
    private BigDecimal giftAmount;
    private BigDecimal totalAmount;
    private Integer sortOrder;
    private Integer status;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public BigDecimal getGiftAmount() { return giftAmount; }
    public void setGiftAmount(BigDecimal giftAmount) { this.giftAmount = giftAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
