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
import com.ruoyi.project.coffee.card.domain.CardCampaign;
import com.ruoyi.project.coffee.card.service.CardManagementService;

@Controller
@RequestMapping("/coffee/cardCampaign")
public class CardCampaignController extends BaseController
{
    private final CardManagementService service;
    public CardCampaignController(CardManagementService service) { this.service=service; }

    @RequiresPermissions("coffee:card:view")
    @GetMapping public String index() { return "coffee/card/campaign"; }

    @RequiresPermissions("coffee:card:list")
    @PostMapping("/list") @ResponseBody
    public TableDataInfo list(CardCampaign filter) { startPage(); return getDataTable(service.listCampaigns(filter)); }

    @RequiresPermissions("coffee:card:add")
    @GetMapping("/add") public String add() { return "coffee/card/campaign-add"; }

    @RequiresPermissions("coffee:card:add") @Log(title="AI卡片活动",businessType=BusinessType.INSERT)
    @PostMapping("/add") @ResponseBody
    public AjaxResult addSave(CardCampaign campaign) { return toAjax(service.addCampaign(campaign,getLoginName())); }

    @RequiresPermissions("coffee:card:edit")
    @GetMapping("/edit/{id}") public String edit(@PathVariable Long id,ModelMap model) { model.put("campaign",service.getCampaign(id)); return "coffee/card/campaign-edit"; }

    @RequiresPermissions("coffee:card:edit") @Log(title="AI卡片活动",businessType=BusinessType.UPDATE)
    @PostMapping("/edit") @ResponseBody
    public AjaxResult editSave(CardCampaign campaign) { return toAjax(service.updateCampaign(campaign,getLoginName())); }

    @RequiresPermissions("coffee:card:remove") @Log(title="AI卡片活动",businessType=BusinessType.DELETE)
    @PostMapping("/remove") @ResponseBody
    public AjaxResult remove(Long id) { return toAjax(service.deleteCampaign(id)); }

    @RequiresPermissions("coffee:card:publish") @Log(title="AI卡片活动发布",businessType=BusinessType.UPDATE)
    @PostMapping("/publish/{id}") @ResponseBody
    public AjaxResult publish(@PathVariable Long id) { return toAjax(service.publishCampaign(id,getLoginName())); }

    @RequiresPermissions("coffee:card:publish") @Log(title="AI卡片活动下架",businessType=BusinessType.UPDATE)
    @PostMapping("/unpublish/{id}") @ResponseBody
    public AjaxResult unpublish(@PathVariable Long id) { return toAjax(service.unpublishCampaign(id,getLoginName())); }
}
