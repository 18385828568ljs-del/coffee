package com.ruoyi.project.coffee.profile.domain;

import java.math.BigDecimal;
import java.util.Date;

/** 画像计算使用的已过滤证据行。 */
public class ProfileEvidence
{
    private String scene;
    private String evidenceType;
    private Long productId;
    private String productName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private Date evidenceTime;

    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Date getEvidenceTime() { return evidenceTime; }
    public void setEvidenceTime(Date evidenceTime) { this.evidenceTime = evidenceTime; }
}
