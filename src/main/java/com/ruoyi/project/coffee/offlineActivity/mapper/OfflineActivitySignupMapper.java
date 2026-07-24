package com.ruoyi.project.coffee.offlineActivity.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivitySignup;

public interface OfflineActivitySignupMapper
{
    public OfflineActivitySignup selectOfflineActivitySignupBySignupId(Long signupId);

    public OfflineActivitySignup selectUserSignup(@Param("activityId") Long activityId, @Param("userId") Long userId);

    public List<OfflineActivitySignup> selectOfflineActivitySignupList(OfflineActivitySignup signup);

    public List<OfflineActivitySignup> selectUserSignupList(Long userId);

    public int countReservedByActivityId(Long activityId);

    public int insertOfflineActivitySignup(OfflineActivitySignup signup);

    public int updateOfflineActivitySignup(OfflineActivitySignup signup);

    public int deleteOfflineActivitySignupByActivityId(Long activityId);

    public int deleteOfflineActivitySignupByActivityIds(String[] activityIds);

    public int markSignupStatus(@Param("signupId") Long signupId, @Param("status") Integer status);
}
