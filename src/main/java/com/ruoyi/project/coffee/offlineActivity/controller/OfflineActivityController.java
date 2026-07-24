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
import com.ruoyi.project.coffee.offlineActivity.service.IOfflineActivityService;

@Controller
@RequestMapping("/coffee/offlineActivity")
public class OfflineActivityController extends BaseController
{
    private String prefix = "coffee/offlineActivity";

    @Autowired
    private IOfflineActivityService offlineActivityService;

    @RequiresPermissions("coffee:offlineActivity:view")
    @GetMapping()
    public String offlineActivity()
    {
        return prefix + "/offlineActivity";
    }

    @RequiresPermissions("coffee:offlineActivity:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(OfflineActivity offlineActivity)
    {
        startPage();
        List<OfflineActivity> list = offlineActivityService.selectOfflineActivityList(offlineActivity);
        return getDataTable(list);
    }

    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @RequiresPermissions("coffee:offlineActivity:add")
    @Log(title = "线下活动", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(OfflineActivity offlineActivity)
    {
        AjaxResult validateResult = validateActivity(offlineActivity);
        if (isErrorResult(validateResult))
        {
            return validateResult;
        }
        return toAjax(offlineActivityService.insertOfflineActivity(offlineActivity));
    }

    @RequiresPermissions("coffee:offlineActivity:edit")
    @GetMapping("/edit/{activityId}")
    public String edit(@PathVariable("activityId") Long activityId, ModelMap mmap)
    {
        OfflineActivity offlineActivity = offlineActivityService.selectOfflineActivityByActivityId(activityId);
        mmap.put("offlineActivity", offlineActivity);
        return prefix + "/edit";
    }

    @RequiresPermissions("coffee:offlineActivity:edit")
    @Log(title = "线下活动", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(OfflineActivity offlineActivity)
    {
        AjaxResult validateResult = validateActivity(offlineActivity);
        if (isErrorResult(validateResult))
        {
            return validateResult;
        }
        return toAjax(offlineActivityService.updateOfflineActivity(offlineActivity));
    }

    @RequiresPermissions("coffee:offlineActivity:remove")
    @Log(title = "线下活动", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(offlineActivityService.deleteOfflineActivityByActivityIds(ids));
    }

    private AjaxResult validateActivity(OfflineActivity activity)
    {
        if (activity == null)
        {
            return AjaxResult.error("活动信息不能为空");
        }
        if (activity.getTitle() == null || activity.getTitle().trim().isEmpty())
        {
            return AjaxResult.error("请填写活动标题");
        }
        if (activity.getStartTime() == null)
        {
            return AjaxResult.error("请选择活动开始时间");
        }
        if (activity.getEndTime() != null && activity.getEndTime().before(activity.getStartTime()))
        {
            return AjaxResult.error("结束时间不能早于开始时间");
        }
        if (activity.getSignupDeadline() != null && activity.getSignupDeadline().after(activity.getStartTime()))
        {
            return AjaxResult.error("预约截止时间不能晚于活动开始时间");
        }
        if (activity.getQuota() == null || activity.getQuota() < 1)
        {
            return AjaxResult.error("人数上限必须大于0");
        }
        return AjaxResult.success();
    }

    private boolean isErrorResult(AjaxResult result)
    {
        Object code = result == null ? null : result.get("code");
        return code != null && !"0".equals(String.valueOf(code)) && !"200".equals(String.valueOf(code));
    }
}
