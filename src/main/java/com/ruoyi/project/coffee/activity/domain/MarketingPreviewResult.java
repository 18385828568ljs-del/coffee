package com.ruoyi.project.coffee.activity.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.ruoyi.project.coffee.item.domain.TOrderItem;

/**
 * 营销活动预览结果
 */
public class MarketingPreviewResult
{
    /** 商品总价 */
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** 实付金额 */
    private BigDecimal payAmount = BigDecimal.ZERO;

    /** 优惠金额 */
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** 会员折扣金额 */
    private BigDecimal memberDiscount = BigDecimal.ZERO;

    /** 运费金额 */
    private BigDecimal freightAmount = BigDecimal.ZERO;

    /** 命中的活动标题 */
    private List<String> activityTitles = new ArrayList<String>();

    /** 活动摘要 */
    private String activitySummary;

    /** 赠品明细 */
    private List<TOrderItem> giftItems = new ArrayList<TOrderItem>();

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPayAmount()
    {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount)
    {
        this.payAmount = payAmount;
    }

    public BigDecimal getDiscountAmount()
    {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount)
    {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getMemberDiscount()
    {
        return memberDiscount;
    }

    public void setMemberDiscount(BigDecimal memberDiscount)
    {
        this.memberDiscount = memberDiscount;
    }

    public BigDecimal getFreightAmount()
    {
        return freightAmount;
    }

    public void setFreightAmount(BigDecimal freightAmount)
    {
        this.freightAmount = freightAmount;
    }

    public List<String> getActivityTitles()
    {
        return activityTitles;
    }

    public void setActivityTitles(List<String> activityTitles)
    {
        this.activityTitles = activityTitles;
    }

    public String getActivitySummary()
    {
        return activitySummary;
    }

    public void setActivitySummary(String activitySummary)
    {
        this.activitySummary = activitySummary;
    }

    public List<TOrderItem> getGiftItems()
    {
        return giftItems;
    }

    public void setGiftItems(List<TOrderItem> giftItems)
    {
        this.giftItems = giftItems;
    }
}
