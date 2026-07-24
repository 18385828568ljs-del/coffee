package com.ruoyi.abucoder.wxapp.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.http.HttpUtils;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.abucoder.wxuser.service.IAbucoderWxuserService;
import com.ruoyi.project.common.storage.FileStorageService;
import com.ruoyi.project.common.storage.StoredFileInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.project.coffee.auth.WxUserTokenService;

/**
 * 微信小程序登录接口
 */
@RestController
@RequestMapping("/wxapi")
public class WxloginController
{
    @Autowired
    private IAbucoderWxuserService iAbucoderWxuserService;

    @Autowired
    private WxUserTokenService wxUserTokenService;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${wx.miniapp.app-id:}")
    private String appId;

    @Value("${wx.miniapp.app-secret:}")
    private String appSecret;

    @GetMapping("/me")
    public AjaxResult me(@RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestHeader(value = "X-Wx-Token", required = false) String wxToken)
    {
        String token = null;
        if (StringUtils.hasLength(authorization))
        {
            token = authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : authorization.trim();
        }
        else if (StringUtils.hasLength(wxToken))
        {
            token = wxToken.trim();
        }

        AbucoderWxuser wxuser = wxUserTokenService.getUserByToken(token);
        if (wxuser == null)
        {
            return AjaxResult.error("登录已失效");
        }

        AjaxResult result = AjaxResult.success(wxuser);
        result.put("token", wxUserTokenService.createToken(wxuser));
        return result;
    }

    @PostMapping("/wxlogin")
    public AjaxResult wxLogin(@RequestBody JSONObject object)
    {
        if (!StringUtils.hasLength(appId) || !StringUtils.hasLength(appSecret))
        {
            return AjaxResult.error("微信小程序配置缺失");
        }
        if (object == null || !StringUtils.hasLength(object.getString("code")))
        {
            return AjaxResult.error("缺少登录凭证 code");
        }

        String url = "https://api.weixin.qq.com/sns/jscode2session";
        String params = StrUtil.format(
            "appid={}&secret={}&js_code={}&grant_type=authorization_code",
            appId,
            appSecret,
            object.getString("code"));

        String res = HttpUtils.sendGet(url, params);
        if (!StringUtils.hasLength(res))
        {
            return AjaxResult.error("微信接口无响应");
        }
        JSONObject jsonObject = JSON.parseObject(res);
        Integer errcode = jsonObject.getInteger("errcode");
        if (errcode != null && errcode != 0)
        {
            String errmsg = jsonObject.getString("errmsg");
            return AjaxResult.error("微信登录失败: " + errcode + " " + errmsg);
        }
        String openid = jsonObject.getString("openid");
        if (!StringUtils.hasLength(openid))
        {
            return AjaxResult.error("未获取到 openid");
        }

        AbucoderWxuser wxuser = iAbucoderWxuserService.selectAbucoderWxuserOpenID(openid);
        if (wxuser == null)
        {
            wxuser = new AbucoderWxuser();
            wxuser.setOpenid(openid);
            wxuser.setCreateTime(DateUtils.getNowDate());
            iAbucoderWxuserService.insertAbucoderWxuser(wxuser);
        }

        AjaxResult result = AjaxResult.success(wxuser);
        result.put("token", wxUserTokenService.createToken(wxuser));
        return result;
    }

    @PostMapping("/saveUserInfo")
    @ResponseBody
    public AjaxResult saveUserInfo(@RequestBody JSONObject object,
        @RequestHeader(value = "Authorization", required = false) String authorization)
    {
        AbucoderWxuser abucoderWxuser = null;
        if (StringUtils.hasLength(authorization))
        {
            String token = authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : authorization.trim();
            abucoderWxuser = wxUserTokenService.getUserByToken(token);
        }

        if (abucoderWxuser == null && (object == null || !StringUtils.hasLength(object.getString("openid"))))
        {
            return AjaxResult.error("缺少登录态");
        }

        if (abucoderWxuser == null)
        {
            abucoderWxuser = iAbucoderWxuserService.selectAbucoderWxuserOpenID(object.getString("openid"));
        }
        if (abucoderWxuser == null)
        {
            return AjaxResult.error("用户不存在");
        }

        if (StringUtils.hasLength(object.getString("nickName")))
        {
            abucoderWxuser.setNickname(object.getString("nickName"));
            abucoderWxuser.setCreateBy(object.getString("nickName"));
        }
        if (StringUtils.hasLength(object.getString("avatarUrl")))
        {
            abucoderWxuser.setAvatar(object.getString("avatarUrl"));
        }

        abucoderWxuser.setUpdateTime(DateUtils.getNowDate());
        iAbucoderWxuserService.updateAbucoderWxuser(abucoderWxuser);
        AjaxResult result = AjaxResult.success(abucoderWxuser);
        result.put("token", wxUserTokenService.createToken(abucoderWxuser));
        return result;
    }

    @PostMapping("/uploadAvatar")
    @ResponseBody
    public AjaxResult uploadAvatar(MultipartFile file,
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestHeader(value = "X-Wx-Token", required = false) String wxToken)
    {
        String token = null;
        if (StringUtils.hasLength(authorization))
        {
            token = authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : authorization.trim();
        }
        else if (StringUtils.hasLength(wxToken))
        {
            token = wxToken.trim();
        }

        if (wxUserTokenService.getUserByToken(token) == null)
        {
            return AjaxResult.error("登录已失效");
        }
        if (file == null || file.isEmpty())
        {
            return AjaxResult.error("缺少头像文件");
        }

        try
        {
            StoredFileInfo storedFile = fileStorageService.upload(file);
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", storedFile.getUrl());
            ajax.put("fileName", storedFile.getFileName());
            ajax.put("newFileName", storedFile.getNewFileName());
            ajax.put("originalFilename", storedFile.getOriginalFilename());
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
