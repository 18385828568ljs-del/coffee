package com.ruoyi.project.coffee.card.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.project.coffee.card.domain.CardDrawRecord;
import com.ruoyi.project.coffee.card.mapper.AiCardMapper;
import com.ruoyi.project.coffee.card.mapper.CardCampaignMapper;
import com.ruoyi.project.coffee.card.mapper.CardDrawRecordMapper;

@ExtendWith(MockitoExtension.class)
class CardDrawServiceTest
{
    @Mock CardCampaignMapper campaignMapper;
    @Mock AiCardMapper cardMapper;
    @Mock CardDrawRecordMapper drawMapper;

    @Test
    void repeatedRequestReturnsExistingRecordWithoutDrawingAgain()
    {
        CardDrawRecord existing=new CardDrawRecord(); existing.setDrawId(10L);
        when(drawMapper.selectByRequestNo("request-1",7L)).thenReturn(existing);
        CardDrawService service=new CardDrawService(campaignMapper,cardMapper,drawMapper);

        CardDrawRecord result=service.draw(3L,7L,"request-1");

        assertSame(existing,result);
        verify(campaignMapper,never()).selectActive(7L);
        verify(cardMapper,never()).selectDrawableByCampaign(3L);
    }
}
