package com.ruoyi.project.coffee.profile.task;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.project.coffee.profile.service.UserProfileService;

/** 用户画像增量刷新、全量校准与过期行为清理任务。 */
@Component("userProfileTask")
public class UserProfileTask
{
    private static final Logger log = LoggerFactory.getLogger(UserProfileTask.class);

    @Autowired
    private UserProfileService userProfileService;

    /** 每 10 分钟刷新发生过行为或交易变化的用户。 */
    public synchronized void refreshIncrementalProfiles()
    {
        refresh("增量刷新", userProfileService.selectChangedUserIds());
    }

    /** 每天凌晨全量校准画像，并清理超过 180 天的行为明细。 */
    public synchronized void refreshAllProfiles()
    {
        try
        {
            int deleted = userProfileService.deleteExpiredBehavior();
            log.info("用户画像全量校准已清理 {} 条过期行为", deleted);
        }
        catch (Exception e)
        {
            log.error("用户画像过期行为清理失败，继续执行全量校准", e);
        }
        refresh("全量校准", userProfileService.selectAllUserIds());
    }

    private void refresh(String taskName, List<Long> userIds)
    {
        int successCount = 0;
        int failedCount = 0;
        for (Long userId : userIds)
        {
            try
            {
                userProfileService.recalculateUser(userId);
                successCount++;
            }
            catch (Exception e)
            {
                failedCount++;
                log.error("用户画像{}失败，userId={}", taskName, userId, e);
            }
        }
        log.info("用户画像{}完成，共 {} 人，成功 {} 人，失败 {} 人",
            taskName, userIds.size(), successCount, failedCount);
    }
}
