package com.ruoyi.project.coffee.scanOrder.domain;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 扫码点单-购物车对象 t_scan_cart
 */
public class ScanCart
{
    /** 购物车ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 微信openid */
    private String openid;

    /** 门店ID */
    private Long shopId;

    /** 桌号 */
    private String tableNo;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品图片 */
    private String productImage;

    /** 单价(已含规格加价) */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 规格描述文本 */
    private String specText;

    /** 规格JSON */
    private String specJson;

    /** 是否勾选结算(0-未勾选,1-已勾选) */
    private Integer selected;

    /** 状态(0-失效,1-有效) */
    private Integer status;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 删除标志(0-正常,1-已删除) */
    private Integer delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public String getTableNo() { return tableNo; }
    public void setTableNo(String tableNo) { this.tableNo = tableNo; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getSpecText() { return specText; }
    public void setSpecText(String specText) { this.specText = specText; }

    public String getSpecJson() { return specJson; }
    public void setSpecJson(String specJson) { this.specJson = specJson; }

    public Integer getSelected() { return selected; }
    public void setSelected(Integer selected) { this.selected = selected; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public Integer getDelFlag() { return delFlag; }
    public void setDelFlag(Integer delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", id)
            .append("userId", userId)
            .append("openid", openid)
            .append("shopId", shopId)
            .append("tableNo", tableNo)
            .append("productId", productId)
            .append("quantity", quantity)
            .append("price", price)
            .append("specText", specText)
            .append("status", status)
            .append("delFlag", delFlag)
            .toString();
    }
}
