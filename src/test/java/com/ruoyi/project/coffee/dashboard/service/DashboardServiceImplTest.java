package com.ruoyi.project.coffee.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import com.ruoyi.project.coffee.dashboard.domain.ActivityWarningVO;
import com.ruoyi.project.coffee.dashboard.domain.DashboardOverviewVO;
import com.ruoyi.project.coffee.dashboard.domain.DashboardQueryDTO;
import com.ruoyi.project.coffee.dashboard.domain.DiscountOrderSummaryVO;
import com.ruoyi.project.coffee.dashboard.domain.PieItemVO;
import com.ruoyi.project.coffee.dashboard.domain.RankItemVO;
import com.ruoyi.project.coffee.dashboard.domain.StatusCountVO;
import com.ruoyi.project.coffee.dashboard.domain.StockWarningVO;
import com.ruoyi.project.coffee.dashboard.domain.TrendPointVO;
import com.ruoyi.project.coffee.dashboard.mapper.DashboardMapper;
import com.ruoyi.project.coffee.dashboard.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class DashboardServiceImplTest
{
    private DashboardServiceImpl dashboardService;

    @Mock
    private DashboardMapper dashboardMapper;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        dashboardService = new DashboardServiceImpl();
        ReflectionTestUtils.setField(dashboardService, "dashboardMapper", dashboardMapper);
    }

    @Test
    void getOverviewShouldDefaultToScanAndFillMissingTrendDates()
    {
        when(dashboardMapper.selectScanSalesTrend(any())).thenReturn(Arrays.asList(
            amountPoint("2026-06-02", "12.345")
        ));
        when(dashboardMapper.selectUserTrend(any())).thenReturn(Arrays.asList(
            countPoint("2026-06-03", 2L)
        ));
        when(dashboardMapper.selectScanOrderStatusDistribution(any())).thenReturn(Arrays.asList(
            statusCount(2, 5L)
        ));
        when(dashboardMapper.selectScanTopProducts(any())).thenReturn(Arrays.asList(
            rank(null, null, null, null)
        ));
        when(dashboardMapper.selectScanCategorySalesShare(any())).thenReturn(Arrays.asList(
            pie(null, null, null)
        ));
        when(dashboardMapper.selectLowStockProducts(any())).thenReturn(Arrays.asList(
            stock(9L, null, null, "p.png")
        ));
        when(dashboardMapper.selectEndingActivities(any())).thenReturn(Arrays.asList(
            activity(3L, "", 1, date("2026-06-04 10:00:00"), 2L)
        ));

        DashboardOverviewVO overview = dashboardService.getOverview("", "custom", "2026-06-01", "2026-06-03");

        ArgumentCaptor<DashboardQueryDTO> queryCaptor = ArgumentCaptor.forClass(DashboardQueryDTO.class);
        verify(dashboardMapper).selectScanSalesTrend(queryCaptor.capture());
        DashboardQueryDTO query = queryCaptor.getValue();
        assertEquals("scan", query.getBusinessType());
        assertEquals("custom", query.getPeriod());
        assertEquals("2026-06-01 至 2026-06-03", query.getPeriodLabel());
        assertEquals(Integer.valueOf(10), query.getLowStockThreshold());
        assertEquals(Integer.valueOf(2), query.getHighUrgeThreshold());

        assertEquals("2026-06-01 至 2026-06-03", overview.getPeriodLabel());
        assertEquals(3, overview.getSalesTrend().size());
        assertEquals(new BigDecimal("0.00"), overview.getSalesTrend().get(0).getAmount());
        assertEquals(new BigDecimal("12.35"), overview.getSalesTrend().get(1).getAmount());
        assertEquals(new BigDecimal("0.00"), overview.getSalesTrend().get(2).getAmount());
        assertEquals(2L, overview.getUserTrend().get(2).getCount());
        assertEquals("制作中", overview.getOrderStatusDistribution().get(2).getName());
        assertEquals(5L, overview.getOrderStatusDistribution().get(2).getCount());
        assertEquals("未知商品", overview.getTopProducts().get(0).getName());
        assertEquals("未分类", overview.getCategorySalesShare().get(0).getName());
        assertEquals("未命名商品", overview.getLowStockProducts().get(0).getProductName());
        assertEquals("未命名活动", overview.getEndingActivities().get(0).getTitle());
        assertEquals("满减", overview.getEndingActivities().get(0).getTypeLabel());
    }

    @Test
    void getOverviewShouldBuildMallKpisMarketingAndStatusLabels()
    {
        when(dashboardMapper.sumSalesAmount(any())).thenReturn(new BigDecimal("100.00"));
        when(dashboardMapper.countOrders(any())).thenReturn(5L);
        when(dashboardMapper.countPaidOrders(any())).thenReturn(4L);
        when(dashboardMapper.countNewUsers(any())).thenReturn(2L);
        when(dashboardMapper.countPendingShipment()).thenReturn(3L);
        when(dashboardMapper.countLowStockProducts(any())).thenReturn(1L);
        when(dashboardMapper.countEndingActivities(any())).thenReturn(1L);
        when(dashboardMapper.countActiveActivities(any())).thenReturn(6L);
        when(dashboardMapper.selectDiscountOrderSummary(any())).thenReturn(discountSummary(2L, "6.789"));
        when(dashboardMapper.selectOrderStatusDistribution(any())).thenReturn(Arrays.asList(
            statusCount(1, 3L)
        ));

        DashboardOverviewVO overview = dashboardService.getOverview(" mall ", "custom", "2026-06-02", "2026-06-02");

        assertEquals(new BigDecimal("100.00"), overview.getKpis().getSalesAmount());
        assertEquals(5L, overview.getKpis().getOrderCount());
        assertEquals(4L, overview.getKpis().getPaidOrderCount());
        assertEquals(new BigDecimal("25.00"), overview.getKpis().getAverageOrderValue());
        assertEquals(2L, overview.getKpis().getNewUserCount());
        assertEquals(3L, overview.getKpis().getPendingShipmentCount());
        assertEquals(1L, overview.getAlerts().getLowStockCount());
        assertEquals(1L, overview.getAlerts().getEndingActivityCount());
        assertEquals(6L, overview.getMarketingSummary().getActiveActivityCount());
        assertEquals(2L, overview.getMarketingSummary().getDiscountedOrderCount());
        assertEquals(new BigDecimal("6.79"), overview.getMarketingSummary().getDiscountAmount());
        assertEquals("待发货", overview.getOrderStatusDistribution().get(1).getName());
        assertEquals(3L, overview.getOrderStatusDistribution().get(1).getCount());
    }

    @Test
    void getOverviewShouldRejectInvalidCustomDateRangeBeforeQueryingMapper()
    {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> dashboardService.getOverview("scan", "custom", "2026-06-05", "2026-06-01")
        );

        assertEquals("开始日期不能晚于结束日期", exception.getMessage());
        verify(dashboardMapper, never()).sumScanSalesAmount(any());
        verify(dashboardMapper, never()).selectScanSalesTrend(any());
    }

    private TrendPointVO amountPoint(String dateLabel, String amount)
    {
        TrendPointVO point = new TrendPointVO();
        point.setDateLabel(dateLabel);
        point.setAmount(new BigDecimal(amount));
        return point;
    }

    private TrendPointVO countPoint(String dateLabel, Long count)
    {
        TrendPointVO point = new TrendPointVO();
        point.setDateLabel(dateLabel);
        point.setCount(count);
        return point;
    }

    private StatusCountVO statusCount(Integer status, Long count)
    {
        StatusCountVO row = new StatusCountVO();
        row.setStatus(status);
        row.setCount(count);
        return row;
    }

    private RankItemVO rank(Long productId, String name, String amount, Long quantity)
    {
        RankItemVO row = new RankItemVO();
        row.setProductId(productId);
        row.setName(name);
        row.setAmount(amount == null ? null : new BigDecimal(amount));
        row.setQuantity(quantity);
        return row;
    }

    private PieItemVO pie(String name, String value, Long count)
    {
        PieItemVO row = new PieItemVO();
        row.setName(name);
        row.setValue(value == null ? null : new BigDecimal(value));
        row.setCount(count);
        return row;
    }

    private StockWarningVO stock(Long productId, String productName, Long stock, String imageUrl)
    {
        StockWarningVO row = new StockWarningVO();
        row.setProductId(productId);
        row.setProductName(productName);
        row.setStock(stock);
        row.setImageUrl(imageUrl);
        return row;
    }

    private ActivityWarningVO activity(Long activityId, String title, Integer type, Date endTime, Long remainingDays)
    {
        ActivityWarningVO row = new ActivityWarningVO();
        row.setActivityId(activityId);
        row.setTitle(title);
        row.setType(type);
        row.setEndTime(endTime);
        row.setRemainingDays(remainingDays);
        return row;
    }

    private DiscountOrderSummaryVO discountSummary(Long orderCount, String discountAmount)
    {
        DiscountOrderSummaryVO row = new DiscountOrderSummaryVO();
        row.setOrderCount(orderCount);
        row.setDiscountAmount(new BigDecimal(discountAmount));
        return row;
    }

    private Date date(String value)
    {
        try
        {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value);
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
}
