package com.ruoyi.project.coffee.decorator.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.decorator.context.DecoratorStoreAccess;
import com.ruoyi.project.coffee.decorator.context.MerchantAccess;

public interface DecoratorMembershipMapper
{
    List<MerchantAccess> selectActiveMemberships(@Param("userId") Long userId);

    MerchantAccess selectActiveMembership(@Param("userId") Long userId,
            @Param("merchantId") Long merchantId);

    List<DecoratorStoreAccess> selectAccessibleStores(@Param("memberId") Long memberId,
            @Param("merchantId") Long merchantId);
}
