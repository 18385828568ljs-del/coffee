package com.ruoyi.project.coffee.api;

import java.util.Collections;
import java.util.List;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.activity.domain.MarketingPreviewResult;
import com.ruoyi.project.coffee.activity.domain.TMarketingActivity;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序营销活动接口
 */
@RestController
@RequestMapping("/api/activity")
public class ActivityApiController
{
    @Autowired
    private MarketingActivityEngine marketingActivityEngine;

    @GetMapping("/list")
    public AjaxResult getActivityList()
    {
        List<TMarketingActivity> activities = marketingActivityEngine.getActiveActivities();
        return AjaxResult.success(activities);
    }

    @PostMapping("/preview")
    public AjaxResult preview(@RequestBody TOrder order)
    {
        try
        {
            List<TOrderItem> orderItems = order == null ? Collections.<TOrderItem>emptyList() : order.getOrderItems();
            MarketingPreviewResult previewResult = marketingActivityEngine.previewOrder(WxUserAuthContext.getCurrentUserId(), orderItems);
            return AjaxResult.success(previewResult);
        }
        catch (IllegalArgumentException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
