package com.ruoyi.project.coffee.card.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.card.domain.AiCard;

public interface AiCardMapper
{
    AiCard selectById(Long cardId);
    List<AiCard> selectList(AiCard filter);
    List<AiCard> selectDrawableByCampaign(Long campaignId);
    int insert(AiCard card);
    int update(AiCard card);
    int updateGenerationResult(@Param("cardId") Long cardId, @Param("artworkUrl") String artworkUrl,
        @Param("finalImageUrl") String finalImageUrl, @Param("status") Integer status,
        @Param("lastError") String lastError);
    int publishReadyByCampaign(Long campaignId);
    int unpublishByCampaign(Long campaignId);
    int delete(Long cardId);
    int deleteByCampaign(Long campaignId);
}
