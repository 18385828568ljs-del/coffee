package com.ruoyi.project.coffee.card.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.jdbc.Sql;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.ruoyi.project.coffee.card.domain.AiCard;
import com.ruoyi.project.coffee.card.domain.CardCampaign;
import com.ruoyi.project.coffee.card.domain.CardDrawRecord;
import com.ruoyi.project.coffee.card.domain.CardGenerationTask;

@MybatisTest(properties={
    "spring.datasource.url=jdbc:h2:mem:card_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/CardCampaignMapper.xml,classpath:mybatis/coffee/AiCardMapper.xml,classpath:mybatis/coffee/CardGenerationTaskMapper.xml,classpath:mybatis/coffee/CardDrawRecordMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.card.domain"
})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.card.mapper")
@Sql(scripts="/mapper-test-schema.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CardMapperIntegrationTest
{
    @Autowired CardCampaignMapper campaignMapper;
    @Autowired AiCardMapper cardMapper;
    @Autowired CardGenerationTaskMapper taskMapper;
    @Autowired CardDrawRecordMapper drawMapper;

    @Test
    void activeCampaignIncludesPersistedDrawAndEnforcesOneDrawPerUser()
    {
        CardCampaign campaign=campaign(); assertEquals(1,campaignMapper.insert(campaign));
        AiCard card=card(campaign.getCampaignId()); assertEquals(1,cardMapper.insert(card));
        CardDrawRecord draw=new CardDrawRecord(); draw.setRequestNo("req-1"); draw.setCampaignId(campaign.getCampaignId()); draw.setCardId(card.getCardId()); draw.setUserId(8L);
        assertEquals(1,drawMapper.insert(draw));

        CardCampaign active=campaignMapper.selectActive(8L);
        assertNotNull(active); assertEquals(draw.getDrawId(),active.getCurrentUserDrawId()); assertEquals(Integer.valueOf(1),active.getReadyCardCount());

        CardDrawRecord duplicate=new CardDrawRecord(); duplicate.setRequestNo("req-2"); duplicate.setCampaignId(campaign.getCampaignId()); duplicate.setCardId(card.getCardId()); duplicate.setUserId(8L);
        assertThrows(DuplicateKeyException.class,()->drawMapper.insert(duplicate));
    }

    @Test
    void taskClaimIsAtomicAndKeepsPersistentStage()
    {
        CardCampaign campaign=campaign(); campaignMapper.insert(campaign); AiCard card=card(campaign.getCampaignId()); cardMapper.insert(card);
        CardGenerationTask task=new CardGenerationTask(); task.setTaskNo("task-1"); task.setCardId(card.getCardId()); task.setStatus("PENDING"); task.setStage("PENDING"); task.setAttemptCount(0); task.setMaxAttempts(3);
        taskMapper.insert(task);
        assertEquals(1,taskMapper.claim(task.getTaskId())); assertEquals(0,taskMapper.claim(task.getTaskId()));
        CardGenerationTask stored=taskMapper.selectById(task.getTaskId()); assertEquals("RUNNING",stored.getStatus()); assertEquals(Integer.valueOf(1),stored.getAttemptCount());
    }

    private CardCampaign campaign()
    {
        CardCampaign c=new CardCampaign(); c.setTitle("今日咖啡"); c.setSubtitle("抽一张"); c.setStartTime(new Date(System.currentTimeMillis()-60000)); c.setEndTime(new Date(System.currentTimeMillis()+3600000)); c.setStatus(1); c.setSortOrder(0); return c;
    }
    private AiCard card(Long campaignId)
    {
        AiCard c=new AiCard(); c.setCampaignId(campaignId); c.setTitle("游野"); c.setLeftProductName("美式"); c.setThemePrompt("复古咖啡馆"); c.setTemplateCode("retro-combo"); c.setPaletteCode("candy"); c.setFinalImageUrl("https://cdn/card.png"); c.setStatus(3); c.setWeight(1); c.setVersion(1); return c;
    }
}
