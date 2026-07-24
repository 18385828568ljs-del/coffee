package com.ruoyi.project.coffee.activity.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.activity.domain.TMarketingActivity;
import com.ruoyi.project.coffee.activity.domain.TMarketingActivityScope;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/TMarketingActivityMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.activity.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.activity.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TMarketingActivityMapperIntegrationTest
{
    @Autowired
    private TMarketingActivityMapper marketingActivityMapper;

    @Test
    void insertAndSelectShouldPersistActivityRuleFields()
    {
        TMarketingActivity activity = buildActivity("满 50 减 8", "mall", 1);

        assertEquals(1, marketingActivityMapper.insertTMarketingActivity(activity));
        assertNotNull(activity.getActivityId());

        TMarketingActivity saved = marketingActivityMapper.selectTMarketingActivityByActivityId(activity.getActivityId());
        assertEquals("满 50 减 8", saved.getTitle());
        assertEquals(new BigDecimal("50.00"), saved.getConditionMinAmount());
        assertEquals(new BigDecimal("8.00"), saved.getEffectValue());
        assertEquals("reduce", saved.getEffectMode());
    }

    @Test
    void selectTMarketingActivityListShouldFilterTargetTypeAndStatus()
    {
        marketingActivityMapper.insertTMarketingActivity(buildActivity("商城活动", "mall", 1));
        marketingActivityMapper.insertTMarketingActivity(buildActivity("点单活动", "scan", 1));
        marketingActivityMapper.insertTMarketingActivity(buildActivity("停用活动", "mall", 0));

        TMarketingActivity query = new TMarketingActivity();
        query.setTargetType("mall");
        query.setStatus(1);
        List<TMarketingActivity> activities = marketingActivityMapper.selectTMarketingActivityList(query);

        assertEquals(1, activities.size());
        assertEquals("商城活动", activities.get(0).getTitle());
    }

    @Test
    void batchInsertScopesShouldRoundTripAndDeleteByActivityId()
    {
        TMarketingActivity activity = buildActivity("指定商品活动", "both", 1);
        marketingActivityMapper.insertTMarketingActivity(activity);

        TMarketingActivityScope productScope = buildScope(activity.getActivityId(), 2, 1001L);
        TMarketingActivityScope categoryScope = buildScope(activity.getActivityId(), 1, 2001L);

        assertEquals(2, marketingActivityMapper.batchInsertTMarketingActivityScopes(Arrays.asList(productScope, categoryScope)));

        List<TMarketingActivityScope> scopes = marketingActivityMapper.selectTMarketingActivityScopesByActivityIds(
            Arrays.asList(activity.getActivityId())
        );
        assertEquals(2, scopes.size());
        assertEquals(Long.valueOf(1001L), scopes.get(0).getScopeTargetId());
        assertEquals(Long.valueOf(2001L), scopes.get(1).getScopeTargetId());

        assertEquals(2, marketingActivityMapper.deleteTMarketingActivityScopeByActivityId(activity.getActivityId()));
        assertEquals(0, marketingActivityMapper.selectTMarketingActivityScopesByActivityIds(
            Arrays.asList(activity.getActivityId())
        ).size());
    }

    private TMarketingActivity buildActivity(String title, String targetType, int status)
    {
        TMarketingActivity activity = new TMarketingActivity();
        activity.setTitle(title);
        activity.setType(1);
        activity.setTargetType(targetType);
        activity.setScopeType(0);
        activity.setConditionMinAmount(new BigDecimal("50.00"));
        activity.setConditionMinQuantity(2L);
        activity.setConditionNewUserOnly(0);
        activity.setEffectMode("reduce");
        activity.setEffectValue(new BigDecimal("8.00"));
        activity.setGiftProductId(1001L);
        activity.setGiftQuantity(1L);
        activity.setShippingBaseFreight(new BigDecimal("6.00"));
        activity.setStatus(status);
        activity.setStartTime(new Date());
        activity.setEndTime(new Date(System.currentTimeMillis() + 86400000L));
        activity.setCreateBy("test");
        activity.setRemark("测试活动");
        return activity;
    }

    private TMarketingActivityScope buildScope(Long activityId, Integer scopeType, Long scopeTargetId)
    {
        TMarketingActivityScope scope = new TMarketingActivityScope();
        scope.setActivityId(activityId);
        scope.setScopeType(scopeType);
        scope.setScopeTargetId(scopeTargetId);
        return scope;
    }
}
