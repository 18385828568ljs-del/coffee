package com.ruoyi.project.coffee.dashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.dashboard.service.IDashboardService;

@Controller
@RequestMapping("/coffee/dashboard")
public class DashboardController
{
    @Autowired
    private IDashboardService dashboardService;

    @GetMapping("/overview")
    @ResponseBody
    public AjaxResult overview(String period, String startDate, String endDate)
    {
        try
        {
            return AjaxResult.success(dashboardService.getOverview(period, startDate, endDate));
        }
        catch (IllegalArgumentException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
