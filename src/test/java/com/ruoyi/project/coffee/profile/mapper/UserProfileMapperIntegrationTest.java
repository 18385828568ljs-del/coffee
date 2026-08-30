package com.ruoyi.project.coffee.profile.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.profile.domain.ProfileEvidence;
import com.ruoyi.project.coffee.profile.domain.UserProfile;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_profile_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/UserProfileMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.profile.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.profile.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserProfileMapperIntegrationTest
{
    private static final Instant NOW = Instant.parse("2026-08-07T03:00:00Z");

    @Autowired
    private UserProfileMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareEvidence()
    {
        jdbcTemplate.update("insert into t_wxuser(id, openid, nickname) values (7, 'openid-7', '顾客')");
        jdbcTemplate.update("insert into t_category(category_id, category_name) values (11, '奶咖')");
        jdbcTemplate.update("insert into t_product(product_id, product_name, price, status, stock, category_id) "
            + "values (101, '拿铁', 25.00, 1, 99, 11)");
        jdbcTemplate.update("insert into t_scan_category(category_id, category_name) values (21, '现制咖啡')");
        jdbcTemplate.update("insert into t_scan_product(product_id, product_name, price, status, category_id) "
            + "values (201, '澳白', 30.00, 1, 21)");

        insertMallOrder(1001L, 1, 0, "25.00", dateDaysAgo(10));
        insertMallItem(1001L, 101L, "拿铁", "25.00", null);
        insertMallItem(1001L, 101L, "赠品拿铁", "0.00", "赠品:活动");
        insertMallOrder(1002L, 3, 2, "99.00", dateDaysAgo(8));
        insertMallItem(1002L, 101L, "已退款拿铁", "99.00", null);
        insertMallOrder(1003L, 4, 0, "88.00", dateDaysAgo(7));
        insertMallOrder(1004L, 1, 0, "200.00", dateDaysAgo(181));
        insertMallItem(1004L, 101L, "旧订单拿铁", "200.00", null);
        insertMallItem(1003L, 101L, "已取消拿铁", "88.00", null);

        jdbcTemplate.update("insert into t_scan_order(order_id, order_no, user_id, total_amount, pay_amount, "
                + "status, refund_status, pay_time, create_time, update_time) values "
                + "(2001, 'SO-1', 7, 30.00, 30.00, 4, 0, ?, ?, ?)",
            dateDaysAgo(5), dateDaysAgo(5), dateDaysAgo(5));
        jdbcTemplate.update("insert into t_scan_order_item(order_id, product_id, product_name, price, quantity) "
            + "values (2001, 201, '澳白', 30.00, 1)");

        jdbcTemplate.update("insert into t_user_behavior_event(user_id, event_type, scene, product_id, "
                + "category_id, dedup_key, event_time) values (7, 'PRODUCT_VIEW', 'MALL', 101, 11, 'view-1', ?)",
            dateDaysAgo(2));
        jdbcTemplate.update("insert into t_user_behavior_event(user_id, event_type, scene, product_id, "
                + "category_id, dedup_key, event_time) values (7, 'CART_REMOVE', 'MALL', 101, 11, 'remove-1', ?)",
            dateDaysAgo(1));
    }

    @Test
    void queriesOnlyEffectiveOrdersAndPositiveRecommendationEvidence()
    {
        List<ProfileEvidence> purchases = mapper.selectPurchaseEvidence(7L);
        List<ProfileEvidence> behaviors = mapper.selectBehaviorEvidence(7L);

        assertEquals(3, purchases.size());
        assertEquals(1, behaviors.size());
        assertEquals("PRODUCT_VIEW", behaviors.get(0).getEvidenceType());
        assertEquals(7L, mapper.selectChangedUserIds().get(0));

    }

    @Test
    void upsertUpdatesSnapshotWithoutDuplicatingUserRow()
    {
        UserProfile first = profile("LEARNING", dateDaysAgo(1));
        UserProfile second = profile("READY", new Date());

        assertEquals(1, mapper.upsertUserProfile(first));
        mapper.upsertUserProfile(second);

        UserProfile saved = mapper.selectUserProfileByUserId(7L);
        assertNotNull(saved);
        assertEquals("READY", saved.getProfileStatus());
        assertEquals(1, jdbcTemplate.queryForObject(
            "select count(*) from t_user_profile where user_id = 7", Integer.class));
    }

    @Test
    void historicalBehaviorRemainsRecommendationEvidence()
    {
        jdbcTemplate.update("insert into t_user_behavior_event(user_id, event_type, scene, product_id, "
                + "category_id, dedup_key, event_time) "
                + "values (7, 'PRODUCT_VIEW', 'MALL', 101, 11, 'old-view', ?)", dateDaysAgo(365));

        assertEquals(2, mapper.selectBehaviorEvidence(7L).size());
    }

    @Test
    void lateCommittedEvidenceWithinOverlapWindowTriggersExistingProfileAgain()
    {
        mapper.upsertUserProfile(profile("READY", Date.from(NOW)));
        jdbcTemplate.update("insert into t_user_behavior_event(user_id, event_type, scene, product_id, "
                + "category_id, dedup_key, event_time) values (7, 'PRODUCT_VIEW', 'MALL', 101, 11, 'late-view', ?)",
            Date.from(NOW.minus(5, ChronoUnit.MINUTES)));

        assertEquals(7L, mapper.selectChangedUserIds().get(0));
    }

    @Test
    void mallOrderUpdateTimeTriggersExistingProfileAgain()
    {
        mapper.upsertUserProfile(profile("READY", Date.from(NOW)));
        jdbcTemplate.update("update t_order set pay_amount = 26.00, update_time = ? where order_id = 1001",
            Date.from(NOW.plus(1, ChronoUnit.MINUTES)));

        assertEquals(7L, mapper.selectChangedUserIds().get(0));
    }

    @Test
    void historicalEvidenceSurvivesProductDeletionAndKeepsCapturedCategory()
    {
        jdbcTemplate.update("insert into t_category(category_id, category_name) values (12, '新品类')");
        jdbcTemplate.update("update t_product set category_id = 12 where product_id = 101");

        ProfileEvidence behavior = mapper.selectBehaviorEvidence(7L).get(0);
        assertEquals(11L, behavior.getCategoryId());
        assertEquals("奶咖", behavior.getCategoryName());

        jdbcTemplate.update("delete from t_product where product_id = 101");
        List<ProfileEvidence> purchases = mapper.selectPurchaseEvidence(7L);
        assertEquals(3, purchases.size());
        assertEquals("拿铁", purchases.get(1).getProductName());
        assertEquals(new BigDecimal("25.00"), purchases.get(1).getPrice());
    }

    @Test
    void deletedUserCannotBeRecreatedByLateProfileUpsert()
    {
        jdbcTemplate.update("delete from t_wxuser where id = 7");

        assertEquals(0, mapper.upsertUserProfile(profile("READY", new Date())));
        assertEquals(0, jdbcTemplate.queryForObject(
            "select count(*) from t_user_profile where user_id = 7", Integer.class));
    }

    private void insertMallOrder(Long orderId, int status, int refundStatus, String amount, Date time)
    {
        jdbcTemplate.update("insert into t_order(order_id, order_no, user_id, total_amount, pay_amount, status, "
                + "refund_status, pay_time, create_time) values (?, ?, 7, ?, ?, ?, ?, ?, ?)",
            orderId, "MO-" + orderId, new BigDecimal(amount), new BigDecimal(amount), status, refundStatus,
            time, time);
    }

    private void insertMallItem(Long orderId, Long productId, String productName, String price, String spec)
    {
        jdbcTemplate.update("insert into t_order_item(order_id, product_id, product_name, price, spec, quantity) "
                + "values (?, ?, ?, ?, ?, 1)",
            orderId, productId, productName, new BigDecimal(price), spec);
    }

    private UserProfile profile(String status, Date calculateTime)
    {
        UserProfile profile = new UserProfile();
        profile.setUserId(7L);
        profile.setProfileStatus(status);
        profile.setProfileData("{\"MALL\":{},\"SCAN\":{}}");
        profile.setCalculateTime(calculateTime);
        profile.setCreateTime(dateDaysAgo(2));
        profile.setUpdateTime(calculateTime);
        return profile;
    }

    private Date dateDaysAgo(long days)
    {
        return Date.from(NOW.minus(days, ChronoUnit.DAYS));
    }
}
