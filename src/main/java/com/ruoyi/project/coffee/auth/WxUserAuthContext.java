package com.ruoyi.project.coffee.auth;

import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;

/**
 * 小程序鉴权上下文。
 */
public class WxUserAuthContext
{
    private static final ThreadLocal<AbucoderWxuser> CURRENT_USER = new ThreadLocal<AbucoderWxuser>();

    private WxUserAuthContext()
    {
    }

    public static void setCurrentUser(AbucoderWxuser user)
    {
        CURRENT_USER.set(user);
    }

    public static AbucoderWxuser getCurrentUser()
    {
        return CURRENT_USER.get();
    }

    public static Long getCurrentUserId()
    {
        AbucoderWxuser user = getCurrentUser();
        return user == null ? null : user.getId();
    }

    public static void clear()
    {
        CURRENT_USER.remove();
    }
}
