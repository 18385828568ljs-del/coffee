package com.ruoyi.project.coffee.card.service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import com.ruoyi.project.coffee.card.config.CardAiProperties;
import com.ruoyi.project.coffee.card.domain.AiCard;
import com.ruoyi.project.coffee.card.domain.CardGenerationTask;
import com.ruoyi.project.coffee.card.mapper.AiCardMapper;
import com.ruoyi.project.coffee.card.mapper.CardGenerationTaskMapper;

@Component
@ConditionalOnProperty(prefix="ai.card",name="enabled",havingValue="true")
public class CardGenerationWorker
{
    private static final Logger log = LoggerFactory.getLogger(CardGenerationWorker.class);

    private final CardAiProperties properties;
    private final CardGenerationTaskMapper taskMapper;
    private final AiCardMapper cardMapper;
    private final CardArtworkService artworkService;
    private final CardRenderService renderService;
    private final ScheduledExecutorService scheduler;
    private final ThreadPoolTaskExecutor executor;
    private final AtomicBoolean polling=new AtomicBoolean(false);
    private final AtomicInteger inFlight=new AtomicInteger(0);

    public CardGenerationWorker(CardAiProperties properties, CardGenerationTaskMapper taskMapper, AiCardMapper cardMapper,
        CardArtworkService artworkService, CardRenderService renderService,
        @Qualifier("scheduledExecutorService") ScheduledExecutorService scheduler,
        @Qualifier("threadPoolTaskExecutor") ThreadPoolTaskExecutor executor)
    {
        this.properties=properties; this.taskMapper=taskMapper; this.cardMapper=cardMapper;
        this.artworkService=artworkService; this.renderService=renderService; this.scheduler=scheduler; this.executor=executor;
    }

    @PostConstruct
    public void start()
    {
        taskMapper.recoverStaleRunning(Math.max(5,properties.getStaleMinutes()));
        scheduler.scheduleWithFixedDelay(this::pollSafely,1,Math.max(2,properties.getPollSeconds()),TimeUnit.SECONDS);
    }

    private void pollSafely()
    {
        try { poll(); }
        catch (Exception e) { log.error("AI卡片任务轮询失败，下个周期将自动重试", e); }
    }

    void poll()
    {
        if (!polling.compareAndSet(false,true)) return;
        try
        {
            taskMapper.recoverStaleRunning(Math.max(5,properties.getStaleMinutes()));
            int limit=Math.max(1,Math.min(properties.getConcurrency(),5));
            int available=limit-inFlight.get();
            if (available<=0) return;
            List<CardGenerationTask> tasks=taskMapper.selectRunnableTasks(available);
            for (CardGenerationTask task:tasks) dispatch(task.getTaskId(),limit);
        }
        finally { polling.set(false); }
    }

    private void dispatch(Long taskId,int limit)
    {
        if (inFlight.incrementAndGet()>limit)
        {
            inFlight.decrementAndGet();
            return;
        }
        if (taskMapper.claim(taskId)!=1)
        {
            inFlight.decrementAndGet();
            return;
        }
        try
        {
            executor.execute(() -> {
                try { processClaimed(taskId); }
                finally { inFlight.decrementAndGet(); }
            });
        }
        catch (RuntimeException e)
        {
            inFlight.decrementAndGet();
            taskMapper.releaseClaim(taskId);
            log.error("AI卡片任务提交线程池失败，任务仍保留在数据库中", e);
        }
    }

    void process(Long taskId)
    {
        if (taskMapper.claim(taskId)!=1) return;
        processClaimed(taskId);
    }

    private void processClaimed(Long taskId)
    {
        CardGenerationTask task=taskMapper.selectById(taskId);
        AiCard card=cardMapper.selectById(task.getCardId());
        if (card==null) { fail(task,"卡片已不存在",false); return; }
        try
        {
            String artworkUrl=task.getArtworkUrl();
            if (StringUtils.isBlank(artworkUrl))
            {
                artworkUrl=artworkService.generate(card);
                task.setArtworkUrl(artworkUrl); task.setStage("ARTWORK_READY"); task.setStatus("RUNNING"); task.setErrorMessage(null);
                taskMapper.updateProgress(task);
                cardMapper.updateGenerationResult(card.getCardId(),artworkUrl,null,1,null);
            }
            card.setArtworkUrl(artworkUrl);
            String finalUrl=renderService.renderAndUpload(card,artworkUrl);
            task.setFinalImageUrl(finalUrl); task.setStage("READY"); task.setStatus("SUCCESS");
            task.setErrorMessage(null); task.setNextRetryTime(null); task.setFinishedTime(new Date());
            taskMapper.updateProgress(task);
            cardMapper.updateGenerationResult(card.getCardId(),artworkUrl,finalUrl,2,null);
        }
        catch(Exception e)
        {
            fail(task,rootMessage(e),isRetryable(e));
        }
    }

    private void fail(CardGenerationTask task,String message,boolean retryable)
    {
        log.warn("AI卡片生成失败，taskId={}，cardId={}，stage={}，message={}",
            task.getTaskId(),task.getCardId(),task.getStage(),limit(message,900));
        boolean retry=retryable && task.getAttemptCount()<task.getMaxAttempts();
        task.setStatus(retry?"RETRY":"FAILED"); task.setStage(retry?task.getStage():"FAILED");
        task.setErrorMessage(limit(message,900));
        task.setNextRetryTime(retry?new Date(System.currentTimeMillis()+Math.min(300000L,30000L*(1L<<Math.max(0,task.getAttemptCount()-1)))):null);
        task.setFinishedTime(retry?null:new Date());
        taskMapper.updateProgress(task);
        cardMapper.updateGenerationResult(task.getCardId(),task.getArtworkUrl(),task.getFinalImageUrl(),retry?1:4,task.getErrorMessage());
    }

    private boolean isRetryable(Exception e)
    {
        String m=rootMessage(e).toLowerCase();
        return !(m.contains("未配置") || m.contains("api_key") || m.contains("参数") || m.contains("不支持") || m.contains("尺寸不足"));
    }
    private String rootMessage(Throwable e) { Throwable c=e; while(c.getCause()!=null)c=c.getCause(); return c.getMessage()==null?c.getClass().getSimpleName():c.getMessage(); }
    private String limit(String s,int max) { if(s==null)return "生成失败"; return s.length()<=max?s:s.substring(0,max); }
}
