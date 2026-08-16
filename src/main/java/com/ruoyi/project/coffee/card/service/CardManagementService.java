package com.ruoyi.project.coffee.card.service;

import java.util.Date;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.card.domain.AiCard;
import com.ruoyi.project.coffee.card.domain.CardCampaign;
import com.ruoyi.project.coffee.card.mapper.AiCardMapper;
import com.ruoyi.project.coffee.card.mapper.CardCampaignMapper;
import com.ruoyi.project.coffee.card.mapper.CardDrawRecordMapper;
import com.ruoyi.project.coffee.card.mapper.CardGenerationTaskMapper;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;

@Service
public class CardManagementService
{
    private final CardCampaignMapper campaignMapper;
    private final AiCardMapper cardMapper;
    private final CardGenerationTaskMapper taskMapper;
    private final CardDrawRecordMapper drawMapper;
    private final ITProductService productService;
    private final IScanProductService scanProductService;

    public CardManagementService(CardCampaignMapper campaignMapper, AiCardMapper cardMapper,
        CardGenerationTaskMapper taskMapper, CardDrawRecordMapper drawMapper, ITProductService productService,
        IScanProductService scanProductService)
    {
        this.campaignMapper=campaignMapper; this.cardMapper=cardMapper; this.taskMapper=taskMapper;
        this.drawMapper=drawMapper; this.productService=productService; this.scanProductService=scanProductService;
    }

    public CardCampaign getCampaign(Long id) { return campaignMapper.selectById(id); }
    public List<CardCampaign> listCampaigns(CardCampaign filter) { return campaignMapper.selectList(filter); }
    public AiCard getCard(Long id) { return cardMapper.selectById(id); }
    public List<AiCard> listCards(AiCard filter) { return cardMapper.selectList(filter); }

    public int addCampaign(CardCampaign campaign, String operator)
    {
        validateCampaign(campaign);
        campaign.setStatus(campaign.getStatus() == null ? 0 : campaign.getStatus());
        campaign.setSortOrder(campaign.getSortOrder() == null ? 0 : campaign.getSortOrder());
        campaign.setCreateBy(operator);
        return campaignMapper.insert(campaign);
    }

