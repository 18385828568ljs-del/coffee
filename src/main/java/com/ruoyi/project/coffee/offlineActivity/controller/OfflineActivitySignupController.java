package com.ruoyi.project.coffee.offlineActivity.controller;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivity;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivitySignup;
import com.ruoyi.project.coffee.offlineActivity.service.IOfflineActivityService;

@Controller
@RequestMapping("/coffee/offlineActivitySignup")
public class OfflineActivitySignupController extends BaseController
{
    private String prefix = "coffee/offlineActivitySignup";

    @Autowired
    private IOfflineActivityService offlineActivityService;

    @RequiresPermissions("coffee:offlineActivity:view")
    @GetMapping("/{activityId}")
    public String signup(@PathVariable("activityId") Long activityId, ModelMap mmap)
    {
        OfflineActivity activity = offlineActivityService.selectOfflineActivityByActivityId(activityId);
        mmap.put("activity", activity);
        mmap.put("activityId", activityId);
        return prefix + "/signup";
    }

    @RequiresPermissions("coffee:offlineActivity:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(OfflineActivitySignup signup)
    {
        startPage();
        List<OfflineActivitySignup> list = offlineActivityService.selectSignupList(signup);
        return getDataTable(list);
    }

    @RequiresPermissions("coffee:offlineActivity:edit")
    @Log(title = "线下活动报名", businessType = BusinessType.UPDATE)
    @PostMapping("/mark")
    @ResponseBody
    public AjaxResult mark(Long signupId, Integer status)
    {
        try
        {
            return toAjax(offlineActivityService.markSignupStatus(signupId, status));
        }
        catch (IllegalArgumentException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
