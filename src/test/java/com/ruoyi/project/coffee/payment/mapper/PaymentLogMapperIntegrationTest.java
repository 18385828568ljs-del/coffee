package com.ruoyi.project.coffee.payment.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.project.coffee.payment.domain.PaymentLog;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:payment_log_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/PaymentLogMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.payment.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.payment.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PaymentLogMapperIntegrationTest
{
    @Autowired
    private PaymentLogMapper paymentLogMapper;

    @Test
    void selectPaymentLogListShouldFilterByBusinessNoAndStatus()
    {
        paymentLogMapper.insertPaymentLog(log("SCAN_ORDER", "SO001", "PAID"));
        paymentLogMapper.insertPaymentLog(log("SCAN_ORDER", "SO002", "REFUND_FAILED"));
        paymentLogMapper.insertPaymentLog(log("MALL_ORDER", "MO001", "PAID"));

        PaymentLog query = new PaymentLog();
        query.setBusinessNo("SO");
        query.setStatus("PAID");
        List<PaymentLog> rows = paymentLogMapper.selectPaymentLogList(query);

        assertEquals(1, rows.size());
        assertEquals("SO001", rows.get(0).getBusinessNo());
        assertEquals("PAID", rows.get(0).getStatus());
    }

    private PaymentLog log(String businessType, String businessNo, String status)
    {
        PaymentLog log = new PaymentLog();
        log.setBusinessType(businessType);
        log.setBusinessNo(businessNo);
        log.setUserId(7L);
        log.setPayType("balance");
        log.setPayChannel("balance");
        log.setAmount(new BigDecimal("12.34"));
        log.setStatus(status);
        return log;
    }
}
