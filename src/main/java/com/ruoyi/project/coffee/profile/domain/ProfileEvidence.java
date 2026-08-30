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
    private String productType;
    private String origin;
    private String processingMethod;
    private String roastLevel;
    private String flavorNotes;
    private String specJson;
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
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getProcessingMethod() { return processingMethod; }
    public void setProcessingMethod(String processingMethod) { this.processingMethod = processingMethod; }
    public String getRoastLevel() { return roastLevel; }
    public void setRoastLevel(String roastLevel) { this.roastLevel = roastLevel; }
    public String getFlavorNotes() { return flavorNotes; }
    public void setFlavorNotes(String flavorNotes) { this.flavorNotes = flavorNotes; }
    public String getSpecJson() { return specJson; }
    public void setSpecJson(String specJson) { this.specJson = specJson; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Date getEvidenceTime() { return evidenceTime; }
    public void setEvidenceTime(Date evidenceTime) { this.evidenceTime = evidenceTime; }
}
