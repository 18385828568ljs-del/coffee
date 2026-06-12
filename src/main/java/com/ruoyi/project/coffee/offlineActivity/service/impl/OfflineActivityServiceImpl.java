package com.ruoyi.project.coffee.offlineActivity.service.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.text.Convert;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivity;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivitySignup;
import com.ruoyi.project.coffee.offlineActivity.mapper.OfflineActivityMapper;
import com.ruoyi.project.coffee.offlineActivity.mapper.OfflineActivitySignupMapper;
import com.ruoyi.project.coffee.offlineActivity.service.IOfflineActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfflineActivityServiceImpl implements IOfflineActivityService
{
    public static final int ACTIVITY_STATUS_OFF = 0;
    public static final int ACTIVITY_STATUS_ON = 1;
    public static final int SIGNUP_STATUS_RESERVED = 0;
    public static final int SIGNUP_STATUS_CANCELLED = 1;
    public static final int SIGNUP_STATUS_ATTENDED = 2;
    public static final int SIGNUP_STATUS_ABSENT = 3;

    @Autowired
    private OfflineActivityMapper offlineActivityMapper;

    @Autowired
    private OfflineActivitySignupMapper signupMapper;

    @Override
    public OfflineActivity selectOfflineActivityByActivityId(Long activityId)
    {
        return offlineActivityMapper.selectOfflineActivityByActivityId(activityId);
    }

    @Override
    public List<OfflineActivity> selectOfflineActivityList(OfflineActivity offlineActivity)
    {
        return offlineActivityMapper.selectOfflineActivityList(offlineActivity);
    }

    @Override
    public List<OfflineActivity> selectAvailableOfflineActivityList(Long userId)
    {
        return offlineActivityMapper.selectAvailableOfflineActivityList(userId);
    }

    @Override
    public OfflineActivity selectAvailableOfflineActivityById(Long activityId, Long userId)
    {
        return offlineActivityMapper.selectAvailableOfflineActivityById(activityId, userId);
    }

    @Override
    public int insertOfflineActivity(OfflineActivity offlineActivity)
    {
        offlineActivity.setCreateTime(DateUtils.getNowDate());
        normalizeActivity(offlineActivity);
        return offlineActivityMapper.insertOfflineActivity(offlineActivity);
    }

    @Override
    public int updateOfflineActivity(OfflineActivity offlineActivity)
    {
        offlineActivity.setUpdateTime(DateUtils.getNowDate());
        normalizeActivity(offlineActivity);
        return offlineActivityMapper.updateOfflineActivity(offlineActivity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteOfflineActivityByActivityIds(String activityIds)
    {
        signupMapper.deleteOfflineActivitySignupByActivityIds(Convert.toStrArray(activityIds));
        return offlineActivityMapper.deleteOfflineActivityByActivityIds(Convert.toStrArray(activityIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteOfflineActivityByActivityId(Long activityId)
    {
        signupMapper.deleteOfflineActivitySignupByActivityId(activityId);
        return offlineActivityMapper.deleteOfflineActivityByActivityId(activityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OfflineActivitySignup signup(Long activityId, Long userId)
    {
        if (activityId == null || userId == null)
        {
            throw new IllegalArgumentException("请先登录后再预约");
        }
        OfflineActivity activity = offlineActivityMapper.selectOfflineActivityByActivityId(activityId);
        validateCanSignup(activity);

        OfflineActivitySignup exist = signupMapper.selectUserSignup(activityId, userId);
        if (exist != null)
        {
            if (SIGNUP_STATUS_RESERVED == exist.getStatus() || SIGNUP_STATUS_ATTENDED == exist.getStatus())
            {
                throw new IllegalArgumentException("你已经预约过该活动");
            }
            if (SIGNUP_STATUS_CANCELLED == exist.getStatus())
            {
                ensureQuotaAvailable(activityId, activity.getQuota());
                exist.setStatus(SIGNUP_STATUS_RESERVED);
                exist.setSignupTime(DateUtils.getNowDate());
                exist.setCancelTime(null);
                exist.setCheckinTime(null);
                exist.setUpdateTime(DateUtils.getNowDate());
                signupMapper.updateOfflineActivitySignup(exist);
                return signupMapper.selectOfflineActivitySignupBySignupId(exist.getSignupId());
            }
        }

        ensureQuotaAvailable(activityId, activity.getQuota());
        OfflineActivitySignup signup = new OfflineActivitySignup();
        signup.setActivityId(activityId);
        signup.setUserId(userId);
        signup.setStatus(SIGNUP_STATUS_RESERVED);
        signup.setSignupTime(DateUtils.getNowDate());
        signup.setCreateTime(DateUtils.getNowDate());
        signupMapper.insertOfflineActivitySignup(signup);
        return signupMapper.selectOfflineActivitySignupBySignupId(signup.getSignupId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelSignup(Long activityId, Long userId)
    {
        if (activityId == null || userId == null)
        {
            throw new IllegalArgumentException("请先登录后再取消预约");
        }
        OfflineActivity activity = offlineActivityMapper.selectOfflineActivityByActivityId(activityId);
        if (activity == null)
        {
            throw new IllegalArgumentException("活动不存在");
        }
        Date now = DateUtils.getNowDate();
        if (activity.getStartTime() != null && !now.before(activity.getStartTime()))
        {
            throw new IllegalArgumentException("活动已开始，不能取消预约");
        }
        OfflineActivitySignup signup = signupMapper.selectUserSignup(activityId, userId);
        if (signup == null || SIGNUP_STATUS_CANCELLED == signup.getStatus())
        {
            throw new IllegalArgumentException("你还没有预约该活动");
        }
        if (SIGNUP_STATUS_ATTENDED == signup.getStatus() || SIGNUP_STATUS_ABSENT == signup.getStatus())
        {
            throw new IllegalArgumentException("活动已完成核对，不能取消预约");
        }
        signup.setStatus(SIGNUP_STATUS_CANCELLED);
        signup.setCancelTime(now);
        signup.setUpdateTime(now);
        return signupMapper.updateOfflineActivitySignup(signup);
    }

    @Override
    public List<OfflineActivitySignup> selectSignupList(OfflineActivitySignup signup)
    {
        return signupMapper.selectOfflineActivitySignupList(signup);
    }

    @Override
    public List<OfflineActivitySignup> selectUserSignupList(Long userId)
    {
        return signupMapper.selectUserSignupList(userId);
    }

    @Override
    public int markSignupStatus(Long signupId, Integer status)
    {
        if (signupId == null)
        {
            throw new IllegalArgumentException("预约记录不存在");
        }
        if (status == null || (status != SIGNUP_STATUS_ATTENDED && status != SIGNUP_STATUS_ABSENT))
        {
            throw new IllegalArgumentException("状态参数错误");
        }
        return signupMapper.markSignupStatus(signupId, status);
    }

    private void normalizeActivity(OfflineActivity activity)
    {
        if (activity.getStatus() == null)
        {
            activity.setStatus(ACTIVITY_STATUS_OFF);
        }
        if (activity.getSortOrder() == null)
        {
            activity.setSortOrder(0);
        }
        if (activity.getQuota() == null || activity.getQuota() < 1)
        {
            activity.setQuota(1);
        }
    }

    private void validateCanSignup(OfflineActivity activity)
    {
        if (activity == null)
        {
            throw new IllegalArgumentException("活动不存在");
        }
        if (activity.getStatus() == null || ACTIVITY_STATUS_ON != activity.getStatus())
        {
            throw new IllegalArgumentException("活动暂未开放预约");
        }
        Date now = DateUtils.getNowDate();
        if (activity.getSignupDeadline() != null && now.after(activity.getSignupDeadline()))
        {
            throw new IllegalArgumentException("活动预约已截止");
        }
        if (activity.getStartTime() != null && !now.before(activity.getStartTime()))
        {
            throw new IllegalArgumentException("活动已开始，不能预约");
        }
    }

    private void ensureQuotaAvailable(Long activityId, Integer quota)
    {
        int reservedCount = signupMapper.countReservedByActivityId(activityId);
        if (quota != null && quota > 0 && reservedCount >= quota)
        {
            throw new IllegalArgumentException("活动名额已满");
        }
    }
}
