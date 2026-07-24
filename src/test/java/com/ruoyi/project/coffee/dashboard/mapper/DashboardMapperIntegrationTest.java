package com.ruoyi.project.coffee.dashboard.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import com.ruoyi.project.coffee.dashboard.domain.DashboardQueryDTO;
import com.ruoyi.project.coffee.dashboard.domain.PieItemVO;
import com.ruoyi.project.coffee.dashboard.domain.RankItemVO;
import com.ruoyi.project.coffee.dashboard.domain.StockWarningVO;
import com.ruoyi.project.coffee.dashboard.domain.TrendPointVO;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/DashboardMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.dashboard.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.dashboard.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DashboardMapperIntegrationTest
{
    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void mallStatisticsShouldOnlyCountPaidOrdersInsideWindow()
    {
        insertMallCategory(10L, "咖啡豆");
        insertProduct(100L, "精品咖啡豆", 10L, 8L, 1);
        insertProduct(101L, "下架商品", 10L, 2L, 0);
        insertMallOrder(1L, "MO1", 66L, 1, "2026-06-02 10:00:00", "2026-06-02 10:30:00",
            "20.00", "18.00", "2.00", "满减");
        insertMallOrder(2L, "MO2", 66L, 3, "2026-06-03 10:00:00", "2026-06-03 10:30:00",
            "40.00", "36.00", "4.00", "");
        insertMallOrder(3L, "MO3", 66L, 0, "2026-06-03 11:00:00", null,
            "50.00", "50.00", "0.00", "");
        insertMallOrder(4L, "MO4", 66L, 1, "2026-05-30 10:00:00", "2026-05-30 10:30:00",
            "90.00", "90.00", "0.00", "");
        insertMallOrderItem(1L, 1L, 100L, "精品咖啡豆", "18.00", 2L);
        insertMallOrderItem(2L, 2L, 100L, "精品咖啡豆", "36.00", 3L);
        insertWxUser(66L, "2026-06-02 08:00:00");
        insertWxUser(67L, "2026-05-29 08:00:00");

        DashboardQueryDTO query = query("2026-06-01 00:00:00", "2026-06-04 00:00:00");

        assertEquals(new BigDecimal("54.00"), dashboardMapper.sumSalesAmount(query));
        assertEquals(3L, dashboardMapper.countOrders(query));
        assertEquals(2L, dashboardMapper.countPaidOrders(query));
        assertEquals(1L, dashboardMapper.countNewUsers(query));
        assertEquals(1L, dashboardMapper.countLowStockProducts(query));
        assertEquals(2L, dashboardMapper.selectDiscountOrderSummary(query).getOrderCount());
        assertEquals(new BigDecimal("6.00"), dashboardMapper.selectDiscountOrderSummary(query).getDiscountAmount());

        List<TrendPointVO> trend = dashboardMapper.selectSalesTrend(query);
        assertEquals(2, trend.size());
        assertEquals("2026-06-02", trend.get(0).getDateLabel());
        assertEquals(new BigDecimal("18.00"), trend.get(0).getAmount());

        List<RankItemVO> topProducts = dashboardMapper.selectTopProducts(query);
        assertEquals(1, topProducts.size());
        assertEquals(Long.valueOf(100L), topProducts.get(0).getProductId());
        assertEquals(new BigDecimal("54.00"), topProducts.get(0).getAmount());
        assertEquals(5L, topProducts.get(0).getQuantity());

        List<PieItemVO> categoryShare = dashboardMapper.selectCategorySalesShare(query);
        assertEquals(1, categoryShare.size());
        assertEquals("咖啡豆", categoryShare.get(0).getName());
        assertEquals(new BigDecimal("54.00"), categoryShare.get(0).getValue());
    }

    @Test
    void scanStatisticsShouldCountDineInOrdersItemsAndWaitMinutes()
    {
        insertScanCategory(20L, "手冲");
        insertScanProduct(200L, "手冲咖啡", 20L);
        insertScanOrder(11L, "SO1", 1, "2026-06-02 09:00:00", "2026-06-02 09:05:00",
            "2026-06-02 09:20:00", "24.00", 2);
        insertScanOrder(12L, "SO2", 4, "2026-06-02 10:00:00", "2026-06-02 10:05:00",
            "2026-06-02 10:25:00", "36.00", 3);
        insertScanOrder(13L, "SO3", 0, "2026-06-02 11:00:00", null, null, "99.00", 0);
        insertScanOrderItem(21L, 11L, 200L, "手冲咖啡", "24.00", 2);
        insertScanOrderItem(22L, 12L, 200L, "手冲咖啡", "36.00", 3);

        DashboardQueryDTO query = query("2026-06-01 00:00:00", "2026-06-04 00:00:00");

        assertEquals(new BigDecimal("60.00"), dashboardMapper.sumScanSalesAmount(query));
        assertEquals(3L, dashboardMapper.countScanOrders(query));
        assertEquals(2L, dashboardMapper.countScanPaidOrders(query));
        assertEquals(1L, dashboardMapper.countScanPendingAccept());
        assertEquals(1L, dashboardMapper.countScanHighUrge(query));
        assertEquals(new BigDecimal("20"), dashboardMapper.avgScanWaitMinutes(query));
        assertEquals(5L, dashboardMapper.sumScanUrgeCount(query));

        List<RankItemVO> topProducts = dashboardMapper.selectScanTopProducts(query);
        assertEquals(1, topProducts.size());
        assertEquals(Long.valueOf(200L), topProducts.get(0).getProductId());
        assertEquals(new BigDecimal("60.00"), topProducts.get(0).getAmount());
        assertEquals(5L, topProducts.get(0).getQuantity());

        List<PieItemVO> categoryShare = dashboardMapper.selectScanCategorySalesShare(query);
        assertEquals(1, categoryShare.size());
        assertEquals("手冲", categoryShare.get(0).getName());
        assertEquals(new BigDecimal("60.00"), categoryShare.get(0).getValue());
    }

    @Test
    void warningsShouldReturnLowStockAndEndingActivitiesInOrder()
    {
        insertProduct(301L, "库存最低", null, 1L, 1);
        insertProduct(302L, "库存较低", null, 5L, 1);
        insertProduct(303L, "库存正常", null, 20L, 1);
        insertMarketingActivity(501L, "即将结束满减", 1, 1, "2026-06-02 10:00:00", "2026-06-03 10:00:00");
        insertMarketingActivity(502L, "未启用活动", 1, 0, "2026-06-02 10:00:00", "2026-06-03 09:00:00");
        insertMarketingActivity(503L, "稍后结束折扣", 2, 1, "2026-06-02 10:00:00", "2026-06-04 10:00:00");

        DashboardQueryDTO query = query("2026-06-01 00:00:00", "2026-06-04 00:00:00");
        query.setLowStockThreshold(10);
        query.setNowTime(date("2026-06-02 12:00:00"));
        query.setEndingSoonTime(date("2026-06-05 00:00:00"));

        List<StockWarningVO> lowStockProducts = dashboardMapper.selectLowStockProducts(query);
        assertEquals(2, lowStockProducts.size());
        assertEquals(Long.valueOf(301L), lowStockProducts.get(0).getProductId());
        assertEquals("库存最低", lowStockProducts.get(0).getProductName());

        assertEquals(2L, dashboardMapper.countEndingActivities(query));
        assertEquals(2L, dashboardMapper.countActiveActivities(query));
    }

    private DashboardQueryDTO query(String start, String end)
    {
        DashboardQueryDTO query = new DashboardQueryDTO();
        query.setStartTime(date(start));
        query.setEndTimeExclusive(date(end));
        query.setNowTime(date("2026-06-02 12:00:00"));
        query.setEndingSoonTime(date("2026-06-05 00:00:00"));
        query.setLowStockThreshold(10);
        query.setHighUrgeThreshold(2);
        return query;
    }

    private void insertMallCategory(Long categoryId, String categoryName)
    {
        jdbcTemplate.update("insert into t_category (category_id, category_name, sort_order) values (?, ?, ?)",
            categoryId, categoryName, 1);
    }

    private void insertProduct(Long productId, String productName, Long categoryId, Long stock, int status)
    {
        jdbcTemplate.update(
            "insert into t_product (product_id, product_name, image_url, price, status, stock, category_id) values (?, ?, ?, ?, ?, ?, ?)",
            productId, productName, "p.png", new BigDecimal("10.00"), status, stock, categoryId
        );
    }

    private void insertMallOrder(Long orderId, String orderNo, Long userId, int status, String createTime, String payTime,
        String totalAmount, String payAmount, String discountAmount, String activitySummary)
    {
        jdbcTemplate.update(
            "insert into t_order (order_id, order_no, user_id, total_amount, pay_amount, status, create_time, pay_time, discount_amount, activity_summary) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            orderId, orderNo, userId, new BigDecimal(totalAmount), new BigDecimal(payAmount), status,
            timestamp(createTime), timestampOrNull(payTime), new BigDecimal(discountAmount), activitySummary
        );
    }

    private void insertMallOrderItem(Long itemId, Long orderId, Long productId, String productName, String totalPrice, Long quantity)
    {
        jdbcTemplate.update(
            "insert into t_order_item (item_id, order_id, product_id, product_name, total_price, quantity) values (?, ?, ?, ?, ?, ?)",
            itemId, orderId, productId, productName, new BigDecimal(totalPrice), quantity
        );
    }

    private void insertWxUser(Long userId, String createTime)
    {
        jdbcTemplate.update("insert into t_wxuser (id, openid, nickname, avatar, create_time) values (?, ?, ?, ?, ?)",
            userId, "openid-" + userId, "用户" + userId, "avatar.png", timestamp(createTime));
    }

    private void insertScanCategory(Long categoryId, String categoryName)
    {
        jdbcTemplate.update("insert into t_scan_category (category_id, category_name, sort_order, status) values (?, ?, ?, ?)",
            categoryId, categoryName, 1, 1);
    }

    private void insertScanProduct(Long productId, String productName, Long categoryId)
    {
        jdbcTemplate.update(
            "insert into t_scan_product (product_id, category_id, product_name, price, status, sort_order) values (?, ?, ?, ?, ?, ?)",
            productId, categoryId, productName, new BigDecimal("12.00"), 1, 1
        );
    }

    private void insertScanOrder(Long orderId, String orderNo, int status, String createTime, String payTime,
        String finishTime, String payAmount, int urgeCount)
    {
        jdbcTemplate.update(
            "insert into t_scan_order (order_id, order_no, user_id, total_amount, pay_amount, status, create_time, pay_time, finish_time, urge_count) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            orderId, orderNo, 77L, new BigDecimal(payAmount), new BigDecimal(payAmount), status,
            timestamp(createTime), timestampOrNull(payTime), timestampOrNull(finishTime), urgeCount
        );
    }

    private void insertScanOrderItem(Long itemId, Long orderId, Long productId, String productName, String totalPrice, int quantity)
    {
        jdbcTemplate.update(
            "insert into t_scan_order_item (item_id, order_id, product_id, product_name, total_price, quantity) values (?, ?, ?, ?, ?, ?)",
            itemId, orderId, productId, productName, new BigDecimal(totalPrice), quantity
        );
    }

    private void insertMarketingActivity(Long activityId, String title, int type, int status, String startTime, String endTime)
    {
        jdbcTemplate.update(
            "insert into t_marketing_activity (activity_id, title, type, status, start_time, end_time) values (?, ?, ?, ?, ?, ?)",
            activityId, title, type, status, timestamp(startTime), timestamp(endTime)
        );
    }

    private Date date(String value)
    {
        return Date.from(LocalDateTime.parse(value.replace(" ", "T")).atZone(ZoneId.systemDefault()).toInstant());
    }

    private Timestamp timestamp(String value)
    {
        return Timestamp.valueOf(LocalDateTime.parse(value.replace(" ", "T")));
    }

    private Timestamp timestampOrNull(String value)
    {
        return value == null ? null : timestamp(value);
    }
}
