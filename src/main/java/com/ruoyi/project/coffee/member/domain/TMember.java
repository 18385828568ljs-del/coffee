package com.ruoyi.project.coffee.member.domain;

import java.math.BigDecimal;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 会员信息对象 t_member
 */
public class TMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long memberId;
    private Long userId;
    private Integer level;
    private String levelName;
    private BigDecimal discountRate;
    private BigDecimal totalSpending;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }
    public BigDecimal getTotalSpending() { return totalSpending; }
    public void setTotalSpending(BigDecimal totalSpending) { this.totalSpending = totalSpending; }
}
