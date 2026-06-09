package com.ruoyi.project.coffee.dashboard.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.dashboard.domain.ActivityWarningVO;
import com.ruoyi.project.coffee.dashboard.domain.AlertSummaryVO;
import com.ruoyi.project.coffee.dashboard.domain.DashboardOverviewVO;
import com.ruoyi.project.coffee.dashboard.domain.DashboardQueryDTO;
import com.ruoyi.project.coffee.dashboard.domain.DiscountOrderSummaryVO;
import com.ruoyi.project.coffee.dashboard.domain.KpiSummaryVO;
import com.ruoyi.project.coffee.dashboard.domain.MarketingSummaryVO;
import com.ruoyi.project.coffee.dashboard.domain.PieItemVO;
import com.ruoyi.project.coffee.dashboard.domain.RankItemVO;
import com.ruoyi.project.coffee.dashboard.domain.StatusCountVO;
import com.ruoyi.project.coffee.dashboard.domain.StockWarningVO;
import com.ruoyi.project.coffee.dashboard.domain.TrendPointVO;
import com.ruoyi.project.coffee.dashboard.mapper.DashboardMapper;
import com.ruoyi.project.coffee.dashboard.service.IDashboardService;

@Service
public class DashboardServiceImpl implements IDashboardService
{
    private static final int LOW_STOCK_THRESHOLD = 10;

    private static final int ENDING_ACTIVITY_DAYS = 3;

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    @Autowired
    private DashboardMapper dashboardMapper;

    @Override
    public DashboardOverviewVO getOverview(String period, String startDate, String endDate)
    {
        PeriodWindow window = buildPeriodWindow(period, startDate, endDate);
        DashboardQueryDTO query = window.getQuery();

        DashboardOverviewVO overview = new DashboardOverviewVO();
        overview.setPeriodLabel(query.getPeriodLabel());
        overview.setAlerts(buildAlerts(query));
        overview.setKpis(buildKpis(query));
        overview.setSalesTrend(fillAmountTrend(dashboardMapper.selectSalesTrend(query), window.getStartDate(), window.getEndDate()));
        overview.setOrderStatusDistribution(buildStatusDistribution(dashboardMapper.selectOrderStatusDistribution(query)));
        overview.setTopProducts(buildTopProducts(dashboardMapper.selectTopProducts(query)));
        overview.setCategorySalesShare(buildCategoryShare(dashboardMapper.selectCategorySalesShare(query)));
        overview.setUserTrend(fillCountTrend(dashboardMapper.selectUserTrend(query), window.getStartDate(), window.getEndDate()));
        overview.setMarketingSummary(buildMarketingSummary(query));
        overview.setLowStockProducts(buildLowStockProducts(dashboardMapper.selectLowStockProducts(query)));
        overview.setEndingActivities(buildEndingActivities(dashboardMapper.selectEndingActivities(query)));
        return overview;
    }

    private AlertSummaryVO buildAlerts(DashboardQueryDTO query)
    {
        AlertSummaryVO alerts = new AlertSummaryVO();
        alerts.setPendingShipmentCount(defaultLong(dashboardMapper.countPendingShipment()));
        alerts.setLowStockCount(defaultLong(dashboardMapper.countLowStockProducts(query)));
        alerts.setEndingActivityCount(defaultLong(dashboardMapper.countEndingActivities(query)));
        return alerts;
    }

    private KpiSummaryVO buildKpis(DashboardQueryDTO query)
    {
        KpiSummaryVO kpis = new KpiSummaryVO();
        BigDecimal salesAmount = money(dashboardMapper.sumSalesAmount(query));
        Long paidOrderCount = defaultLong(dashboardMapper.countPaidOrders(query));
        kpis.setSalesAmount(salesAmount);
        kpis.setOrderCount(defaultLong(dashboardMapper.countOrders(query)));
        kpis.setPaidOrderCount(paidOrderCount);
        kpis.setAverageOrderValue(calculateAverage(salesAmount, paidOrderCount));
        kpis.setNewUserCount(defaultLong(dashboardMapper.countNewUsers(query)));
        kpis.setPendingShipmentCount(defaultLong(dashboardMapper.countPendingShipment()));
        return kpis;
    }

    private MarketingSummaryVO buildMarketingSummary(DashboardQueryDTO query)
    {
        MarketingSummaryVO summary = new MarketingSummaryVO();
        summary.setActiveActivityCount(defaultLong(dashboardMapper.countActiveActivities(query)));
        summary.setEndingSoonCount(defaultLong(dashboardMapper.countEndingActivities(query)));
        DiscountOrderSummaryVO discountOrderSummary = dashboardMapper.selectDiscountOrderSummary(query);
        if (discountOrderSummary == null)
        {
            summary.setDiscountedOrderCount(0L);
            summary.setDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return summary;
        }
        summary.setDiscountedOrderCount(defaultLong(discountOrderSummary.getOrderCount()));
        summary.setDiscountAmount(money(discountOrderSummary.getDiscountAmount()));
        return summary;
    }

