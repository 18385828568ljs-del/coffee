package com.ruoyi.project.coffee.scanOrder.wx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.HttpUtils;

/**
 * 微信 access_token 缓存服务
 *
 * 微信官方限制 access_token 每个小程序的有效期为 7200 秒(2 小时),
 * 同一时间只允许存在两个有效的 token,所以必须做内存缓存,避免频繁请求
 * 触发 limit。
 *
 * 当前实现仅做单实例内存缓存,适合本项目单节点部署。
 * 如果未来部署多节点,需要换 Redis。
 */
@Service
public class WxAccessTokenService
{
    private static final Logger log = LoggerFactory.getLogger(WxAccessTokenService.class);

    /** 提前 5 分钟刷新,避免边界过期 */
    private static final long REFRESH_GAP_MS = 5 * 60 * 1000L;

    @Value("${wx.miniapp.app-id:}")
    private String appId;

    @Value("${wx.miniapp.app-secret:}")
    private String appSecret;

    private volatile String cachedToken;

    /** 失效时间戳(System.currentTimeMillis 比较) */
    private volatile long expireAt;

    public String getAccessToken()
    {
        long now = System.currentTimeMillis();
        if (StringUtils.isNotEmpty(cachedToken) && now < expireAt - REFRESH_GAP_MS)
        {
            return cachedToken;
        }
        synchronized (this)
        {
            if (StringUtils.isNotEmpty(cachedToken) && now < expireAt - REFRESH_GAP_MS)
            {
                return cachedToken;
            }
            refresh();
            return cachedToken;
        }
    }

    private void refresh()
    {
        if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(appSecret))
        {
            throw new ServiceException("wx.miniapp.app-id / app-secret 未配置");
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token";
        String params = "grant_type=client_credential&appid=" + appId + "&secret=" + appSecret;
        String res = HttpUtils.sendGet(url, params);
        if (StringUtils.isEmpty(res))
        {
            throw new ServiceException("微信 access_token 接口无响应,请检查后端到 api.weixin.qq.com 的网络");
        }
        JSONObject obj;
        try
        {
            obj = JSON.parseObject(res);
        }
        catch (Exception e)
        {
            throw new ServiceException("微信 access_token 响应解析失败: " + res);
        }
        String token = obj.getString("access_token");
        Integer expiresIn = obj.getInteger("expires_in");
        if (StringUtils.isEmpty(token))
        {
            String errcode = obj.getString("errcode");
            String errmsg = obj.getString("errmsg");
            log.error("微信获取 access_token 失败: {}", res);
            throw new ServiceException("微信 access_token 获取失败: errcode=" + errcode + ", errmsg=" + errmsg);
        }
        this.cachedToken = token;
        this.expireAt = System.currentTimeMillis() + (expiresIn == null ? 7200 : expiresIn) * 1000L;
        log.info("微信 access_token 已刷新,有效期 {} 秒", expiresIn);
    }
}
