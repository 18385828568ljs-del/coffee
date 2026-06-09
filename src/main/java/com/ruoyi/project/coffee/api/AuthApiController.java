package com.ruoyi.project.coffee.api;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.abucoder.wxuser.service.IAbucoderWxuserService;
import com.ruoyi.project.coffee.auth.WxUserTokenService;

/**
 * 小程序登录认证API
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController extends BaseController
{
    @Value("${wx.miniapp.app-id}")
    private String appId;

    @Value("${wx.miniapp.app-secret}")
    private String appSecret;

    @Autowired
    private IAbucoderWxuserService wxuserService;

    @Autowired
    private WxUserTokenService tokenService;

    /**
     * 微信小程序登录
     *
     * @param params 包含 code 字段
     * @return token 和用户信息
     */
    @PostMapping("/wxLogin")
    public AjaxResult wxLogin(@RequestBody Map<String, String> params)
    {
        String code = params.get("code");
        if (StringUtils.isEmpty(code))
        {
            return AjaxResult.error("code 不能为空");
        }

        // 调用微信 code2Session 接口
        String openid;
        try
        {
            openid = getOpenIdByCode(code);
        }
        catch (Exception e)
        {
            logger.error("调用微信 code2Session 失败", e);
            return AjaxResult.error("登录失败,请重试");
        }

        if (StringUtils.isEmpty(openid))
        {
            return AjaxResult.error("获取微信用户信息失败");
        }

        // 查询或创建用户
        AbucoderWxuser wxuser = wxuserService.selectAbucoderWxuserOpenID(openid);
        if (wxuser == null)
        {
            // 首次登录,创建用户
            wxuser = new AbucoderWxuser();
            wxuser.setOpenid(openid);
            wxuser.setCreateTime(DateUtils.getNowDate());
            wxuser.setUpdateTime(DateUtils.getNowDate());
            wxuserService.insertAbucoderWxuser(wxuser);

            // 重新查询获取ID
            wxuser = wxuserService.selectAbucoderWxuserOpenID(openid);
        }
        else
        {
            // 更新最后登录时间
            wxuser.setUpdateTime(DateUtils.getNowDate());
            wxuserService.updateAbucoderWxuser(wxuser);
        }

        if (wxuser == null || wxuser.getId() == null)
        {
            return AjaxResult.error("用户创建失败");
        }

        // 生成 token
        String token = tokenService.createToken(wxuser);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", wxuser.getId());
        result.put("openid", wxuser.getOpenid());
        result.put("nickName", wxuser.getNickname());
        result.put("avatarUrl", wxuser.getAvatar());

        return AjaxResult.success("登录成功", result);
    }

    /**
     * 调用微信 code2Session 接口获取 openid
     */
    private String getOpenIdByCode(String code)
    {
        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appId, appSecret, code
        );

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        if (StringUtils.isEmpty(response))
        {
            throw new RuntimeException("微信接口返回为空");
        }

        JSONObject json = JSONObject.parseObject(response);

        // 检查错误码
        Integer errcode = json.getInteger("errcode");
        if (errcode != null && errcode != 0)
        {
            String errmsg = json.getString("errmsg");
            throw new RuntimeException("微信接口返回错误: " + errcode + " " + errmsg);
        }

        return json.getString("openid");
    }
}
