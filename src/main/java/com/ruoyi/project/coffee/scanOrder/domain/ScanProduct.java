package com.ruoyi.project.coffee.scanOrder.domain;

import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 扫码点单-商品对象 t_scan_product
 */
public class ScanProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long productId;

    /** 所属分类ID */
    @Excel(name = "分类ID")
    private Long categoryId;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String productName;

    /** 商品副标题/描述 */
    private String subTitle;

    /** 商品主图 */
    private String imageUrl;

    /** 商品讲解视频地址 */
    private String videoUrl;

    /** 基础价格 */
    @Excel(name = "价格")
    private BigDecimal price;

    /** 月销量(展示用) */
    private Integer monthSales;

    /** 标签(新品/招牌/热销等) */
    private String tag;

    /** 商品状态(0-下架,1-上架) */
    @Excel(name = "状态")
    private Integer status;

    /** 排序 */
    private Integer sortOrder;

    /** 非数据库字段:商品规格组(含选项),查询详情时填充 */
    private List<ScanProductSpec> specs;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSubTitle() { return subTitle; }
    public void setSubTitle(String subTitle) { this.subTitle = subTitle; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getMonthSales() { return monthSales; }
    public void setMonthSales(Integer monthSales) { this.monthSales = monthSales; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public List<ScanProductSpec> getSpecs() { return specs; }
    public void setSpecs(List<ScanProductSpec> specs) { this.specs = specs; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("productId", getProductId())
            .append("categoryId", getCategoryId())
            .append("productName", getProductName())
            .append("price", getPrice())
            .append("status", getStatus())
            .toString();
    }
}
