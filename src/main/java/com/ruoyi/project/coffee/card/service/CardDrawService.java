package com.ruoyi.project.coffee.card.service;

import java.security.SecureRandom;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.card.domain.AiCard;
import com.ruoyi.project.coffee.card.domain.CardCampaign;
import com.ruoyi.project.coffee.card.domain.CardDrawRecord;
import com.ruoyi.project.coffee.card.mapper.AiCardMapper;
import com.ruoyi.project.coffee.card.mapper.CardCampaignMapper;
import com.ruoyi.project.coffee.card.mapper.CardDrawRecordMapper;

@Service
public class CardDrawService
{
    private final CardCampaignMapper campaignMapper;
    private final AiCardMapper cardMapper;
    private final CardDrawRecordMapper drawMapper;
    private final SecureRandom random=new SecureRandom();

    public CardDrawService(CardCampaignMapper campaignMapper,AiCardMapper cardMapper,CardDrawRecordMapper drawMapper)
    { this.campaignMapper=campaignMapper; this.cardMapper=cardMapper; this.drawMapper=drawMapper; }

    public CardCampaign active(Long userId) { return campaignMapper.selectActive(userId); }
    public CardDrawRecord result(Long drawId,Long userId) { return drawMapper.selectByIdAndUser(drawId,userId); }
    public List<CardDrawRecord> my(Long userId) { return drawMapper.selectByUser(userId); }

    @Transactional(rollbackFor=Exception.class)
    public CardDrawRecord draw(Long campaignId,Long userId,String requestNo)
    {
        if(userId==null)throw new ServiceException("请先登录");
        if(StringUtils.isEmpty(requestNo) || requestNo.length()>64)throw new ServiceException("抽卡请求号不正确");
        CardDrawRecord byRequest=drawMapper.selectByRequestNo(requestNo,userId);
        if(byRequest!=null)return byRequest;
        CardDrawRecord existing=drawMapper.selectByCampaignAndUser(campaignId,userId);
        if(existing!=null)return existing;
        CardCampaign active=campaignMapper.selectActive(userId);
        if(active==null || !active.getCampaignId().equals(campaignId))throw new ServiceException("活动不存在、未开始或已结束");
        List<AiCard> cards=cardMapper.selectDrawableByCampaign(campaignId);
        if(cards.isEmpty())throw new ServiceException("当前没有可抽取的卡片");
        AiCard selected=weighted(cards);
        CardDrawRecord record=new CardDrawRecord();
        record.setRequestNo(requestNo); record.setCampaignId(campaignId); record.setCardId(selected.getCardId()); record.setUserId(userId);
        try { drawMapper.insert(record); }
        catch(DuplicateKeyException e)
        {
            CardDrawRecord duplicate=drawMapper.selectByCampaignAndUser(campaignId,userId);
            if(duplicate!=null)return duplicate;
            throw e;
        }
        return drawMapper.selectByIdAndUser(record.getDrawId(),userId);
    }

    private AiCard weighted(List<AiCard> cards)
    {
        long total=0;
        for(AiCard card:cards)total+=Math.max(1,card.getWeight());
        long target=(long)(random.nextDouble()*total);
        for(AiCard card:cards) { target-=Math.max(1,card.getWeight()); if(target<0)return card; }
        return cards.get(cards.size()-1);
    }
}
