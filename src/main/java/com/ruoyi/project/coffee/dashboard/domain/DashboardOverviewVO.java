package com.ruoyi.project.coffee.dashboard.domain;

import java.util.List;

public class DashboardOverviewVO
{
    private String periodLabel;

    private AlertSummaryVO alerts;

    private KpiSummaryVO kpis;

    private List<TrendPointVO> salesTrend;

    private List<PieItemVO> orderStatusDistribution;

    private List<RankItemVO> topProducts;

    private List<PieItemVO> categorySalesShare;

    private List<TrendPointVO> userTrend;

    private MarketingSummaryVO marketingSummary;

    private List<StockWarningVO> lowStockProducts;

    private List<ActivityWarningVO> endingActivities;

    public String getPeriodLabel()
    {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel)
    {
        this.periodLabel = periodLabel;
    }

    public AlertSummaryVO getAlerts()
    {
        return alerts;
    }

    public void setAlerts(AlertSummaryVO alerts)
    {
        this.alerts = alerts;
    }

    public KpiSummaryVO getKpis()
    {
        return kpis;
    }

    public void setKpis(KpiSummaryVO kpis)
    {
        this.kpis = kpis;
    }

    public List<TrendPointVO> getSalesTrend()
    {
        return salesTrend;
    }

    public void setSalesTrend(List<TrendPointVO> salesTrend)
    {
        this.salesTrend = salesTrend;
    }

    public List<PieItemVO> getOrderStatusDistribution()
    {
        return orderStatusDistribution;
    }

    public void setOrderStatusDistribution(List<PieItemVO> orderStatusDistribution)
    {
        this.orderStatusDistribution = orderStatusDistribution;
    }

    public List<RankItemVO> getTopProducts()
    {
        return topProducts;
    }

    public void setTopProducts(List<RankItemVO> topProducts)
    {
        this.topProducts = topProducts;
    }

    public List<PieItemVO> getCategorySalesShare()
    {
        return categorySalesShare;
    }

    public void setCategorySalesShare(List<PieItemVO> categorySalesShare)
    {
        this.categorySalesShare = categorySalesShare;
    }

    public List<TrendPointVO> getUserTrend()
    {
        return userTrend;
    }

    public void setUserTrend(List<TrendPointVO> userTrend)
    {
        this.userTrend = userTrend;
    }

    public MarketingSummaryVO getMarketingSummary()
    {
        return marketingSummary;
    }

    public void setMarketingSummary(MarketingSummaryVO marketingSummary)
    {
        this.marketingSummary = marketingSummary;
    }

    public List<StockWarningVO> getLowStockProducts()
    {
        return lowStockProducts;
    }

    public void setLowStockProducts(List<StockWarningVO> lowStockProducts)
    {
        this.lowStockProducts = lowStockProducts;
    }

    public List<ActivityWarningVO> getEndingActivities()
    {
        return endingActivities;
    }

    public void setEndingActivities(List<ActivityWarningVO> endingActivities)
    {
        this.endingActivities = endingActivities;
    }
}
