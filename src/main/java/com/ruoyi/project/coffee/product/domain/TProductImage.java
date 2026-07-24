package com.ruoyi.project.coffee.product.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 商品图片对象 t_product_image
 *
 * @author ruoyi
 */
public class TProductImage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 图片ID */
    private Long imageId;

    /** 商品ID */
    private Long productId;

    /** 图片地址 */
    private String imageUrl;

    /** 排序 */
    private Integer sortOrder;

    /** 是否主图 */
    private Integer isMain;

    public Long getImageId()
    {
        return imageId;
    }

    public void setImageId(Long imageId)
    {
        this.imageId = imageId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Integer getIsMain()
    {
        return isMain;
    }

    public void setIsMain(Integer isMain)
    {
        this.isMain = isMain;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("imageId", getImageId())
            .append("productId", getProductId())
            .append("imageUrl", getImageUrl())
            .append("sortOrder", getSortOrder())
            .append("isMain", getIsMain())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
