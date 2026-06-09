package com.ruoyi.project.coffee.item.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 订单明细对象 t_order_item
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
public class TOrderItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private static final String GIFT_SPEC_PREFIX = "赠品";

    /** 明细ID */
    private Long itemId;

    /** 订单ID */
    @Excel(name = "订单ID")
    private Long orderId;

    /** 商品ID */
    @Excel(name = "商品ID")
    private Long productId;

    /** 商品名称(快照) */
    @Excel(name = "商品名称(快照)")
    private String productName;

    /** 商品图片(快照) */
    @Excel(name = "商品图片(快照)")
    private String productImage;

    /** 商品单价(快照) */
    @Excel(name = "商品单价(快照)")
    private BigDecimal price;

    /** 商品规格(快照) */
    private String spec;

    /** 购买数量 */
    @Excel(name = "购买数量")
    private Long quantity;

    /** 小计金额 */
    @Excel(name = "小计金额")
    private BigDecimal totalPrice;

    /** 是否赠品（接口计算字段） */
    private Boolean giftItem;

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public Long getItemId()
    {
        return itemId;
    }
    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public Long getOrderId()
    {
        return orderId;
    }
    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getProductId()
    {
        return productId;
    }
    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getProductName()
    {
        return productName;
    }
    public void setProductImage(String productImage)
    {
        this.productImage = productImage;
    }

    public String getProductImage()
    {
        return productImage;
    }
    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public BigDecimal getPrice()
    {
        return price;
    }
    public void setSpec(String spec)
    {
        this.spec = spec;
    }

    public String getSpec()
    {
        return spec;
    }
    public void setQuantity(Long quantity)
    {
        this.quantity = quantity;
    }

    public Long getQuantity()
    {
        return quantity;
    }
    public void setTotalPrice(BigDecimal totalPrice)
    {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getTotalPrice()
    {
        return totalPrice;
    }
    public void setGiftItem(Boolean giftItem)
    {
        this.giftItem = giftItem;
    }

    public Boolean getGiftItem()
    {
        if (giftItem != null)
        {
            return giftItem;
        }
        return spec != null && spec.startsWith(GIFT_SPEC_PREFIX);
    }
    public String getProductImg()
    {
        return productImage;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("orderId", getOrderId())
            .append("productId", getProductId())
            .append("productName", getProductName())
            .append("productImage", getProductImage())
            .append("price", getPrice())
            .append("spec", getSpec())
            .append("quantity", getQuantity())
            .append("totalPrice", getTotalPrice())
            .toString();
    }
}
