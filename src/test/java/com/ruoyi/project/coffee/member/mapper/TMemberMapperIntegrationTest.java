package com.ruoyi.project.coffee.member.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.member.domain.TMember;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/TMemberMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.member.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.member.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TMemberMapperIntegrationTest
{
    @Autowired
    private TMemberMapper memberMapper;

    @Test
    void insertAndAtomicAddSpendingShouldPersistMemberSpending()
    {
        TMember member = new TMember();
        member.setUserId(7001L);
        member.setLevel(1);
        member.setLevelName("普通会员");
        member.setDiscountRate(new BigDecimal("1.00"));
        member.setTotalSpending(new BigDecimal("20.00"));

        assertEquals(1, memberMapper.insertMember(member));
        assertNotNull(member.getMemberId());
        assertEquals(1, memberMapper.atomicAddSpending(7001L, new BigDecimal("35.50")));

        TMember saved = memberMapper.selectMemberByUserId(7001L);
        assertEquals(new BigDecimal("55.50"), saved.getTotalSpending());
    }

    @Test
    void updateMemberShouldOnlyChangeProvidedFields()
    {
        TMember member = new TMember();
        member.setUserId(7002L);
        member.setLevel(1);
        member.setLevelName("普通会员");
        member.setDiscountRate(new BigDecimal("1.00"));
        member.setTotalSpending(new BigDecimal("200.00"));
        memberMapper.insertMember(member);

        TMember update = new TMember();
        update.setUserId(7002L);
        update.setLevel(2);
        update.setLevelName("银卡会员");
        update.setDiscountRate(new BigDecimal("0.95"));

        assertEquals(1, memberMapper.updateMember(update));

        TMember saved = memberMapper.selectMemberByUserId(7002L);
        assertEquals(Integer.valueOf(2), saved.getLevel());
        assertEquals("银卡会员", saved.getLevelName());
        assertEquals(new BigDecimal("0.95"), saved.getDiscountRate());
        assertEquals(new BigDecimal("200.00"), saved.getTotalSpending());
    }
}
