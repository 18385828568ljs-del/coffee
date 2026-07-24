package com.ruoyi.project.coffee.offlineActivity.service;

import java.util.List;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivity;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivitySignup;

public interface IOfflineActivityService
{
    public OfflineActivity selectOfflineActivityByActivityId(Long activityId);

    public List<OfflineActivity> selectOfflineActivityList(OfflineActivity offlineActivity);

    public List<OfflineActivity> selectAvailableOfflineActivityList(Long userId);

    public OfflineActivity selectAvailableOfflineActivityById(Long activityId, Long userId);

    public int insertOfflineActivity(OfflineActivity offlineActivity);

    public int updateOfflineActivity(OfflineActivity offlineActivity);

    public int deleteOfflineActivityByActivityIds(String activityIds);

    public int deleteOfflineActivityByActivityId(Long activityId);

    public OfflineActivitySignup signup(Long activityId, Long userId);

    public int cancelSignup(Long activityId, Long userId);

    public List<OfflineActivitySignup> selectSignupList(OfflineActivitySignup signup);

    public List<OfflineActivitySignup> selectUserSignupList(Long userId);

    public int markSignupStatus(Long signupId, Integer status);
}
