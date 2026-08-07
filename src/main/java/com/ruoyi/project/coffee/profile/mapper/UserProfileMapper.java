package com.ruoyi.project.coffee.profile.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.profile.domain.ProfileEvidence;
import com.ruoyi.project.coffee.profile.domain.ProfileOrderSummary;
import com.ruoyi.project.coffee.profile.domain.UserProfile;

/** 用户画像读取与计算数据 Mapper。 */
public interface UserProfileMapper
{
    UserProfile selectUserProfileByUserId(Long userId);

    List<Long> selectAllUserIds();

    List<Long> selectChangedUserIds();

    ProfileOrderSummary selectOrderSummary(Long userId);

    Date selectLastBehaviorTime(Long userId);

    int countRecentBehaviorEvents(@Param("userId") Long userId, @Param("cutoffTime") Date cutoffTime);

    List<ProfileEvidence> selectBehaviorEvidence(
        @Param("userId") Long userId, @Param("cutoffTime") Date cutoffTime);

    List<ProfileEvidence> selectPurchaseEvidence(
        @Param("userId") Long userId, @Param("cutoffTime") Date cutoffTime);

    int upsertUserProfile(UserProfile profile);

    int deleteExpiredBehavior(Date cutoffTime);

    int deleteBehaviorByUserIds(String[] userIds);

    int deleteProfilesByUserIds(String[] userIds);
}
