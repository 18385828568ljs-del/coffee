package com.ruoyi.project.coffee.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.abucoder.wxuser.service.IAbucoderWxuserService;

/**
 * 小程序 token 服务。
 *
 * 使用无状态签名 token，避免服务重启或多实例部署时登录态失效。
 */
@Component
public class WxUserTokenService
{
    private static final long SESSION_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String WX_TOKEN_HEADER = "X-Wx-Token";

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${wx.miniapp.token-secret:}")
    private String configuredTokenSecret;

    @Value("${wx.miniapp.app-secret:}")
    private String appSecret;

    @Autowired
    private IAbucoderWxuserService wxuserService;

    public String createToken(AbucoderWxuser wxuser)
    {
        if (wxuser == null || wxuser.getId() == null || !StringUtils.hasText(wxuser.getOpenid()))
        {
            throw new IllegalArgumentException("微信用户信息不完整");
        }

        long expireAt = System.currentTimeMillis() + SESSION_TTL_MILLIS;
        JSONObject payload = new JSONObject();
        payload.put("uid", wxuser.getId());
        payload.put("oid", wxuser.getOpenid());
        payload.put("exp", expireAt);
        String encodedPayload = encodePayload(payload.toJSONString());
        return encodedPayload + "." + sign(encodedPayload);
    }

    public AbucoderWxuser resolveUser(HttpServletRequest request)
    {
        if (request == null)
        {
            return null;
        }
        return getUserByToken(extractToken(request));
    }

    public Long resolveUserId(HttpServletRequest request)
    {
        AbucoderWxuser wxuser = resolveUser(request);
        return wxuser == null ? null : wxuser.getId();
    }

    public AbucoderWxuser getUserByToken(String token)
    {
        if (!StringUtils.hasText(token))
        {
            return null;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2)
        {
            return null;
        }
        if (!MessageDigest.isEqual(parts[1].getBytes(StandardCharsets.UTF_8), sign(parts[0]).getBytes(StandardCharsets.UTF_8)))
        {
            return null;
        }

        JSONObject payload = decodePayload(parts[0]);
        if (payload == null)
        {
            return null;
        }

        Long userId = payload.getLong("uid");
        String openid = payload.getString("oid");
        Long expireAt = payload.getLong("exp");
        if (userId == null || !StringUtils.hasText(openid) || expireAt == null || expireAt.longValue() < System.currentTimeMillis())
        {
            return null;
        }

        AbucoderWxuser wxuser = wxuserService.selectAbucoderWxuserById(userId);
        if (wxuser == null || !openid.equals(wxuser.getOpenid()))
        {
            return null;
        }
        return wxuser;
    }

    public String extractToken(HttpServletRequest request)
    {
        if (request == null)
        {
            return null;
        }

        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authorization))
        {
            String lowerCase = authorization.toLowerCase();
            if (lowerCase.startsWith("bearer "))
            {
                return authorization.substring(7).trim();
            }
            return authorization.trim();
        }

        String wxToken = request.getHeader(WX_TOKEN_HEADER);
        return StringUtils.hasText(wxToken) ? wxToken.trim() : null;
    }

    private String sign(String payload)
    {
        try
        {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(resolveSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signBytes);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("生成小程序 token 签名失败", e);
        }
    }

    private String resolveSecret()
    {
        if (StringUtils.hasText(configuredTokenSecret))
        {
            return configuredTokenSecret.trim();
        }
        if (StringUtils.hasText(appSecret))
        {
            return appSecret.trim();
        }
        return "ruoyi-wx-miniapp-token-secret";
    }

    private String encodePayload(String payload)
    {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private JSONObject decodePayload(String encodedPayload)
    {
        try
        {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedPayload);
            return JSONObject.parseObject(new String(decoded, StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
