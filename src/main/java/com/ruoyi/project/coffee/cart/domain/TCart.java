package com.ruoyi.project.coffee.cart.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 购物车对象 t_cart
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
public class TCart extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 购物车ID */
    private Long cartId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 商品ID */
    @Excel(name = "商品ID")
    private Long productId;

    /** 购买数量 */
    @Excel(name = "购买数量")
    private Long quantity;

    /** 规格 */
    private String spec;

    /** 商品名称 */
    private String productName;

    /** 商品图片 */
    private String productImage;

    /** 商品价格 */
    private BigDecimal price;

    public void setCartId(Long cartId)
    {
        this.cartId = cartId;
    }

    public Long getCartId()
    {
        return cartId;
    }
    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }
    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getProductId()
    {
        return productId;
    }
    public void setQuantity(Long quantity)
    {
        this.quantity = quantity;
    }

    public Long getQuantity()
    {
        return quantity;
    }
    public void setSpec(String spec)
    {
        this.spec = spec;
    }

    public String getSpec()
    {
        return spec;
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
    public String getProductImg()
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("cartId", getCartId())
            .append("userId", getUserId())
            .append("productId", getProductId())
            .append("quantity", getQuantity())
            .append("spec", getSpec())
            .append("productName", getProductName())
            .append("productImage", getProductImage())
            .append("price", getPrice())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
