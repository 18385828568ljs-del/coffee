package com.ruoyi.project.coffee.decorator.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.decorator.ai.domain.DecoratorAiResult;
import com.ruoyi.project.coffee.decorator.ai.domain.DecoratorAiTask;

public interface DecoratorAiMapper
{
    int insertTask(DecoratorAiTask task);
    int updateTaskStatus(@Param("taskId") Long taskId, @Param("status") String status,
            @Param("errorMessage") String errorMessage);
    DecoratorAiTask selectTask(@Param("merchantId") Long merchantId, @Param("taskId") Long taskId);
    int insertResult(DecoratorAiResult result);
    List<DecoratorAiResult> selectResults(@Param("merchantId") Long merchantId, @Param("taskId") Long taskId);
    DecoratorAiResult selectResult(@Param("merchantId") Long merchantId, @Param("resultId") Long resultId);
    int acceptResult(@Param("merchantId") Long merchantId, @Param("resultId") Long resultId,
            @Param("assetId") Long assetId);
}