    public int updateCampaign(CardCampaign campaign, String operator)
    {
        validateCampaign(campaign);
        CardCampaign existing = requiredCampaign(campaign.getCampaignId());
        if (existing.getStatus() != null && existing.getStatus() == 1 && campaign.getStatus() != null && campaign.getStatus() != 1)
        {
            cardMapper.unpublishByCampaign(campaign.getCampaignId());
        }
        campaign.setUpdateBy(operator);
        return campaignMapper.update(campaign);
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteCampaign(Long id)
    {
        CardCampaign campaign = requiredCampaign(id);
        if (campaign.getStatus() != null && campaign.getStatus() == 1) throw new ServiceException("请先下架活动再删除");
        if (drawMapper.countByCampaign(id) > 0) throw new ServiceException("活动已有抽卡记录，不能删除");
        List<AiCard> cards = listCards(filterForCampaign(id));
        for (AiCard card : cards)
        {
            if (card.getStatus() != null && card.getStatus() == 1) throw new ServiceException("活动中有正在生成的卡片，请等待任务完成后再删除");
        }
        for (AiCard card : cards)
        {
            taskMapper.deleteByCard(card.getCardId());
            cardMapper.delete(card.getCardId());
        }
        return campaignMapper.delete(id);
    }

    public int addCard(AiCard card, String operator)
    {
        requiredCampaign(card.getCampaignId());
        normalizeAndValidateCard(card);
        hydrateProductSnapshots(card);
        card.setStatus(0); card.setVersion(1); card.setCreateBy(operator); card.setLastError(null);
        return cardMapper.insert(card);
    }

    public int updateCard(AiCard card, String operator)
    {
        AiCard existing = requiredCard(card.getCardId());
        if (existing.getStatus() != null && existing.getStatus() == 3) throw new ServiceException("已发布卡片请先下架活动后再修改");
        if (existing.getStatus() != null && existing.getStatus() == 1) throw new ServiceException("卡片正在生成，请等待任务完成后再修改");
        normalizeAndValidateCard(card);
        hydrateProductSnapshots(card);
        card.setArtworkUrl(existing.getArtworkUrl());
        // Text or layout edits invalidate the old composite, while the paid artwork remains reusable.
        card.setFinalImageUrl(null);
        card.setStatus(0);
        card.setVersion(existing.getVersion() + 1);
        card.setLastError(null); card.setUpdateBy(operator);
        return cardMapper.update(card);
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteCard(Long cardId)
    {
        AiCard card = requiredCard(cardId);
        CardCampaign campaign = requiredCampaign(card.getCampaignId());
        if (campaign.getStatus() != null && campaign.getStatus() == 1) throw new ServiceException("活动已发布，不能删除卡片");
        if (card.getStatus() != null && card.getStatus() == 1) throw new ServiceException("卡片正在生成，请等待任务完成后再删除");
        taskMapper.deleteByCard(cardId);
        return cardMapper.delete(cardId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int publishCampaign(Long campaignId, String operator)
    {
        CardCampaign campaign = requiredCampaign(campaignId);
        List<AiCard> cards = listCards(filterForCampaign(campaignId));
        int ready=0;
        for (AiCard card : cards) if (card.getStatus() != null && (card.getStatus()==2 || card.getStatus()==3) && StringUtils.isNotEmpty(card.getFinalImageUrl())) ready++;
        if (ready == 0) throw new ServiceException("至少需要一张生成成功的卡片才能发布");
        cardMapper.publishReadyByCampaign(campaignId);
        campaign.setStatus(1); campaign.setUpdateBy(operator);
        return campaignMapper.update(campaign);
    }

    @Transactional(rollbackFor = Exception.class)
    public int unpublishCampaign(Long campaignId, String operator)
    {
        CardCampaign campaign = requiredCampaign(campaignId);
        cardMapper.unpublishByCampaign(campaignId);
        campaign.setStatus(0); campaign.setUpdateBy(operator);
        return campaignMapper.update(campaign);
    }

    private void validateCampaign(CardCampaign campaign)
    {
        if (campaign == null || StringUtils.isEmpty(campaign.getTitle())) throw new ServiceException("活动标题不能为空");
        if (campaign.getStartTime() == null) throw new ServiceException("活动开始时间不能为空");
        if (campaign.getEndTime() != null && campaign.getEndTime().before(campaign.getStartTime())) throw new ServiceException("结束时间不能早于开始时间");
        campaign.setTitle(limit(campaign.getTitle(),120,"活动标题"));
        campaign.setSubtitle(limitNullable(campaign.getSubtitle(),255,"活动副标题"));
    }

    private void normalizeAndValidateCard(AiCard card)
    {
        if (card == null || StringUtils.isEmpty(card.getTitle())) throw new ServiceException("卡片标题不能为空");
        if (StringUtils.isEmpty(card.getLeftProductName()) && card.getLeftProductId()==null) throw new ServiceException("请填写或选择左侧商品");
        if (StringUtils.isEmpty(card.getThemePrompt())) throw new ServiceException("插画主题不能为空");
        card.setTitle(limit(card.getTitle(),80,"卡片标题"));
        card.setEnglishTitle(limitNullable(card.getEnglishTitle(),80,"英文标题"));
        card.setLeftProductName(limitNullable(card.getLeftProductName(),80,"左侧商品名"));
        card.setLeftDescription(limitNullable(card.getLeftDescription(),160,"左侧描述"));
        card.setRightProductName(limitNullable(card.getRightProductName(),80,"右侧商品名"));
        card.setRightDescription(limitNullable(card.getRightDescription(),160,"右侧描述"));
        card.setBrandName(limitNullable(card.getBrandName(),80,"品牌名"));
        card.setThemePrompt(limit(card.getThemePrompt(),1000,"插画主题"));
        card.setTemplateCode(StringUtils.defaultIfEmpty(card.getTemplateCode(),"retro-combo"));
        card.setPaletteCode(StringUtils.defaultIfEmpty(card.getPaletteCode(),"candy"));
        card.setWeight(card.getWeight()==null || card.getWeight()<1 ? 1 : Math.min(card.getWeight(),10000));
    }

    private void hydrateProductSnapshots(AiCard card)
    {
        if (card.getLeftProductId()!=null) hydrate(card,true,card.getLeftProductType(),card.getLeftProductId());
        if (card.getRightProductId()!=null) hydrate(card,false,card.getRightProductType(),card.getRightProductId());
    }

    private void hydrate(AiCard card, boolean left, String type, Long id)
    {
        if ("scan".equals(type))
        {
            ScanProduct product=scanProductService.selectScanProductById(id);
            if (product==null) throw new ServiceException("点单商品不存在："+id);
            if (left) { card.setLeftProductName(product.getProductName()); card.setLeftProductImage(product.getImageUrl()); }
            else { card.setRightProductName(product.getProductName()); card.setRightProductImage(product.getImageUrl()); }
        }
        else
        {
            TProduct product=productService.selectTProductByProductId(id);
            if (product==null) throw new ServiceException("商城商品不存在："+id);
            if (left) { card.setLeftProductType("mall"); card.setLeftProductName(product.getProductName()); card.setLeftProductImage(product.getImageUrl()); }
            else { card.setRightProductType("mall"); card.setRightProductName(product.getProductName()); card.setRightProductImage(product.getImageUrl()); }
        }
    }

    private CardCampaign requiredCampaign(Long id) { CardCampaign c=id==null?null:campaignMapper.selectById(id); if(c==null)throw new ServiceException("卡片活动不存在"); return c; }
    private AiCard requiredCard(Long id) { AiCard c=id==null?null:cardMapper.selectById(id); if(c==null)throw new ServiceException("卡片不存在"); return c; }
    private AiCard filterForCampaign(Long id) { AiCard card=new AiCard(); card.setCampaignId(id); return card; }
    private String limit(String value,int max,String name) { String v=value==null?"":value.trim(); if(v.length()>max)throw new ServiceException(name+"不能超过"+max+"个字符"); return v; }
    private String limitNullable(String value,int max,String name) { return value==null?null:limit(value,max,name); }
}
