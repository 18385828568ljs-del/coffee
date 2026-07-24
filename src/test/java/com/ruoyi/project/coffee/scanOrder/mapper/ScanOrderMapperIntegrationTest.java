package com.ruoyi.project.coffee.scanOrder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/ScanOrderMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.scanOrder.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.scanOrder.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ScanOrderMapperIntegrationTest
{
    @Autowired
    private ScanOrderMapper scanOrderMapper;

    @Test
    void insertAndSelectShouldPersistCoreFields()
    {
        ScanOrder order = buildOrder("SO202606170001", 0);

        assertEquals(1, scanOrderMapper.insertScanOrder(order));
        assertNotNull(order.getOrderId());

        ScanOrder saved = scanOrderMapper.selectScanOrderByOrderNo("SO202606170001");
        assertEquals(Long.valueOf(order.getOrderId()), saved.getOrderId());
        assertEquals("堂食", saved.getRemark());
        assertEquals(new BigDecimal("36.00"), saved.getPayAmount());
    }

    @Test
    void updateScanOrderStatusShouldRequireOldStatusMatch()
    {
        ScanOrder order = buildOrder("SO202606170002", 0);
        scanOrderMapper.insertScanOrder(order);

        Date payTime = new Date();
        int updated = scanOrderMapper.updateScanOrderStatus(
            order.getOrderId(),
            0,
            2,
            payTime,
            null,
            null,
            null,
            null,
            null,
            "018",
            12,
            "WECHAT"
        );
        int staleUpdate = scanOrderMapper.updateScanOrderStatus(
            order.getOrderId(),
            0,
            3,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        ScanOrder saved = scanOrderMapper.selectScanOrderById(order.getOrderId());
        assertEquals(1, updated);
        assertEquals(0, staleUpdate);
        assertEquals(Integer.valueOf(2), saved.getStatus());
        assertEquals("018", saved.getPickupNo());
        assertEquals(Integer.valueOf(12), saved.getEstimatedWaitMinutes());
        assertNotNull(saved.getPayTime());
    }

    @Test
    void markUrgedShouldOnlyWorkForPaidProcessingOrders()
    {
        ScanOrder processing = buildOrder("SO202606170003", 2);
        scanOrderMapper.insertScanOrder(processing);

        ScanOrder unpaid = buildOrder("SO202606170004", 0);
        scanOrderMapper.insertScanOrder(unpaid);

        Date urgeTime = new Date();
        int urged = scanOrderMapper.markUrged(processing.getOrderId(), urgeTime);
        int ignored = scanOrderMapper.markUrged(unpaid.getOrderId(), urgeTime);

        ScanOrder urgedOrder = scanOrderMapper.selectScanOrderById(processing.getOrderId());
        ScanOrder unpaidOrder = scanOrderMapper.selectScanOrderById(unpaid.getOrderId());

        assertEquals(1, urged);
        assertEquals(0, ignored);
        assertEquals(Integer.valueOf(1), urgedOrder.getUrgeCount());
        assertNotNull(urgedOrder.getLastUrgeTime());
        assertEquals(null, unpaidOrder.getUrgeCount());
    }

    @Test
    void selectScanOrderListShouldOrderNewestFirst()
    {
        ScanOrder oldMaking = buildOrder("SO202606170005", 2);
        oldMaking.setCreateTime(Date.from(Instant.parse("2026-06-17T10:00:00Z")));
        scanOrderMapper.insertScanOrder(oldMaking);

        ScanOrder newestCompleted = buildOrder("SO202606170006", 4);
        newestCompleted.setCreateTime(Date.from(Instant.parse("2026-06-18T10:00:00Z")));
        scanOrderMapper.insertScanOrder(newestCompleted);

        List<ScanOrder> list = scanOrderMapper.selectScanOrderList(new ScanOrder());

        assertEquals("SO202606170006", list.get(0).getOrderNo());
        assertEquals("SO202606170005", list.get(1).getOrderNo());
    }

    private ScanOrder buildOrder(String orderNo, int status)
    {
        ScanOrder order = new ScanOrder();
        order.setOrderNo(orderNo);
        order.setUserId(66L);
        order.setOpenid("openid-test");
        order.setShopId(1L);
        order.setShopName("阿布咖啡");
        order.setTableNo("A08");
        order.setScene("dine_in");
        order.setTotalAmount(new BigDecimal("40.00"));
        order.setPayAmount(new BigDecimal("36.00"));
        order.setDiscountAmount(new BigDecimal("4.00"));
        order.setMemberDiscount(new BigDecimal("1.00"));
        order.setActivitySummary("满减活动");
        order.setStatus(status);
        order.setRemark("堂食");
        order.setCreateBy("test");
        order.setUpdateBy("test");
        return order;
    }
}
