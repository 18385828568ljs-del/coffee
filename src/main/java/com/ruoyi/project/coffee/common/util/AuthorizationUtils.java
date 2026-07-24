package com.ruoyi.project.coffee.common.util;

import java.util.function.Function;
import com.ruoyi.common.exception.ServiceException;

/**
 * 越权检查工具类
 *
 * 统一处理订单、地址、购物车等资源的归属校验,避免横向越权(IDOR)漏洞
 */
public class AuthorizationUtils
{
    /**
     * 检查资源归属,不匹配则抛异常
     *
     * @param currentUserId 当前登录用户ID
     * @param entity 待检查的资源实体
     * @param ownerIdGetter 获取资源所有者ID的函数(如 TOrder::getUserId)
     * @param errorMessage 校验失败时的错误信息
     * @param <T> 资源类型
     * @return 校验通过后返回原实体
     * @throws ServiceException 当资源不存在或不属于当前用户时抛出
     */
    public static <T> T checkOwnership(Long currentUserId, T entity,
                                       Function<T, Long> ownerIdGetter,
                                       String errorMessage)
    {
        if (entity == null)
        {
            throw new ServiceException(errorMessage);
        }

        Long ownerId = ownerIdGetter.apply(entity);
        if (ownerId == null || !currentUserId.equals(ownerId))
        {
            throw new ServiceException(errorMessage);
        }

        return entity;
    }

    /**
     * 检查订单归属(便捷方法)
     */
    public static <T> T checkOrderOwnership(Long currentUserId, T order,
                                           Function<T, Long> ownerIdGetter)
    {
        return checkOwnership(currentUserId, order, ownerIdGetter, "订单不存在");
    }

    /**
     * 检查地址归属(便捷方法)
     */
    public static <T> T checkAddressOwnership(Long currentUserId, T address,
                                             Function<T, Long> ownerIdGetter)
    {
        return checkOwnership(currentUserId, address, ownerIdGetter, "地址不存在");
    }

    /**
     * 检查购物车归属(便捷方法)
     */
    public static <T> T checkCartOwnership(Long currentUserId, T cart,
                                          Function<T, Long> ownerIdGetter)
    {
        return checkOwnership(currentUserId, cart, ownerIdGetter, "购物车记录不存在");
    }
}
