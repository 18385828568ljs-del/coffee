package com.ruoyi.project.coffee.scanOrder.domain;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 扫码点单-商品规格组对象 t_scan_product_spec
 */
public class ScanProductSpec
{
    /** 规格ID */
    private Long specId;

    /** 商品ID */
    private Long productId;

    /** 规格名称 */
    private String specName;

    /** 规格类型(single-单选,multiple-多选) */
    private String specType;

    /** 是否必选(0-否,1-是) */
    private Integer required;

    /** 排序 */
    private Integer sortOrder;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 非数据库字段:规格选项列表 */
    private List<ScanProductSpecOption> options;

    public Long getSpecId() { return specId; }
    public void setSpecId(Long specId) { this.specId = specId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getSpecName() { return specName; }
    public void setSpecName(String specName) { this.specName = specName; }

    public String getSpecType() { return specType; }
    public void setSpecType(String specType) { this.specType = specType; }

    public Integer getRequired() { return required; }
    public void setRequired(Integer required) { this.required = required; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public List<ScanProductSpecOption> getOptions() { return options; }
    public void setOptions(List<ScanProductSpecOption> options) { this.options = options; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("specId", specId)
            .append("productId", productId)
            .append("specName", specName)
            .append("specType", specType)
            .toString();
    }
}
