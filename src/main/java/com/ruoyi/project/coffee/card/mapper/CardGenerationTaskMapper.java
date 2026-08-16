package com.ruoyi.project.coffee.card.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.card.domain.CardGenerationTask;

public interface CardGenerationTaskMapper
{
    CardGenerationTask selectById(Long taskId);
    CardGenerationTask selectLatestByCard(Long cardId);
    List<CardGenerationTask> selectRunnableTasks(@Param("limit") Integer limit);
    int insert(CardGenerationTask task);
    int claim(Long taskId);
    int releaseClaim(Long taskId);
    int updateProgress(CardGenerationTask task);
    int recoverStaleRunning(@Param("staleMinutes") Integer staleMinutes);
    int deleteByCard(Long cardId);
}
