package com.ruoyi.project.coffee.card.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.ruoyi.project.coffee.card.config.CardAiProperties;
import com.ruoyi.project.coffee.card.domain.AiCard;
import com.ruoyi.project.coffee.card.domain.CardGenerationTask;
import com.ruoyi.project.coffee.card.mapper.AiCardMapper;
import com.ruoyi.project.coffee.card.mapper.CardGenerationTaskMapper;

@ExtendWith(MockitoExtension.class)
class CardGenerationWorkerTest
{
    @Mock CardGenerationTaskMapper taskMapper;
    @Mock AiCardMapper cardMapper;
    @Mock CardArtworkService artworkService;
    @Mock CardRenderService renderService;
    @Mock ScheduledExecutorService scheduler;
    @Mock ThreadPoolTaskExecutor executor;

    @Test
    void resumesFromPersistedArtworkWithoutCallingAiAgain() throws Exception
    {
        CardGenerationTask task=new CardGenerationTask(); task.setTaskId(1L); task.setCardId(2L);
        task.setStatus("RUNNING"); task.setStage("ARTWORK_READY"); task.setAttemptCount(2); task.setMaxAttempts(3);
        task.setArtworkUrl("https://cdn.example/art.png");
        AiCard card=new AiCard(); card.setCardId(2L); card.setVersion(1);
        when(taskMapper.claim(1L)).thenReturn(1); when(taskMapper.selectById(1L)).thenReturn(task);
        when(cardMapper.selectById(2L)).thenReturn(card);
        when(renderService.renderAndUpload(card,"https://cdn.example/art.png")).thenReturn("https://cdn.example/final.png");
        CardGenerationWorker worker=new CardGenerationWorker(new CardAiProperties(),taskMapper,cardMapper,
            artworkService,renderService,scheduler,executor);

        worker.process(1L);

        verify(artworkService,never()).generate(card);
        verify(cardMapper).updateGenerationResult(2L,"https://cdn.example/art.png","https://cdn.example/final.png",2,null);
    }

    @Test
    void pollDoesNotQueueMoreThanConfiguredConcurrency()
    {
        CardAiProperties properties=new CardAiProperties(); properties.setConcurrency(2);
        CardGenerationTask first=new CardGenerationTask(); first.setTaskId(1L);
        CardGenerationTask second=new CardGenerationTask(); second.setTaskId(2L);
        when(taskMapper.selectRunnableTasks(2)).thenReturn(java.util.Arrays.asList(first,second));
        when(taskMapper.claim(1L)).thenReturn(1); when(taskMapper.claim(2L)).thenReturn(1);
        CardGenerationWorker worker=new CardGenerationWorker(properties,taskMapper,cardMapper,
            artworkService,renderService,scheduler,executor);

        worker.poll();
        worker.poll();

        verify(executor,org.mockito.Mockito.times(2)).execute(any(Runnable.class));
        verify(taskMapper).claim(1L); verify(taskMapper).claim(2L);
        verify(taskMapper,org.mockito.Mockito.times(1)).selectRunnableTasks(2);
    }
}
