package com.ruoyi.project.coffee.auth;

/**
 * 小程序用户会话。
 */
public class WxUserSession
{
    private Long userId;

    private String openid;

    private long expireAt;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getOpenid()
    {
        return openid;
    }

    public void setOpenid(String openid)
    {
        this.openid = openid;
    }

    public long getExpireAt()
    {
        return expireAt;
    }

    public void setExpireAt(long expireAt)
    {
        this.expireAt = expireAt;
    }
}
