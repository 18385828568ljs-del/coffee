package com.ruoyi.project.coffee.order.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.order.domain.TOrder;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/TOrderMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.order.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.order.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TOrderMapperIntegrationTest
{
    @Autowired
    private TOrderMapper orderMapper;

    @Test
    void insertAndSelectShouldPersistMarketingAmounts()
    {
        TOrder order = buildOrder("MO202606170001", 0);

        assertEquals(1, orderMapper.insertTOrder(order));
        assertNotNull(order.getOrderId());

        TOrder saved = orderMapper.selectTOrderByOrderId(order.getOrderId());
        assertEquals("满减活动", saved.getActivitySummary());
        assertEquals(new BigDecimal("8.00"), saved.getDiscountAmount());
        assertEquals(new BigDecimal("6.00"), saved.getFreightAmount());
        assertEquals(new BigDecimal("48.00"), saved.getPayAmount());
    }

    @Test
    void updateOrderStatusShouldRequireOldStatusMatch()
    {
        TOrder order = buildOrder("MO202606170002", 0);
        orderMapper.insertTOrder(order);

        Date payTime = new Date();
        assertEquals(1, orderMapper.updateOrderStatus(
            order.getOrderId(),
            0,
            1,
            payTime,
            null,
            null,
            null,
            null,
            "已支付"
        ));
        assertEquals(0, orderMapper.updateOrderStatus(
            order.getOrderId(),
            0,
            2,
            null,
            new Date(),
            null,
            null,
            "SF123",
            null
        ));

        TOrder saved = orderMapper.selectTOrderByOrderId(order.getOrderId());
        assertEquals(Integer.valueOf(1), saved.getStatus());
        assertNotNull(saved.getPayTime());
        assertNotNull(saved.getUpdateTime());
        assertEquals("已支付", saved.getRemark());
    }

    @Test
    void countCompletedOrdersByUserIdShouldOnlyCountPaidFlowOrders()
    {
        orderMapper.insertTOrder(buildOrder("MO202606170003", 1));
        orderMapper.insertTOrder(buildOrder("MO202606170004", 2));
        orderMapper.insertTOrder(buildOrder("MO202606170005", 3));
        orderMapper.insertTOrder(buildOrder("MO202606170006", 0));
        orderMapper.insertTOrder(buildOrder("MO202606170007", 4));

        assertEquals(3, orderMapper.countCompletedOrdersByUserId(9101L));
    }

    @Test
    void selectTOrderListShouldFilterCurrentUserAndStatus()
    {
        orderMapper.insertTOrder(buildOrder("MO202606170008", 1));
        TOrder other = buildOrder("MO202606170009", 1);
        other.setUserId(9102L);
        orderMapper.insertTOrder(other);

        TOrder query = new TOrder();
        query.setUserId(9101L);
        query.setStatus(1);
        List<TOrder> orders = orderMapper.selectTOrderList(query);

        assertEquals(1, orders.size());
        assertEquals("MO202606170008", orders.get(0).getOrderNo());
    }

    @Test
    void paidOrderCannotBeHardDeleted()
    {
        TOrder paid = buildOrder("MO202606170010", 1);
        orderMapper.insertTOrder(paid);

        assertEquals(0, orderMapper.deleteTOrderByOrderId(paid.getOrderId()));
        assertNotNull(orderMapper.selectTOrderByOrderId(paid.getOrderId()));
    }

    @Test
    void existingOrderCannotBeReassignedToAnotherUser()
    {
        TOrder order = buildOrder("MO202606170011", 1);
        orderMapper.insertTOrder(order);

        TOrder change = new TOrder();
        change.setOrderId(order.getOrderId());
        change.setUserId(9102L);
        change.setPayAmount(new BigDecimal("49.00"));
        assertEquals(1, orderMapper.updateTOrder(change));

        TOrder saved = orderMapper.selectTOrderByOrderId(order.getOrderId());
        assertEquals(9101L, saved.getUserId());
        assertEquals(new BigDecimal("49.00"), saved.getPayAmount());
    }

    private TOrder buildOrder(String orderNo, int status)
    {
        TOrder order = new TOrder();
        order.setOrderNo(orderNo);
        order.setUserId(9101L);
        order.setTotalAmount(new BigDecimal("50.00"));
        order.setPayAmount(new BigDecimal("48.00"));
        order.setReceiverName("测试用户");
        order.setReceiverPhone("13800000000");
        order.setReceiverAddress("测试地址");
        order.setStatus(status);
        order.setPayType("WECHAT");
        order.setDiscountAmount(new BigDecimal("8.00"));
        order.setFreightAmount(new BigDecimal("6.00"));
        order.setActivitySummary("满减活动");
        order.setRemark("待支付");
        return order;
    }
}
