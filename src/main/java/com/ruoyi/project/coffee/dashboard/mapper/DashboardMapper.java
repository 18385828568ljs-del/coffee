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
    // 商城订单统计
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

    // 扫码点单统计
    BigDecimal sumScanSalesAmount(DashboardQueryDTO query);

    Long countScanOrders(DashboardQueryDTO query);

    Long countScanPaidOrders(DashboardQueryDTO query);

    Long countScanPendingAccept();

    Long countScanMaking();

    Long countScanHighUrge(DashboardQueryDTO query);

    BigDecimal avgScanWaitMinutes(DashboardQueryDTO query);

    Long sumScanUrgeCount(DashboardQueryDTO query);

    List<TrendPointVO> selectScanSalesTrend(DashboardQueryDTO query);

    List<StatusCountVO> selectScanOrderStatusDistribution(DashboardQueryDTO query);

    List<RankItemVO> selectScanTopProducts(DashboardQueryDTO query);

    List<PieItemVO> selectScanCategorySalesShare(DashboardQueryDTO query);
}
