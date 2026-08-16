package com.ruoyi.project.coffee.card.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.card.domain.CardCampaign;

public interface CardCampaignMapper
{
    CardCampaign selectById(Long campaignId);
    List<CardCampaign> selectList(CardCampaign filter);
    CardCampaign selectActive(@Param("userId") Long userId);
    int insert(CardCampaign campaign);
    int update(CardCampaign campaign);
    int delete(Long campaignId);
}
