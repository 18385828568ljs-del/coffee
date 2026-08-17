package com.ruoyi.project.coffee.decorator.context;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Immutable merchant context resolved from the authenticated backend session. */
public final class TenantContext
{
    private final Long userId;
    private final Long memberId;
    private final Long merchantId;
    private final String role;
    private final String storeScope;
    private final Set<Long> storeIds;

    public TenantContext(Long userId, Long memberId, Long merchantId, String role,
            String storeScope, Set<Long> storeIds)
    {
        this.userId = userId;
        this.memberId = memberId;
        this.merchantId = merchantId;
        this.role = role;
        this.storeScope = storeScope;
        this.storeIds = storeIds == null
                ? Collections.<Long>emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<Long>(storeIds));
    }

    public Long getUserId() { return userId; }
    public Long getMemberId() { return memberId; }
    public Long getMerchantId() { return merchantId; }
    public String getRole() { return role; }
    public String getStoreScope() { return storeScope; }
    public Set<Long> getStoreIds() { return storeIds; }

    public boolean canAccessStore(Long storeId)
    {
        return storeId != null && ("ALL".equals(storeScope) || storeIds.contains(storeId));
    }
}
