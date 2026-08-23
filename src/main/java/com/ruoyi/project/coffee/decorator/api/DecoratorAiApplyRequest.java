package com.ruoyi.project.coffee.decorator.api;

public class DecoratorAiApplyRequest
{
    private Long themeId;
    private Integer revision;
    private Long productId;
    public Long getThemeId() { return themeId; }
    public void setThemeId(Long themeId) { this.themeId = themeId; }
    public Integer getRevision() { return revision; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
}
