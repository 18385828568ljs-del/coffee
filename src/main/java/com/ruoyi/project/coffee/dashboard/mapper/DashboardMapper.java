package com.ruoyi.project.coffee.dashboard.mapper;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.project.coffee.dashboard.domain.ActivityWarningVO;
import com.ruoyi.project.coffee.dashboard.domain.DashboardQueryDTO;
import com.ruoyi.project.coffee.dashboard.domain.DiscountOrderSummaryVO;
import com.ruoyi.project.coffee.dashboard.domain.PieItemVO;
import com.ruoyi.project.coffee.dashboard.domain.RankItemVO;
import com.ruoyi.project.coffee.dashboard.domain.StatusCountVO;
import com.ruoyi.project.coffee.dashboard.domain.StockWarningVO;
import com.ruoyi.project.coffee.dashboard.domain.TrendPointVO;

public interface DashboardMapper
{
    BigDecimal sumSalesAmount(DashboardQueryDTO query);

    Long countOrders(DashboardQueryDTO query);

    Long countPaidOrders(DashboardQueryDTO query);

    Long countNewUsers(DashboardQueryDTO query);

    Long countPendingShipment();

    Long countLowStockProducts(DashboardQueryDTO query);

    Long countEndingActivities(DashboardQueryDTO query);

    Long countActiveActivities(DashboardQueryDTO query);

    DiscountOrderSummaryVO selectDiscountOrderSummary(DashboardQueryDTO query);

    List<TrendPointVO> selectSalesTrend(DashboardQueryDTO query);

    List<StatusCountVO> selectOrderStatusDistribution(DashboardQueryDTO query);

    List<RankItemVO> selectTopProducts(DashboardQueryDTO query);

    List<PieItemVO> selectCategorySalesShare(DashboardQueryDTO query);

    List<TrendPointVO> selectUserTrend(DashboardQueryDTO query);

    List<StockWarningVO> selectLowStockProducts(DashboardQueryDTO query);

    List<ActivityWarningVO> selectEndingActivities(DashboardQueryDTO query);
}
