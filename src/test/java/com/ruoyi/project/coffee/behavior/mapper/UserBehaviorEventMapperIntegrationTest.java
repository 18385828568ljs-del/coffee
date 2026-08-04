package com.ruoyi.project.coffee.behavior.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ruoyi.project.coffee.behavior.service.UserBehaviorEventService;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_behavior_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/UserBehaviorEventMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.behavior.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.behavior.mapper")
@Import(UserBehaviorEventService.class)
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserBehaviorEventMapperIntegrationTest
{
    @Autowired
    private UserBehaviorEventService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void productViewsRemainRepeatableAndCartAddsAreIdempotent()
    {
        assertTrue(service.recordProductView(7L, UserBehaviorEventService.SCENE_MALL, 100L, 3L));
        assertTrue(service.recordProductView(7L, UserBehaviorEventService.SCENE_MALL, 100L, 3L));
        assertTrue(service.recordFirstCartAdd(7L, UserBehaviorEventService.SCENE_MALL, 100L, 3L, 55L));
        assertFalse(service.recordFirstCartAdd(7L, UserBehaviorEventService.SCENE_MALL, 100L, 3L, 55L));

        assertEquals(3, countEvents());
        assertEquals(1, countEventsByDedupKey("CART_ADD:MALL:55"));
    }

    private int countEvents()
    {
        return jdbcTemplate.queryForObject("select count(*) from t_user_behavior_event", Integer.class);
    }

    private int countEventsByDedupKey(String dedupKey)
    {
        return jdbcTemplate.queryForObject(
            "select count(*) from t_user_behavior_event where dedup_key = ?", Integer.class, dedupKey);
    }
}
