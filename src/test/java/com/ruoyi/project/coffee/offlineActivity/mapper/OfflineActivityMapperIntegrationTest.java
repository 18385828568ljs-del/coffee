package com.ruoyi.project.coffee.offlineActivity.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.List;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivity;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivitySignup;
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
    "mybatis.mapper-locations=classpath:mybatis/coffee/OfflineActivityMapper.xml,classpath:mybatis/coffee/OfflineActivitySignupMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.offlineActivity.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.offlineActivity.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OfflineActivityMapperIntegrationTest
{
    @Autowired
    private OfflineActivityMapper activityMapper;

    @Autowired
    private OfflineActivitySignupMapper signupMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectAvailableActivitiesShouldIncludeSignupCountAndCurrentUserSignup()
    {
        Long activityId = insertActivity("手冲体验课", 1, 1);
        insertSignup(activityId, 2001L, 0);
        Long currentSignupId = insertSignup(activityId, 2002L, 2);
        insertSignup(activityId, 2003L, 1);
        insertWxUser(2002L, "测试用户", "avatar.png");

        List<OfflineActivity> activities = activityMapper.selectAvailableOfflineActivityList(2002L);

        assertEquals(1, activities.size());
        OfflineActivity activity = activities.get(0);
        assertEquals("手冲体验课", activity.getTitle());
        assertEquals(Integer.valueOf(2), activity.getSignupCount());
        assertEquals(Integer.valueOf(2), activity.getCurrentUserSignupStatus());
        assertEquals(currentSignupId, activity.getCurrentUserSignupId());
    }

    @Test
    void selectAvailableActivitiesShouldFilterOffAndEndedActivities()
    {
        insertActivity("开放活动", 1, 1);
        insertActivity("未开放活动", 0, 2);
        Long endedId = insertActivity("已结束活动", 1, 3);
        jdbcTemplate.update("update t_offline_activity set end_time = ? where activity_id = ?",
            new Date(System.currentTimeMillis() - 3600000L), endedId);

        List<OfflineActivity> activities = activityMapper.selectAvailableOfflineActivityList(2001L);

        assertEquals(1, activities.size());
        assertEquals("开放活动", activities.get(0).getTitle());
    }

    @Test
    void signupMapperShouldJoinActivityAndUserProfile()
    {
        Long activityId = insertActivity("拉花沙龙", 1, 1);
        insertWxUser(2001L, "咖啡客", "wx-avatar.png");
        Long signupId = insertSignup(activityId, 2001L, 0);

        OfflineActivitySignup signup = signupMapper.selectOfflineActivitySignupBySignupId(signupId);

        assertEquals("拉花沙龙", signup.getActivityTitle());
        assertEquals("咖啡客", signup.getNickname());
        assertEquals("wx-avatar.png", signup.getAvatar());
        assertNotNull(signup.getActivityStartTime());
        assertEquals("测试门店", signup.getActivityLocation());
    }

    @Test
    void selectUserSignupShouldReturnLatestSignupForSameActivityAndUser()
    {
        Long activityId = insertActivity("杯测活动", 1, 1);
        Long oldSignupId = insertSignup(activityId, 2001L, 1);
        Long newSignupId = insertSignup(activityId, 2001L, 0);

        OfflineActivitySignup signup = signupMapper.selectUserSignup(activityId, 2001L);

        assertEquals(newSignupId, signup.getSignupId());
        assertEquals(Integer.valueOf(0), signup.getStatus());
        assertNotNull(oldSignupId);
    }

    @Test
    void markSignupStatusShouldOnlyUpdateReservedSignup()
    {
        Long activityId = insertActivity("咖啡分享会", 1, 1);
        Long reservedId = insertSignup(activityId, 2001L, 0);
        Long cancelledId = insertSignup(activityId, 2002L, 1);

        assertEquals(1, signupMapper.markSignupStatus(reservedId, 2));
        assertEquals(0, signupMapper.markSignupStatus(cancelledId, 2));

        OfflineActivitySignup reserved = signupMapper.selectOfflineActivitySignupBySignupId(reservedId);
        OfflineActivitySignup cancelled = signupMapper.selectOfflineActivitySignupBySignupId(cancelledId);
        assertEquals(Integer.valueOf(2), reserved.getStatus());
        assertNotNull(reserved.getCheckinTime());
        assertEquals(Integer.valueOf(1), cancelled.getStatus());
        assertNull(cancelled.getCheckinTime());
    }

    @Test
    void deleteActivityShouldNotDeleteSignupUnlessSignupMapperIsCalled()
    {
        Long activityId = insertActivity("可删除活动", 1, 1);
        insertSignup(activityId, 2001L, 0);

        assertEquals(1, signupMapper.deleteOfflineActivitySignupByActivityId(activityId));
        assertEquals(1, activityMapper.deleteOfflineActivityByActivityId(activityId));

        assertNull(activityMapper.selectOfflineActivityByActivityId(activityId));
        assertEquals(0, signupMapper.countReservedByActivityId(activityId));
    }

    private Long insertActivity(String title, int status, int sortOrder)
    {
        OfflineActivity activity = new OfflineActivity();
        activity.setTitle(title);
        activity.setCoverImage("cover.png");
        activity.setSummary("活动简介");
        activity.setContent("活动详情");
        activity.setStartTime(new Date(System.currentTimeMillis() + 3600000L));
        activity.setEndTime(new Date(System.currentTimeMillis() + 7200000L));
        activity.setSignupDeadline(new Date(System.currentTimeMillis() + 1800000L));
        activity.setLocation("测试门店");
        activity.setQuota(10);
        activity.setSortOrder(sortOrder);
        activity.setStatus(status);
        activity.setCreateBy("test");
        activity.setCreateTime(new Date());
        activity.setRemark("测试活动");
        assertEquals(1, activityMapper.insertOfflineActivity(activity));
        assertNotNull(activity.getActivityId());
        return activity.getActivityId();
    }

    private Long insertSignup(Long activityId, Long userId, int status)
    {
        OfflineActivitySignup signup = new OfflineActivitySignup();
        signup.setActivityId(activityId);
        signup.setUserId(userId);
        signup.setStatus(status);
        signup.setSignupTime(new Date(System.currentTimeMillis() + userId));
        signup.setCreateTime(new Date());
        signup.setRemark("测试预约");
        assertEquals(1, signupMapper.insertOfflineActivitySignup(signup));
        assertNotNull(signup.getSignupId());
        return signup.getSignupId();
    }

    private void insertWxUser(Long userId, String nickname, String avatar)
    {
        jdbcTemplate.update(
            "insert into t_wxuser (id, openid, nickname, avatar) values (?, ?, ?, ?)",
            userId, "openid-" + userId, nickname, avatar
        );
    }
}