    private List<PieItemVO> buildStatusDistribution(List<StatusCountVO> rows)
    {
        Map<Integer, Long> countMap = new HashMap<Integer, Long>();
        if (rows != null)
        {
            for (StatusCountVO row : rows)
            {
                countMap.put(row.getStatus(), defaultLong(row.getCount()));
            }
        }

        List<PieItemVO> result = new ArrayList<PieItemVO>();
        for (int status = 0; status <= 4; status++)
        {
            PieItemVO item = new PieItemVO();
            Long count = countMap.containsKey(status) ? countMap.get(status) : 0L;
            item.setName(orderStatusLabel(status));
            item.setCount(count);
            item.setValue(BigDecimal.valueOf(count));
            result.add(item);
        }
        return result;
    }

    private List<RankItemVO> buildTopProducts(List<RankItemVO> rows)
    {
        List<RankItemVO> result = new ArrayList<RankItemVO>();
        if (rows == null)
        {
            return result;
        }
        for (RankItemVO row : rows)
        {
            RankItemVO item = new RankItemVO();
            item.setProductId(row.getProductId());
            item.setName(StringUtils.isEmpty(row.getName()) ? "未知商品" : row.getName());
            item.setAmount(money(row.getAmount()));
            item.setQuantity(defaultLong(row.getQuantity()));
            result.add(item);
        }
        return result;
    }

    private List<PieItemVO> buildCategoryShare(List<PieItemVO> rows)
    {
        List<PieItemVO> result = new ArrayList<PieItemVO>();
        if (rows == null)
        {
            return result;
        }
        for (PieItemVO row : rows)
        {
            PieItemVO item = new PieItemVO();
            item.setName(StringUtils.isEmpty(row.getName()) ? "未分类" : row.getName());
            item.setCount(defaultLong(row.getCount()));
            item.setValue(money(row.getValue()));
            result.add(item);
        }
        return result;
    }

    private List<StockWarningVO> buildLowStockProducts(List<StockWarningVO> rows)
    {
        List<StockWarningVO> result = new ArrayList<StockWarningVO>();
        if (rows == null)
        {
            return result;
        }
        for (StockWarningVO row : rows)
        {
            StockWarningVO item = new StockWarningVO();
            item.setProductId(row.getProductId());
            item.setProductName(StringUtils.isEmpty(row.getProductName()) ? "未命名商品" : row.getProductName());
            item.setStock(defaultLong(row.getStock()));
            item.setImageUrl(row.getImageUrl());
            result.add(item);
        }
        return result;
    }

    private List<ActivityWarningVO> buildEndingActivities(List<ActivityWarningVO> rows)
    {
        List<ActivityWarningVO> result = new ArrayList<ActivityWarningVO>();
        if (rows == null)
        {
            return result;
        }
        for (ActivityWarningVO row : rows)
        {
            ActivityWarningVO item = new ActivityWarningVO();
            item.setActivityId(row.getActivityId());
            item.setTitle(StringUtils.isEmpty(row.getTitle()) ? "未命名活动" : row.getTitle());
            item.setType(row.getType());
            item.setTypeLabel(activityTypeLabel(row.getType()));
            item.setEndTime(row.getEndTime());
            item.setEndDate(formatDate(row.getEndTime()));
            item.setRemainingDays(defaultLong(row.getRemainingDays()));
            result.add(item);
        }
        return result;
    }

