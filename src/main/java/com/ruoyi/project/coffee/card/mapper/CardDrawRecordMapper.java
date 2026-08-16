package com.ruoyi.project.coffee.card.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.card.domain.CardDrawRecord;

public interface CardDrawRecordMapper
{
    CardDrawRecord selectByIdAndUser(@Param("drawId") Long drawId, @Param("userId") Long userId);
    CardDrawRecord selectByRequestNo(@Param("requestNo") String requestNo, @Param("userId") Long userId);
    CardDrawRecord selectByCampaignAndUser(@Param("campaignId") Long campaignId, @Param("userId") Long userId);
    List<CardDrawRecord> selectByUser(Long userId);
    int countByCampaign(Long campaignId);
    int insert(CardDrawRecord record);
}
