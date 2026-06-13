package com.ruoyi.project.coffee.scanOrder.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 扫码点单-订单对象 t_scan_order
 *
 * 表内使用单一 status 字段标识订单生命周期:
 *   0-待支付, 1-保留, 2-制作中, 3-待取餐, 4-已完成, 5-已取消
 * 前端历史上惯用 payStatus + orderStatus 两段式,这里提供派生只读 getter,
 * 数据库仍只存 status 一列。
 */
public class ScanOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderNo;

    /** 下单用户ID */
    private Long userId;

    /** WeChat openid */
    private String openid;

    /** 门店ID */
    @Excel(name = "门店ID")
    private Long shopId;

    /** 门店名称 */
    private String shopName;

    /** 桌号 */
    @Excel(name = "桌号")
    private String tableNo;

    /** 场景(dine_in-堂食,take_out-外带) */
    private String scene;

    /** 订单总金额 */
    @Excel(name = "订单总金额")
    private BigDecimal totalAmount;

    /** 实付金额 */
    @Excel(name = "实付金额")
    private BigDecimal payAmount;

    /** 活动优惠金额(满减/打折/一口价命中后省下的钱) */
    @Excel(name = "活动优惠金额")
    private BigDecimal discountAmount;

    /** 会员折扣金额(基于会员等级折扣率额外减免) */
    @Excel(name = "会员折扣金额")
    private BigDecimal memberDiscount;

    /** 命中的活动文案,逗号分隔,供前端展示 */
    private String activitySummary;

    /** 状态(0-待支付,1-保留,2-制作中,3-待取餐,4-已完成,5-已取消) */
    @Excel(name = "状态")
    private Integer status;

    /** 取餐号 */
    private String pickupNo;

    /** 预计等待分钟数 */
    private Integer estimatedWaitMinutes;

    /** 自动开始时间 */
    private Date acceptTime;

    /** 开始制作时间 */
    private Date makingTime;

    /** 叫号时间 */
    private Date callTime;

    /** 催单次数 */
    private Integer urgeCount;

    /** 最后催单时间 */
    private Date lastUrgeTime;

    /** 支付方式 */
    private String payType;

    /** 支付时间 */
    private Date payTime;

    /** 完成时间 */
    private Date finishTime;

    /** 取消时间 */
    private Date cancelTime;

    /** 非数据库字段:订单明细 */
    private List<ScanOrderItem> items;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getTableNo() { return tableNo; }
    public void setTableNo(String tableNo) { this.tableNo = tableNo; }

    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getMemberDiscount() { return memberDiscount; }
    public void setMemberDiscount(BigDecimal memberDiscount) { this.memberDiscount = memberDiscount; }

    public String getActivitySummary() { return activitySummary; }
    public void setActivitySummary(String activitySummary) { this.activitySummary = activitySummary; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getPickupNo() { return pickupNo; }
    public void setPickupNo(String pickupNo) { this.pickupNo = pickupNo; }

    public Integer getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) { this.estimatedWaitMinutes = estimatedWaitMinutes; }

    public Date getAcceptTime() { return acceptTime; }
    public void setAcceptTime(Date acceptTime) { this.acceptTime = acceptTime; }

    public Date getMakingTime() { return makingTime; }
    public void setMakingTime(Date makingTime) { this.makingTime = makingTime; }

    public Date getCallTime() { return callTime; }
    public void setCallTime(Date callTime) { this.callTime = callTime; }

    public Integer getUrgeCount() { return urgeCount; }
    public void setUrgeCount(Integer urgeCount) { this.urgeCount = urgeCount; }

    public Date getLastUrgeTime() { return lastUrgeTime; }
    public void setLastUrgeTime(Date lastUrgeTime) { this.lastUrgeTime = lastUrgeTime; }

    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }

    public Date getPayTime() { return payTime; }
    public void setPayTime(Date payTime) { this.payTime = payTime; }

    public Date getFinishTime() { return finishTime; }
    public void setFinishTime(Date finishTime) { this.finishTime = finishTime; }

    public Date getCancelTime() { return cancelTime; }
    public void setCancelTime(Date cancelTime) { this.cancelTime = cancelTime; }

    public List<ScanOrderItem> getItems() { return items; }
    public void setItems(List<ScanOrderItem> items) { this.items = items; }

    /** 派生:支付状态(0-未支付,1-已支付) */
    public Integer getPayStatus()
    {
        if (status == null) return 0;
        return ScanOrderStatus.isPaid(status) ? 1 : 0;
    }

    /** 派生:订单流转状态,与 status 同值,提供给前端更语义化的字段名 */
    public Integer getOrderStatus()
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("orderId", orderId)
            .append("orderNo", orderNo)
            .append("userId", userId)
            .append("openid", openid)
            .append("shopId", shopId)
            .append("tableNo", tableNo)
            .append("totalAmount", totalAmount)
            .append("payAmount", payAmount)
            .append("status", status)
            .append("pickupNo", pickupNo)
            .append("payType", payType)
            .append("payTime", payTime)
            .append("createTime", getCreateTime())
            .toString();
    }
}
