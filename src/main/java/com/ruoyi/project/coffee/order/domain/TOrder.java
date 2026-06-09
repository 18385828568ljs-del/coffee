package com.ruoyi.project.coffee.order.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;
import com.ruoyi.project.coffee.item.domain.TOrderItem;

/**
 * 订单对象 t_order
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
public class TOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderNo;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 订单总金额 */
    @Excel(name = "订单总金额")
    private BigDecimal totalAmount;

    /** 实付金额 */
    @Excel(name = "实付金额")
    private BigDecimal payAmount;

    /** 收货人姓名 */
    @Excel(name = "收货人姓名")
    private String receiverName;

    /** 收货人电话 */
    @Excel(name = "收货人电话")
    private String receiverPhone;

    /** 收货地址 */
    @Excel(name = "收货地址")
    private String receiverAddress;

    /** 订单状态(0-待支付,1-待发货,2-已发货,3-已完成,4-已取消) */
    @Excel(name = "订单状态(0-待支付,1-待发货,2-已发货,3-已完成,4-已取消)")
    private Integer status;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "支付时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date payTime;

    /** 发货时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "发货时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date shipTime;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "完成时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date finishTime;

    /** 取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "取消时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date cancelTime;

    /** 物流单号 */
    @Excel(name = "物流单号")
    private String expressNo;

    /** 订单明细列表 */
    private List<TOrderItem> orderItems;

    /** 本次下单对应的购物车ID */
    private List<Long> cartIds;

    /** 优惠金额（接口返回用） */
    @Excel(name = "优惠金额")
    private BigDecimal discountAmount;

    /** 运费金额（接口返回用） */
    @Excel(name = "运费金额")
    private BigDecimal freightAmount;

    /** 活动摘要（接口返回用） */
    @Excel(name = "活动摘要")
    private String activitySummary;

    /** 支付方式（wechat, balance等） */
    private String payType;

    /** 会员折扣金额（接口返回用） */
    @Excel(name = "会员折扣金额")
    private BigDecimal memberDiscount;

    /** 商品摘要（后台展示与导出用） */
    @Excel(name = "商品信息")
    private String productSummary;

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public Long getOrderId()
    {
        return orderId;
    }
    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getOrderNo()
    {
        return orderNo;
    }
    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }
    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }
    public void setPayAmount(BigDecimal payAmount)
    {
        this.payAmount = payAmount;
    }

    public BigDecimal getPayAmount()
    {
        return payAmount;
    }
    public void setReceiverName(String receiverName)
    {
        this.receiverName = receiverName;
    }

    public String getReceiverName()
    {
        return receiverName;
    }
    public void setReceiverPhone(String receiverPhone)
    {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverPhone()
    {
        return receiverPhone;
    }
    public void setReceiverAddress(String receiverAddress)
    {
        this.receiverAddress = receiverAddress;
    }

    public String getReceiverAddress()
    {
        return receiverAddress;
    }
    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getStatus()
    {
        return status;
    }
    public void setPayTime(Date payTime)
    {
        this.payTime = payTime;
    }

    public Date getPayTime()
    {
        return payTime;
    }
    public void setShipTime(Date shipTime)
    {
        this.shipTime = shipTime;
    }

    public Date getShipTime()
    {
        return shipTime;
    }
    public void setFinishTime(Date finishTime)
    {
        this.finishTime = finishTime;
    }

    public Date getFinishTime()
    {
        return finishTime;
    }
    public void setCancelTime(Date cancelTime)
    {
        this.cancelTime = cancelTime;
    }

    public Date getCancelTime()
    {
        return cancelTime;
    }
    public void setExpressNo(String expressNo)
    {
        this.expressNo = expressNo;
    }

    public String getExpressNo()
    {
        return expressNo;
    }

    public void setOrderItems(List<TOrderItem> orderItems)
    {
        this.orderItems = orderItems;
    }

    public List<TOrderItem> getOrderItems()
    {
        return orderItems;
    }
    public void setCartIds(List<Long> cartIds)
    {
        this.cartIds = cartIds;
    }

    public List<Long> getCartIds()
    {
        return cartIds;
    }
    public void setPayType(String payType)
    {
        this.payType = payType;
    }

    public String getPayType()
    {
        return payType;
    }

    public void setMemberDiscount(BigDecimal memberDiscount)
    {
        this.memberDiscount = memberDiscount;
    }

    public BigDecimal getMemberDiscount()
    {
        return memberDiscount;
    }

    public void setDiscountAmount(BigDecimal discountAmount)
    {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getDiscountAmount()
    {
        if (discountAmount != null)
        {
            return discountAmount;
        }
        if (totalAmount == null || payAmount == null)
        {
            return BigDecimal.ZERO;
        }
        BigDecimal freight = freightAmount == null ? BigDecimal.ZERO : freightAmount;
        return totalAmount.add(freight).subtract(payAmount).max(BigDecimal.ZERO);
    }
    public void setFreightAmount(BigDecimal freightAmount)
    {
        this.freightAmount = freightAmount;
    }

    public BigDecimal getFreightAmount()
    {
        return freightAmount;
    }
    public void setActivitySummary(String activitySummary)
    {
        this.activitySummary = activitySummary;
    }

    public String getActivitySummary()
    {
        return activitySummary != null ? activitySummary : getRemark();
    }
    public void setProductSummary(String productSummary)
    {
        this.productSummary = productSummary;
    }

    public String getProductSummary()
    {
        return productSummary;
    }
    public void setItems(List<TOrderItem> orderItems)
    {
        this.orderItems = orderItems;
    }

    public List<TOrderItem> getItems()
    {
        return orderItems;
    }
    public void setDeliveryTime(Date deliveryTime)
    {
        this.shipTime = deliveryTime;
    }

    public Date getDeliveryTime()
    {
        return shipTime;
    }
    public BigDecimal getProductAmount()
    {
        return totalAmount;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("orderId", getOrderId())
            .append("orderNo", getOrderNo())
            .append("userId", getUserId())
            .append("totalAmount", getTotalAmount())
            .append("payAmount", getPayAmount())
            .append("receiverName", getReceiverName())
            .append("receiverPhone", getReceiverPhone())
            .append("receiverAddress", getReceiverAddress())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("payTime", getPayTime())
            .append("shipTime", getShipTime())
            .append("finishTime", getFinishTime())
            .append("cancelTime", getCancelTime())
            .append("expressNo", getExpressNo())
            .append("discountAmount", getDiscountAmount())
            .append("freightAmount", getFreightAmount())
            .append("activitySummary", getActivitySummary())
            .append("productSummary", getProductSummary())
            .append("remark", getRemark())
            .toString();
    }
}
