package com.ruoyi.project.coffee.product.domain;

import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 商品对象 t_product
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
public class TProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long productId;

    /** 分类ID */
    @Excel(name = "分类ID")
    private Long categoryId;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String productName;

    /** 产地 */
    @Excel(name = "产地")
    private String origin;

    /** 处理法 */
    @Excel(name = "处理法")
    private String processingMethod;

    /** 烘焙度 */
    @Excel(name = "烘焙度")
    private String roastLevel;

    /** 风味描述 */
    @Excel(name = "风味描述")
    private String flavorNotes;

    /** 详细描述 */
    @Excel(name = "详细描述")
    private String description;

    /** 价格 */
    @Excel(name = "价格")
    private BigDecimal price;

    /** 库存 */
    @Excel(name = "库存")
    private Long stock;

    /** 主图地址 */
    @Excel(name = "主图地址")
    private String imageUrl;

    /** 商品状态(0-下架,1-上架) */
    @Excel(name = "商品状态(0-下架,1-上架)")
    private Integer status;

    /** 原价（前端展示用） */
    private BigDecimal originalPrice;

    /** 活动价（前端展示用） */
    private BigDecimal salePrice;

    /** 主活动标签 */
    private String activityTag;

    /** 活动摘要 */
    private String activitySummary;

    /** 活动标签列表 */
    private List<String> activityTags;

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getProductId()
    {
        return productId;
    }
    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }
    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getProductName()
    {
        return productName;
    }
    public void setOrigin(String origin)
    {
        this.origin = origin;
    }

    public String getOrigin()
    {
        return origin;
    }
    public void setProcessingMethod(String processingMethod)
    {
        this.processingMethod = processingMethod;
    }

    public String getProcessingMethod()
    {
        return processingMethod;
    }
    public void setRoastLevel(String roastLevel)
    {
        this.roastLevel = roastLevel;
    }

    public String getRoastLevel()
    {
        return roastLevel;
    }
    public void setFlavorNotes(String flavorNotes)
    {
        this.flavorNotes = flavorNotes;
    }

    public String getFlavorNotes()
    {
        return flavorNotes;
    }
    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }
    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public BigDecimal getPrice()
    {
        return price;
    }
    public void setStock(Long stock)
    {
        this.stock = stock;
    }

    public Long getStock()
    {
        return stock;
    }
    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }
    public String getProductImg()
    {
        return imageUrl;
    }
    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getStatus()
    {
        return status;
    }
    public void setOriginalPrice(BigDecimal originalPrice)
    {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getOriginalPrice()
    {
        return originalPrice == null ? price : originalPrice;
    }
    public void setSalePrice(BigDecimal salePrice)
    {
        this.salePrice = salePrice;
    }

    public BigDecimal getSalePrice()
    {
        return salePrice;
    }
    public void setActivityTag(String activityTag)
    {
        this.activityTag = activityTag;
    }

    public String getActivityTag()
    {
        return activityTag;
    }
    public void setActivitySummary(String activitySummary)
    {
        this.activitySummary = activitySummary;
    }

    public String getActivitySummary()
    {
        return activitySummary;
    }
    public void setActivityTags(List<String> activityTags)
    {
        this.activityTags = activityTags;
    }

    public List<String> getActivityTags()
    {
        return activityTags;
    }
    public String getFlavorTags()
    {
        return flavorNotes;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("productId", getProductId())
            .append("categoryId", getCategoryId())
            .append("productName", getProductName())
            .append("origin", getOrigin())
            .append("processingMethod", getProcessingMethod())
            .append("roastLevel", getRoastLevel())
            .append("flavorNotes", getFlavorNotes())
            .append("description", getDescription())
            .append("price", getPrice())
            .append("stock", getStock())
            .append("imageUrl", getImageUrl())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
