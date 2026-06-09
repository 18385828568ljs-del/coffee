package com.ruoyi.abucoder.wxapp.controller;

import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.banners.domain.AbucoderBanners;
import com.ruoyi.project.abucoder.banners.service.IAbucoderBannersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * author：AbuCoder QQ:932696181
 * 微信小程序获取轮播图接口
 * Gitee:https://gitee.com/rahman
 */
@RestController
@RequestMapping("/wxapi")
public class WxBannerController {

    @Autowired
    IAbucoderBannersService iAbucoderBannersService;
    @GetMapping("/loadBanner")
    public AjaxResult loadBanner(AbucoderBanners abucoderBanners){
        List<AbucoderBanners> bannerslist = iAbucoderBannersService.selectAbucoderBannersList(abucoderBanners);
        return AjaxResult.success(bannerslist);
    }
}
