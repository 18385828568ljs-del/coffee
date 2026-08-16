package com.ruoyi.project.coffee.card.controller;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.coffee.card.domain.AiCard;
import com.ruoyi.project.coffee.card.domain.CardGenerationTask;
import com.ruoyi.project.coffee.card.service.CardGenerationService;
import com.ruoyi.project.coffee.card.service.CardManagementService;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;

@Controller
@RequestMapping("/coffee/card")
public class AiCardController extends BaseController
{
    private final CardManagementService service;
    private final CardGenerationService generationService;
    private final ITProductService productService;
    private final IScanProductService scanProductService;

    public AiCardController(CardManagementService service,CardGenerationService generationService,
        ITProductService productService,IScanProductService scanProductService)
    { this.service=service; this.generationService=generationService; this.productService=productService; this.scanProductService=scanProductService; }

    @RequiresPermissions("coffee:card:view")
    @GetMapping("/campaign/{campaignId}")
    public String index(@PathVariable Long campaignId,ModelMap model)
    { model.put("campaign",service.getCampaign(campaignId)); return "coffee/card/card"; }

    @RequiresPermissions("coffee:card:list")
    @PostMapping("/list") @ResponseBody
    public TableDataInfo list(AiCard filter) { startPage(); return getDataTable(service.listCards(filter)); }

    @RequiresPermissions("coffee:card:add")
    @GetMapping("/add/{campaignId}")
    public String add(@PathVariable Long campaignId,ModelMap model)
    { AiCard card=new AiCard(); card.setCampaignId(campaignId); card.setTemplateCode("retro-combo"); card.setPaletteCode("candy"); card.setWeight(1); model.put("card",card); model.put("campaign",service.getCampaign(campaignId)); putProducts(model); return "coffee/card/card-add"; }

    @RequiresPermissions("coffee:card:add") @Log(title="AI卡片",businessType=BusinessType.INSERT)
    @PostMapping("/add") @ResponseBody
    public AjaxResult addSave(AiCard card) { return toAjax(service.addCard(card,getLoginName())); }

    @RequiresPermissions("coffee:card:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id,ModelMap model)
    { AiCard card=service.getCard(id); model.put("card",card); model.put("campaign",service.getCampaign(card.getCampaignId())); putProducts(model); return "coffee/card/card-edit"; }

    @RequiresPermissions("coffee:card:edit") @Log(title="AI卡片",businessType=BusinessType.UPDATE)
    @PostMapping("/edit") @ResponseBody
    public AjaxResult editSave(AiCard card) { return toAjax(service.updateCard(card,getLoginName())); }

    @RequiresPermissions("coffee:card:remove") @Log(title="AI卡片",businessType=BusinessType.DELETE)
    @PostMapping("/remove") @ResponseBody
    public AjaxResult remove(Long id) { return toAjax(service.deleteCard(id)); }

    @RequiresPermissions("coffee:card:generate") @Log(title="AI卡片生成",businessType=BusinessType.OTHER)
    @PostMapping("/generate/{id}") @ResponseBody
    public AjaxResult generate(@PathVariable Long id) { return AjaxResult.success(generationService.start(id)); }

    @RequiresPermissions("coffee:card:list")
    @GetMapping("/task/{id}") @ResponseBody
    public AjaxResult task(@PathVariable Long id) { CardGenerationTask task=generationService.latest(id); return AjaxResult.success(task); }

    @RequiresPermissions("coffee:card:edit") @Log(title="AI卡片模板合成",businessType=BusinessType.UPDATE)
    @PostMapping("/render/{id}") @ResponseBody
    public AjaxResult render(@PathVariable Long id) { return AjaxResult.success().put("finalImageUrl",generationService.rerender(id)); }

    private void putProducts(ModelMap model)
    {
        model.put("mallProducts",productService.selectTProductList(new TProduct()));
        model.put("scanProducts",scanProductService.selectScanProductList(new ScanProduct()));
    }
}
