package com.ruoyi.project.coffee.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

class AuthorizationUtilsTest
{
    @Test
    void checkOwnershipShouldReturnOriginalEntityWhenCurrentUserOwnsResource()
    {
        OwnedResource resource = new OwnedResource(10L);

        OwnedResource result = AuthorizationUtils.checkOwnership(10L, resource, OwnedResource::getUserId, "资源不存在");

        assertSame(resource, result);
    }

    @Test
    void checkOwnershipShouldRejectMissingResource()
    {
        ServiceException exception = assertThrows(ServiceException.class,
            () -> AuthorizationUtils.checkOwnership(10L, null, OwnedResource::getUserId, "资源不存在"));

        assertEquals("资源不存在", exception.getMessage());
    }

    @Test
    void checkOwnershipShouldRejectMissingOwnerId()
    {
        OwnedResource resource = new OwnedResource(null);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> AuthorizationUtils.checkOwnership(10L, resource, OwnedResource::getUserId, "资源不存在"));

        assertEquals("资源不存在", exception.getMessage());
    }

    @Test
    void checkOwnershipShouldRejectOtherUsersResource()
    {
        OwnedResource resource = new OwnedResource(99L);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> AuthorizationUtils.checkOwnership(10L, resource, OwnedResource::getUserId, "资源不存在"));

        assertEquals("资源不存在", exception.getMessage());
    }

    @Test
    void shortcutMethodsShouldUseBusinessSpecificErrorMessages()
    {
        OwnedResource otherUsersResource = new OwnedResource(99L);

        assertMessage("订单不存在",
            () -> AuthorizationUtils.checkOrderOwnership(10L, otherUsersResource, OwnedResource::getUserId));
        assertMessage("地址不存在",
            () -> AuthorizationUtils.checkAddressOwnership(10L, otherUsersResource, OwnedResource::getUserId));
        assertMessage("购物车记录不存在",
            () -> AuthorizationUtils.checkCartOwnership(10L, otherUsersResource, OwnedResource::getUserId));
    }

    private static void assertMessage(String expectedMessage, ThrowingRunnable runnable)
    {
        ServiceException exception = assertThrows(ServiceException.class, runnable::run);
        assertEquals(expectedMessage, exception.getMessage());
    }

    private interface ThrowingRunnable
    {
        void run();
    }

    private static class OwnedResource
    {
        private final Long userId;

        private OwnedResource(Long userId)
        {
            this.userId = userId;
        }

        private Long getUserId()
        {
            return userId;
        }
    }
}
