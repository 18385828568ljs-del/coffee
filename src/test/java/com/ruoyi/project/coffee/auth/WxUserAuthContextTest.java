package com.ruoyi.project.coffee.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class WxUserAuthContextTest
{
    @AfterEach
    void tearDown()
    {
        WxUserAuthContext.clear();
    }

    @Test
    void getCurrentUserIdReturnsNullWhenNoUserBound()
    {
        assertNull(WxUserAuthContext.getCurrentUser());
        assertNull(WxUserAuthContext.getCurrentUserId());
    }

    @Test
    void setCurrentUserBindsUserToCurrentThread()
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(12L);
        user.setOpenid("openid-12");

        WxUserAuthContext.setCurrentUser(user);

        assertEquals(user, WxUserAuthContext.getCurrentUser());
        assertEquals(12L, WxUserAuthContext.getCurrentUserId());
    }

    @Test
    void clearRemovesCurrentUser()
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(12L);
        WxUserAuthContext.setCurrentUser(user);

        WxUserAuthContext.clear();

        assertNull(WxUserAuthContext.getCurrentUser());
        assertNull(WxUserAuthContext.getCurrentUserId());
    }
}

