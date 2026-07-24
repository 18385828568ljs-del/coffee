package com.ruoyi.project.coffee.activity.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.text.Convert;
import com.ruoyi.project.coffee.activity.domain.TMarketingActivity;
import com.ruoyi.project.coffee.activity.domain.TMarketingActivityScope;
import com.ruoyi.project.coffee.activity.mapper.TMarketingActivityMapper;
import com.ruoyi.project.coffee.activity.service.ITMarketingActivityService;
import com.ruoyi.project.coffee.activity.util.MarketingActivityScopeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 营销活动 Service 业务层处理
 */
@Service
public class TMarketingActivityServiceImpl implements ITMarketingActivityService
{
    @Autowired
    private TMarketingActivityMapper tMarketingActivityMapper;

    @Override
    public TMarketingActivity selectTMarketingActivityByActivityId(Long activityId)
    {
        TMarketingActivity activity = tMarketingActivityMapper.selectTMarketingActivityByActivityId(activityId);
        populateScopeDetails(activity);
        return activity;
    }

    @Override
    public List<TMarketingActivity> selectTMarketingActivityList(TMarketingActivity tMarketingActivity)
    {
        List<TMarketingActivity> list = tMarketingActivityMapper.selectTMarketingActivityList(tMarketingActivity);
        populateScopeDetails(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTMarketingActivity(TMarketingActivity tMarketingActivity)
    {
        tMarketingActivity.setCreateTime(DateUtils.getNowDate());
        int rows = tMarketingActivityMapper.insertTMarketingActivity(tMarketingActivity);
        syncActivityScopes(tMarketingActivity);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTMarketingActivity(TMarketingActivity tMarketingActivity)
    {
        tMarketingActivity.setUpdateTime(DateUtils.getNowDate());
        int rows = tMarketingActivityMapper.updateTMarketingActivity(tMarketingActivity);
        syncActivityScopes(tMarketingActivity);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTMarketingActivityByActivityIds(String activityIds)
    {
        String[] idArray = Convert.toStrArray(activityIds);
        tMarketingActivityMapper.deleteTMarketingActivityScopeByActivityIds(idArray);
        return tMarketingActivityMapper.deleteTMarketingActivityByActivityIds(idArray);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTMarketingActivityByActivityId(Long activityId)
    {
        tMarketingActivityMapper.deleteTMarketingActivityScopeByActivityId(activityId);
        return tMarketingActivityMapper.deleteTMarketingActivityByActivityId(activityId);
    }

    private void populateScopeDetails(TMarketingActivity activity)
    {
        if (activity == null || activity.getActivityId() == null)
        {
            return;
        }
        populateScopeDetails(Collections.singletonList(activity));
    }

    private void populateScopeDetails(List<TMarketingActivity> activities)
    {
        if (activities == null || activities.isEmpty())
        {
            return;
        }

        List<Long> activityIds = new ArrayList<Long>();
        for (TMarketingActivity activity : activities)
        {
            if (activity != null && activity.getActivityId() != null)
            {
                activityIds.add(activity.getActivityId());
                activity.setScopeIds(Collections.<Long>emptyList());
            }
        }
        if (activityIds.isEmpty())
        {
            return;
        }

        List<TMarketingActivityScope> scopes = tMarketingActivityMapper.selectTMarketingActivityScopesByActivityIds(activityIds);
        for (TMarketingActivity activity : activities)
        {
            if (activity == null || activity.getActivityId() == null)
            {
                continue;
            }
            List<Long> scopeIds = new ArrayList<Long>();
            if (scopes != null)
            {
                for (TMarketingActivityScope scope : scopes)
                {
                    if (scope != null && activity.getActivityId().equals(scope.getActivityId()) && scope.getScopeTargetId() != null)
                    {
                        scopeIds.add(scope.getScopeTargetId());
                    }
                }
            }
            scopeIds = MarketingActivityScopeUtils.dedupeScopeIds(scopeIds);
            activity.setScopeIds(scopeIds);
            activity.setScopeIdsText(MarketingActivityScopeUtils.joinScopeIds(scopeIds));
        }
    }

    private void syncActivityScopes(TMarketingActivity activity)
    {
        if (activity == null || activity.getActivityId() == null)
        {
            return;
        }

        tMarketingActivityMapper.deleteTMarketingActivityScopeByActivityId(activity.getActivityId());
        if (activity.getScopeType() == null || activity.getScopeType() == 0)
        {
            activity.setScopeIds(Collections.<Long>emptyList());
            activity.setScopeIdsText("");
            return;
        }

        List<Long> scopeIds = resolveScopeIds(activity);
        if (scopeIds.isEmpty())
        {
            activity.setScopeIds(Collections.<Long>emptyList());
            activity.setScopeIdsText("");
            return;
        }

        List<TMarketingActivityScope> scopes = new ArrayList<TMarketingActivityScope>();
        for (Long scopeId : scopeIds)
        {
            TMarketingActivityScope scope = new TMarketingActivityScope();
            scope.setActivityId(activity.getActivityId());
            scope.setScopeType(activity.getScopeType());
            scope.setScopeTargetId(scopeId);
            scopes.add(scope);
        }
        tMarketingActivityMapper.batchInsertTMarketingActivityScopes(scopes);
        activity.setScopeIds(scopeIds);
        activity.setScopeIdsText(MarketingActivityScopeUtils.joinScopeIds(scopeIds));
    }

    private List<Long> resolveScopeIds(TMarketingActivity activity)
    {
        if (activity == null)
        {
            return Collections.emptyList();
        }
        if (activity.getScopeIds() != null && !activity.getScopeIds().isEmpty())
        {
            return MarketingActivityScopeUtils.dedupeScopeIds(activity.getScopeIds());
        }
        if (StringUtils.isNotEmpty(activity.getScopeIdsText()))
        {
            return MarketingActivityScopeUtils.parseScopeIds(activity.getScopeIdsText());
        }
        return Collections.emptyList();
    }
}
