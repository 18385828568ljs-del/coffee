package com.ruoyi.project.coffee.scanOrder.wx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.HttpUtils;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;

/**
 * Send mini-program subscribe messages for scan orders.
 */
@Service
public class ScanOrderSubscribeMessageService
{
    private static final Logger log = LoggerFactory.getLogger(ScanOrderSubscribeMessageService.class);

    @Value("${wx.miniapp.subscribe.pickup-template-id:}")
    private String pickupTemplateId;

    @Autowired
    private WxAccessTokenService wxAccessTokenService;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void sendPickupNoticeAsync(final ScanOrder order)
    {
        if (order == null || StringUtils.isEmpty(order.getOpenid()) || StringUtils.isEmpty(pickupTemplateId))
        {
            return;
        }
        threadPoolTaskExecutor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                sendPickupNotice(order);
            }
        });
    }

    private void sendPickupNotice(ScanOrder order)
    {
        try
        {
            String accessToken = wxAccessTokenService.getAccessToken();
            String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;
            String response = HttpUtils.sendPost(url, buildPickupBody(order).toJSONString());
            if (StringUtils.isEmpty(response))
            {
                log.warn("WeChat subscribe message returned empty response, orderId={}", order.getOrderId());
                return;
            }
            JSONObject result = JSON.parseObject(response);
            Integer errcode = result.getInteger("errcode");
            if (errcode != null && errcode != 0)
            {
                log.warn("WeChat subscribe message failed, orderId={}, response={}", order.getOrderId(), response);
            }
        }
        catch (Exception e)
        {
            log.warn("WeChat subscribe message error, orderId={}", order.getOrderId(), e);
        }
    }

    private JSONObject buildPickupBody(ScanOrder order)
    {
        JSONObject body = new JSONObject();
        body.put("touser", order.getOpenid());
        body.put("template_id", pickupTemplateId);
        body.put("page", "pages/scan/detail?orderId=" + order.getOrderId());
        // 正式版小程序使用 "formal"，开发版使用 "developer"，体验版使用 "trial"
        body.put("miniprogram_state", "formal");

        JSONObject data = new JSONObject();
        data.put("character_string10", value(firstNotEmpty(order.getPickupNo(), "-")));
        data.put("thing5", value("您的订单已完成"));
        body.put("data", data);
        return body;
    }

    private JSONObject value(String value)
    {
        JSONObject item = new JSONObject();
        item.put("value", value);
        return item;
    }

    private String firstNotEmpty(String first, String fallback)
    {
        return StringUtils.isNotEmpty(first) ? first : fallback;
    }
}
