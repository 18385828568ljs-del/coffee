package com.ruoyi.project.coffee.decorator.context;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorMembershipMapper;

@Service
public class TenantContextService
{
    @Autowired
    private DecoratorMembershipMapper membershipMapper;

    public List<MerchantAccess> listMemberships(Long userId)
    {
        return membershipMapper.selectActiveMemberships(userId);
    }

    public TenantContext resolve(Long userId, Long merchantId)
    {
        if (userId == null || merchantId == null)
        {
            throw new ServiceException("请先选择商家");
        }
        MerchantAccess membership = membershipMapper.selectActiveMembership(userId, merchantId);
        if (membership == null)
        {
            throw new ServiceException("无权访问该商家");
        }
        List<DecoratorStoreAccess> stores = membershipMapper.selectAccessibleStores(
                membership.getMemberId(), membership.getMerchantId());
        Set<Long> storeIds = new LinkedHashSet<Long>();
        for (DecoratorStoreAccess store : stores)
        {
            storeIds.add(store.getStoreId());
        }
        return new TenantContext(userId, membership.getMemberId(), membership.getMerchantId(),
                membership.getRole(), membership.getStoreScope(), storeIds);
    }

    public void requireStore(TenantContext context, Long storeId)
    {
        if (context == null || !context.canAccessStore(storeId))
        {
            throw new ServiceException("无权访问该门店");
        }
    }

    public void requirePermission(TenantContext context, DecoratorPermission permission)
    {
        if (context == null || !permissionsFor(context.getRole()).contains(permission))
        {
            throw new ServiceException("当前角色无此操作权限");
        }
    }

    public EnumSet<DecoratorPermission> permissionsFor(String role)
    {
        if ("OWNER".equals(role) || "ADMIN".equals(role))
        {
            return EnumSet.allOf(DecoratorPermission.class);
        }
        if ("DESIGNER".equals(role))
        {
            return EnumSet.of(DecoratorPermission.THEME_VIEW, DecoratorPermission.THEME_EDIT,
                    DecoratorPermission.THEME_PREVIEW, DecoratorPermission.ASSET_VIEW,
                    DecoratorPermission.ASSET_MANAGE,
                    DecoratorPermission.VERSION_VIEW);
        }
        if ("OPERATOR".equals(role))
        {
            return EnumSet.of(DecoratorPermission.THEME_VIEW, DecoratorPermission.ASSET_VIEW,
                    DecoratorPermission.ASSET_MANAGE, DecoratorPermission.VERSION_VIEW);
        }
        if ("VIEWER".equals(role))
        {
            return EnumSet.of(DecoratorPermission.THEME_VIEW, DecoratorPermission.ASSET_VIEW,
                    DecoratorPermission.VERSION_VIEW);
        }
        return EnumSet.noneOf(DecoratorPermission.class);
    }
}
