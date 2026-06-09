package com.ruoyi.project.coffee.dashboard.service;

import com.ruoyi.project.coffee.dashboard.domain.DashboardOverviewVO;

public interface IDashboardService
{
    DashboardOverviewVO getOverview(String period, String startDate, String endDate);
}
