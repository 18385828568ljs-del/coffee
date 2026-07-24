package com.ruoyi.project.coffee.scanOrder.domain;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 扫码点单-规格选项对象 t_scan_product_spec_option
 */
public class ScanProductSpecOption
{
    /** 选项ID */
    private Long optionId;

    /** 规格组ID */
    private Long specId;

    /** 商品ID(冗余) */
    private Long productId;

    /** 选项名称 */
    private String optionName;

    /** 加价金额 */
    private BigDecimal extraPrice;

    /** 是否默认(0-否,1-是) */
    private Integer isDefault;

    /** 排序 */
    private Integer sortOrder;

    /** 创建时间 */
    private Date createTime;

    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }

    public Long getSpecId() { return specId; }
    public void setSpecId(Long specId) { this.specId = specId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getOptionName() { return optionName; }
    public void setOptionName(String optionName) { this.optionName = optionName; }

    public BigDecimal getExtraPrice() { return extraPrice; }
    public void setExtraPrice(BigDecimal extraPrice) { this.extraPrice = extraPrice; }

    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("optionId", optionId)
            .append("specId", specId)
            .append("optionName", optionName)
            .append("extraPrice", extraPrice)
            .toString();
    }
}
