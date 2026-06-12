package com.ruoyi.project.coffee.offlineActivity.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivity;

public interface OfflineActivityMapper
{
    public OfflineActivity selectOfflineActivityByActivityId(Long activityId);

    public List<OfflineActivity> selectOfflineActivityList(OfflineActivity offlineActivity);

    public List<OfflineActivity> selectAvailableOfflineActivityList(@Param("userId") Long userId);

    public OfflineActivity selectAvailableOfflineActivityById(@Param("activityId") Long activityId, @Param("userId") Long userId);

    public int insertOfflineActivity(OfflineActivity offlineActivity);

    public int updateOfflineActivity(OfflineActivity offlineActivity);

    public int deleteOfflineActivityByActivityId(Long activityId);

    public int deleteOfflineActivityByActivityIds(String[] activityIds);
}
