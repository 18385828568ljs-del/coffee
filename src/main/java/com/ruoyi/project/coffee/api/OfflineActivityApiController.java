package com.ruoyi.project.coffee.api;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.auth.WxUserTokenService;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivity;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivitySignup;
import com.ruoyi.project.coffee.offlineActivity.service.IOfflineActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/offlineActivity")
public class OfflineActivityApiController
{
    @Autowired
    private IOfflineActivityService offlineActivityService;

    @Autowired
    private WxUserTokenService wxUserTokenService;

    @GetMapping("/list")
    public AjaxResult list(HttpServletRequest request)
    {
        List<OfflineActivity> list = offlineActivityService.selectAvailableOfflineActivityList(wxUserTokenService.resolveUserId(request));
        return AjaxResult.success(list);
    }

    @GetMapping("/{activityId}")
    public AjaxResult detail(@PathVariable("activityId") Long activityId, HttpServletRequest request)
    {
        OfflineActivity activity = offlineActivityService.selectAvailableOfflineActivityById(activityId, wxUserTokenService.resolveUserId(request));
        if (activity == null)
        {
            return AjaxResult.error("活动不存在或已下架");
        }
        return AjaxResult.success(activity);
    }

    @PostMapping("/signup")
    public AjaxResult signup(@RequestBody Map<String, Object> body)
    {
        try
        {
            Long activityId = getLong(body, "activityId");
            OfflineActivitySignup signup = offlineActivityService.signup(activityId, WxUserAuthContext.getCurrentUserId());
            return AjaxResult.success(signup);
        }
        catch (IllegalArgumentException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("/cancel")
    public AjaxResult cancel(@RequestBody Map<String, Object> body)
    {
        try
        {
            Long activityId = getLong(body, "activityId");
            return AjaxResult.success(offlineActivityService.cancelSignup(activityId, WxUserAuthContext.getCurrentUserId()));
        }
        catch (IllegalArgumentException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("/my")
    public AjaxResult my()
    {
        List<OfflineActivitySignup> list = offlineActivityService.selectUserSignupList(WxUserAuthContext.getCurrentUserId());
        return AjaxResult.success(list);
    }

    private Long getLong(Map<String, Object> body, String key)
    {
        Object value = body == null ? null : body.get(key);
        if (value == null)
        {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }
}
