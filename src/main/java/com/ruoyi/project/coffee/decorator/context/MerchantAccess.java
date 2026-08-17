package com.ruoyi.project.coffee.decorator.context;

public class MerchantAccess
{
    private Long memberId;
    private Long merchantId;
    private String merchantName;
    private String role;
    private String storeScope;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStoreScope() { return storeScope; }
    public void setStoreScope(String storeScope) { this.storeScope = storeScope; }
}
