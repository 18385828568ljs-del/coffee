package com.ruoyi.project.coffee.profile.domain;

import java.math.BigDecimal;

/** Merchant-facing recommended product explanation. */
public class RecommendedProduct
{
    private Long productId;
    private String productName;
    private Long categoryId;
    private BigDecimal price;
    private Double score;
    private String reason;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
