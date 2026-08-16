package com.ruoyi.project.coffee.card.service;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.card.config.CardAiProperties;
import com.ruoyi.project.coffee.card.domain.AiCard;
import com.ruoyi.project.coffee.card.domain.CardGenerationTask;
import com.ruoyi.project.coffee.card.mapper.AiCardMapper;
import com.ruoyi.project.coffee.card.mapper.CardGenerationTaskMapper;

@Service
public class CardGenerationService
{
    private final CardAiProperties properties;
    private final AiCardMapper cardMapper;
    private final CardGenerationTaskMapper taskMapper;
    private final CardRenderService renderService;

    public CardGenerationService(CardAiProperties properties, AiCardMapper cardMapper,
        CardGenerationTaskMapper taskMapper, CardRenderService renderService)
    {
        this.properties=properties; this.cardMapper=cardMapper; this.taskMapper=taskMapper; this.renderService=renderService;
    }

    @Transactional(rollbackFor = Exception.class)
    public CardGenerationTask start(Long cardId)
    {
        if (!properties.isEnabled()) throw new ServiceException("AI卡片生成未启用，请先配置 AI_CARD_ENABLED=true 和 NEWAPI_API_KEY");
        AiCard card=requiredCard(cardId);
        if (card.getStatus()!=null && card.getStatus()==3) throw new ServiceException("已发布卡片不能重新生成");
        CardGenerationTask latest=taskMapper.selectLatestByCard(cardId);
        if (latest!=null && ("PENDING".equals(latest.getStatus()) || "RUNNING".equals(latest.getStatus()) || "RETRY".equals(latest.getStatus()))) return latest;
        CardGenerationTask task=new CardGenerationTask();
        task.setTaskNo(UUID.randomUUID().toString().replace("-","")); task.setCardId(cardId);
        task.setStatus("PENDING"); task.setStage("PENDING"); task.setAttemptCount(0);
        task.setMaxAttempts(Math.max(1,properties.getMaxAttempts()));
        taskMapper.insert(task);
        cardMapper.updateGenerationResult(cardId,card.getArtworkUrl(),card.getFinalImageUrl(),1,null);
        return task;
    }

    public CardGenerationTask latest(Long cardId) { return taskMapper.selectLatestByCard(cardId); }

    public String rerender(Long cardId)
    {
        AiCard card=requiredCard(cardId);
        if (card.getStatus()!=null && card.getStatus()==3) throw new ServiceException("已发布卡片不能重新合成，请先下架活动");
        if (card.getStatus()!=null && card.getStatus()==1) throw new ServiceException("卡片正在生成，请等待任务完成");
        if (StringUtils.isEmpty(card.getArtworkUrl())) throw new ServiceException("卡片还没有AI插画，请先生成");
        try
        {
            String url=renderService.renderAndUpload(card,card.getArtworkUrl());
            cardMapper.updateGenerationResult(cardId,card.getArtworkUrl(),url,2,null);
            return url;
        }
        catch(IOException e) { throw new ServiceException("模板合成失败："+e.getMessage()); }
    }

    private AiCard requiredCard(Long id) { AiCard card=id==null?null:cardMapper.selectById(id); if(card==null)throw new ServiceException("卡片不存在"); return card; }
}
