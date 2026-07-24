package com.ruoyi.project.coffee.activity.service;

import java.util.List;
import com.ruoyi.project.coffee.activity.domain.TMarketingActivity;

/**
 * 营销活动Service接口
 * 
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
public interface ITMarketingActivityService 
{
    /**
     * 查询营销活动
     * 
     * @param activityId 营销活动主键
     * @return 营销活动
     */
    public TMarketingActivity selectTMarketingActivityByActivityId(Long activityId);

    /**
     * 查询营销活动列表
     * 
     * @param tMarketingActivity 营销活动
     * @return 营销活动集合
     */
    public List<TMarketingActivity> selectTMarketingActivityList(TMarketingActivity tMarketingActivity);

    /**
     * 新增营销活动
     * 
     * @param tMarketingActivity 营销活动
     * @return 结果
     */
    public int insertTMarketingActivity(TMarketingActivity tMarketingActivity);

    /**
     * 修改营销活动
     * 
     * @param tMarketingActivity 营销活动
     * @return 结果
     */
    public int updateTMarketingActivity(TMarketingActivity tMarketingActivity);

    /**
     * 批量删除营销活动
     * 
     * @param activityIds 需要删除的营销活动主键集合
     * @return 结果
     */
    public int deleteTMarketingActivityByActivityIds(String activityIds);

    /**
     * 删除营销活动信息
     * 
     * @param activityId 营销活动主键
     * @return 结果
     */
    public int deleteTMarketingActivityByActivityId(Long activityId);
}
