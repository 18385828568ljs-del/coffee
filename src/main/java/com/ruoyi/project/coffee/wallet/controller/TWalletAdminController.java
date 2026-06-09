package com.ruoyi.project.coffee.wallet.controller;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.ruoyi.project.coffee.wallet.domain.TRechargeTemplate;
import com.ruoyi.project.coffee.wallet.domain.TRechargeRecord;
import com.ruoyi.project.coffee.wallet.domain.TWalletLog;
import com.ruoyi.project.coffee.wallet.mapper.TWalletMapper;

/**
 * 钱包管理 后台Controller（充值模板、充值记录、余额流水）
 */
@Controller
@RequestMapping("/coffee")
public class TWalletAdminController extends BaseController
{
    @Autowired
    private TWalletMapper walletMapper;

    // ===== 充值模板管理 =====

    @RequiresPermissions("coffee:rechargeTemplate:view")
    @GetMapping("/rechargeTemplate")
    public String rechargeTemplate()
    {
        return "coffee/rechargeTemplate/rechargeTemplate";
    }

    @RequiresPermissions("coffee:rechargeTemplate:list")
    @PostMapping("/rechargeTemplate/list")
    @ResponseBody
    public TableDataInfo rechargeTemplateList()
    {
        startPage();
        List<TRechargeTemplate> list = walletMapper.selectAllTemplates();
        return getDataTable(list);
    }

    @GetMapping("/rechargeTemplate/add")
    public String rechargeTemplateAdd()
    {
        return "coffee/rechargeTemplate/add";
    }

    @RequiresPermissions("coffee:rechargeTemplate:add")
    @Log(title = "充值模板", businessType = BusinessType.INSERT)
    @PostMapping("/rechargeTemplate/add")
    @ResponseBody
    public AjaxResult rechargeTemplateAddSave(TRechargeTemplate template)
    {
        // 自动计算到账总金额
        if (template.getPayAmount() != null && template.getGiftAmount() != null)
        {
            template.setTotalAmount(template.getPayAmount().add(template.getGiftAmount()));
        }
        return toAjax(walletMapper.insertTemplate(template));
    }

    @GetMapping("/rechargeTemplate/edit/{templateId}")
    public String rechargeTemplateEdit(@PathVariable Long templateId, ModelMap mmap)
    {
        TRechargeTemplate template = walletMapper.selectTemplateById(templateId);
        mmap.put("template", template);
        return "coffee/rechargeTemplate/edit";
    }

    @RequiresPermissions("coffee:rechargeTemplate:edit")
    @Log(title = "充值模板", businessType = BusinessType.UPDATE)
    @PostMapping("/rechargeTemplate/edit")
    @ResponseBody
    public AjaxResult rechargeTemplateEditSave(TRechargeTemplate template)
    {
        if (template.getPayAmount() != null && template.getGiftAmount() != null)
        {
            template.setTotalAmount(template.getPayAmount().add(template.getGiftAmount()));
        }
        return toAjax(walletMapper.updateTemplate(template));
    }

    @RequiresPermissions("coffee:rechargeTemplate:remove")
    @Log(title = "充值模板", businessType = BusinessType.DELETE)
    @PostMapping("/rechargeTemplate/remove")
    @ResponseBody
    public AjaxResult rechargeTemplateRemove(String ids)
    {
        return toAjax(walletMapper.deleteTemplateByIds(ids.split(",")));
    }

    // ===== 充值记录 =====

    @RequiresPermissions("coffee:rechargeRecord:view")
    @GetMapping("/rechargeRecord")
    public String rechargeRecord()
    {
        return "coffee/rechargeRecord/rechargeRecord";
    }

    @RequiresPermissions("coffee:rechargeRecord:list")
    @PostMapping("/rechargeRecord/list")
    @ResponseBody
    public TableDataInfo rechargeRecordList(TRechargeRecord record)
    {
        startPage();
        List<TRechargeRecord> list = walletMapper.selectAllRechargeRecords();
        return getDataTable(list);
    }

    // ===== 余额流水 =====

    @RequiresPermissions("coffee:walletLog:view")
    @GetMapping("/walletLog")
    public String walletLog()
    {
        return "coffee/walletLog/walletLog";
    }

    @RequiresPermissions("coffee:walletLog:list")
    @PostMapping("/walletLog/list")
    @ResponseBody
    public TableDataInfo walletLogList(TWalletLog log)
    {
        startPage();
        List<TWalletLog> list = walletMapper.selectAllWalletLogs();
        return getDataTable(list);
    }
}
