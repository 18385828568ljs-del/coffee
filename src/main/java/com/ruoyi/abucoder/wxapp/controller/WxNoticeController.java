package com.ruoyi.abucoder.wxapp.controller;

import com.ruoyi.framework.web.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * author：AbuCoder QQ:932696181
 * 微信小程序获取通知公告接口
 * Gitee:https://gitee.com/rahman
 */
@RestController
@RequestMapping("/wxapi")
public class WxNoticeController {

    @GetMapping("/loadNotice")
    public AjaxResult loadNotice(){
        return AjaxResult.success(Collections.emptyList());
    }
}
