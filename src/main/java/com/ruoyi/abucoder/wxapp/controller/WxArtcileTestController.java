package com.ruoyi.abucoder.wxapp.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.http.HttpUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/wxapi")
public class WxArtcileTestController {
    private final static String UID = "";
    private final static String MYKEY = "";

    public static void main(String[] args) {
        String wxurl = "https://mp.weixin.qq.com/s/mI7PH1Cr7RXXFzUNNXaVBA";
        String geturl = "https://qsys.scom.run/home/api";
        String myurl = "https://api.rasmall.cn/weChatArticle/";
//        String params = StrUtil.format("type=wxwz&uid={}&key={}&url={}", UID, MYKEY, wxurl);
        String params = StrUtil.format("url={}", wxurl);
//        String res = HttpUtils.sendGet(geturl,params);
        String res = HttpUtils.sendGet(myurl,params);
        System.out.println("输出请求到的结果");
        JSONObject jsonObject = JSON.parseObject(res);
        System.out.println(jsonObject.get("voice"));
    }
}
