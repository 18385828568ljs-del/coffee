package com.ruoyi.project.coffee.wallet.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.wallet.domain.TRechargeTemplate;
import com.ruoyi.project.coffee.wallet.domain.TWallet;
import com.ruoyi.project.coffee.wallet.domain.TWalletLog;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/TWalletMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.wallet.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.wallet.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TWalletMapperIntegrationTest
{
    @Autowired
    private TWalletMapper walletMapper;

    @Test
    void addAndDeductBalanceShouldKeepBalanceAndConsumedConsistent()
    {
        TWallet wallet = createWallet(8001L, new BigDecimal("30.00"));
        assertEquals(1, walletMapper.insertWallet(wallet));
        assertNotNull(wallet.getWalletId());

        assertEquals(1, walletMapper.addBalance(8001L, new BigDecimal("20.00")));
        assertEquals(1, walletMapper.deductBalance(8001L, new BigDecimal("15.50")));
        assertEquals(0, walletMapper.deductBalance(8001L, new BigDecimal("100.00")));

        TWallet saved = walletMapper.selectWalletByUserId(8001L);
        assertEquals(new BigDecimal("34.50"), saved.getBalance());
        assertEquals(new BigDecimal("15.50"), saved.getTotalConsumed());
    }

    @Test
    void walletLogShouldReturnCurrentUserLogsInNewestOrder()
    {
        TWalletLog first = createWalletLog(8002L, "SO001", new BigDecimal("10.00"));
        TWalletLog second = createWalletLog(8002L, "SO002", new BigDecimal("12.00"));
        walletMapper.insertWalletLog(first);
        walletMapper.insertWalletLog(second);
        walletMapper.insertWalletLog(createWalletLog(8003L, "SO003", new BigDecimal("99.00")));

        List<TWalletLog> logs = walletMapper.selectWalletLogList(8002L);

        assertEquals(2, logs.size());
        assertEquals("SO002", logs.get(0).getRelatedOrderNo());
        assertEquals("SO001", logs.get(1).getRelatedOrderNo());
    }

    @Test
    void selectActiveTemplatesShouldOnlyReturnEnabledTemplatesBySortOrder()
    {
        TRechargeTemplate lowSort = createTemplate(new BigDecimal("100.00"), new BigDecimal("20.00"), 2, 1);
        TRechargeTemplate highSort = createTemplate(new BigDecimal("50.00"), new BigDecimal("5.00"), 1, 1);
        TRechargeTemplate disabled = createTemplate(new BigDecimal("200.00"), new BigDecimal("60.00"), 0, 0);
        walletMapper.insertTemplate(lowSort);
        walletMapper.insertTemplate(highSort);
        walletMapper.insertTemplate(disabled);

        List<TRechargeTemplate> templates = walletMapper.selectActiveTemplates();

        assertEquals(2, templates.size());
        assertEquals(new BigDecimal("50.00"), templates.get(0).getPayAmount());
        assertEquals(new BigDecimal("100.00"), templates.get(1).getPayAmount());
    }

    private TWallet createWallet(Long userId, BigDecimal balance)
    {
        TWallet wallet = new TWallet();
        wallet.setUserId(userId);
        wallet.setBalance(balance);
        wallet.setTotalRecharge(BigDecimal.ZERO.setScale(2));
        wallet.setTotalGift(BigDecimal.ZERO.setScale(2));
        wallet.setTotalConsumed(BigDecimal.ZERO.setScale(2));
        wallet.setFrozenAmount(BigDecimal.ZERO.setScale(2));
        return wallet;
    }

    private TWalletLog createWalletLog(Long userId, String orderNo, BigDecimal amount)
    {
        TWalletLog log = new TWalletLog();
        log.setUserId(userId);
        log.setType(2);
        log.setAmount(amount);
        log.setBalanceBefore(new BigDecimal("100.00"));
        log.setBalanceAfter(new BigDecimal("100.00").subtract(amount));
        log.setRelatedOrderNo(orderNo);
        log.setRemark("测试流水");
        return log;
    }

    private TRechargeTemplate createTemplate(BigDecimal payAmount, BigDecimal giftAmount, Integer sortOrder, Integer status)
    {
        TRechargeTemplate template = new TRechargeTemplate();
        template.setPayAmount(payAmount);
        template.setGiftAmount(giftAmount);
        template.setTotalAmount(payAmount.add(giftAmount));
        template.setSortOrder(sortOrder);
        template.setStatus(status);
        return template;
    }
}