    private List<TrendPointVO> fillAmountTrend(List<TrendPointVO> rows, LocalDate startDate, LocalDate endDate)
    {
        Map<LocalDate, BigDecimal> amountMap = new HashMap<LocalDate, BigDecimal>();
        if (rows != null)
        {
            for (TrendPointVO row : rows)
            {
                if (StringUtils.isEmpty(row.getDateLabel()))
                {
                    continue;
                }
                amountMap.put(LocalDate.parse(row.getDateLabel()), money(row.getAmount()));
            }
        }

        List<TrendPointVO> result = new ArrayList<TrendPointVO>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate))
        {
            TrendPointVO point = new TrendPointVO();
            point.setDateLabel(cursor.toString());
            point.setAmount(amountMap.containsKey(cursor) ? amountMap.get(cursor) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            result.add(point);
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private List<TrendPointVO> fillCountTrend(List<TrendPointVO> rows, LocalDate startDate, LocalDate endDate)
    {
        Map<LocalDate, Long> countMap = new HashMap<LocalDate, Long>();
        if (rows != null)
        {
            for (TrendPointVO row : rows)
            {
                if (StringUtils.isEmpty(row.getDateLabel()))
                {
                    continue;
                }
                countMap.put(LocalDate.parse(row.getDateLabel()), defaultLong(row.getCount()));
            }
        }

        List<TrendPointVO> result = new ArrayList<TrendPointVO>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate))
        {
            TrendPointVO point = new TrendPointVO();
            point.setDateLabel(cursor.toString());
            point.setCount(countMap.containsKey(cursor) ? countMap.get(cursor) : 0L);
            result.add(point);
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private PeriodWindow buildPeriodWindow(String period, String startDate, String endDate)
    {
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;
        String normalizedPeriod = StringUtils.isEmpty(period) ? "month" : period.trim().toLowerCase();
        String periodLabel;

        if ("today".equals(normalizedPeriod))
        {
            start = today;
            end = today;
            periodLabel = "今日";
        }
        else if ("7d".equals(normalizedPeriod))
        {
            start = today.minusDays(6);
            end = today;
            periodLabel = "近7天";
        }
        else if ("30d".equals(normalizedPeriod))
        {
            start = today.minusDays(29);
            end = today;
            periodLabel = "近30天";
        }
        else if ("month".equals(normalizedPeriod))
        {
            start = today.withDayOfMonth(1);
            end = today;
            periodLabel = "本月累计";
        }
        else if ("custom".equals(normalizedPeriod))
        {
            if (StringUtils.isEmpty(startDate) || StringUtils.isEmpty(endDate))
            {
                throw new IllegalArgumentException("自定义时间范围不能为空");
            }
            try
            {
                start = LocalDate.parse(startDate);
                end = LocalDate.parse(endDate);
            }
            catch (DateTimeParseException e)
            {
                throw new IllegalArgumentException("自定义日期格式不正确，应为 yyyy-MM-dd");
            }
            if (start.isAfter(end))
            {
                throw new IllegalArgumentException("开始日期不能晚于结束日期");
            }
            periodLabel = startDate + " 至 " + endDate;
        }
        else
        {
            throw new IllegalArgumentException("不支持的统计周期");
        }

        DashboardQueryDTO query = new DashboardQueryDTO();
        query.setPeriod(normalizedPeriod);
        query.setPeriodLabel(periodLabel);
        query.setStartTime(toDate(start.atStartOfDay()));
        query.setEndTimeExclusive(toDate(end.plusDays(1).atStartOfDay()));
        query.setNowTime(new Date());
        query.setEndingSoonTime(toDate(today.plusDays(ENDING_ACTIVITY_DAYS + 1).atStartOfDay()));
        query.setLowStockThreshold(LOW_STOCK_THRESHOLD);
        return new PeriodWindow(query, start, end);
    }

    private Date toDate(LocalDateTime dateTime)
    {
        return Date.from(dateTime.atZone(ZONE_ID).toInstant());
    }

    private Long defaultLong(Long value)
    {
        return value == null ? 0L : value;
    }

    private BigDecimal money(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverage(BigDecimal amount, Long count)
    {
        if (count == null || count.longValue() <= 0L)
        {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.divide(BigDecimal.valueOf(count.longValue()), 2, RoundingMode.HALF_UP);
    }

    private String orderStatusLabel(int status)
    {
        switch (status)
        {
            case 0:
                return "待支付";
            case 1:
                return "待发货";
            case 2:
                return "已发货";
            case 3:
                return "已完成";
            case 4:
                return "已取消";
            default:
                return "未知状态";
        }
    }

    private String activityTypeLabel(Integer type)
    {
        if (type == null)
        {
            return "未知活动";
        }
        switch (type.intValue())
        {
            case 1:
                return "满减";
            case 2:
                return "折扣";
            case 3:
                return "赠品";
            case 4:
                return "包邮";
            case 5:
                return "限时特价";
            default:
                return "其他活动";
        }
    }

    private String formatDate(Date date)
    {
        if (date == null)
        {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private static class PeriodWindow
    {
        private final DashboardQueryDTO query;

        private final LocalDate startDate;

        private final LocalDate endDate;

        PeriodWindow(DashboardQueryDTO query, LocalDate startDate, LocalDate endDate)
        {
            this.query = query;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        DashboardQueryDTO getQuery()
        {
            return query;
        }

        LocalDate getStartDate()
        {
            return startDate;
        }

        LocalDate getEndDate()
        {
            return endDate;
        }
    }
}
