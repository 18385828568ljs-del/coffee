package com.ruoyi.project.abucoder.wxuser.controller;

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
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.abucoder.wxuser.service.IAbucoderWxuserService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 微信用户Controller
 * 
 * @author ruoyi
 * @date 2022-05-27
 */
@Controller
@RequestMapping("/abucoder/wxuser")
public class AbucoderWxuserController extends BaseController
{
    private String prefix = "abucoder/wxuser";

    @Autowired
    private IAbucoderWxuserService abucoderWxuserService;

    @RequiresPermissions("abucoder:wxuser:view")
    @GetMapping()
    public String wxuser()
    {
        return prefix + "/wxuser";
    }

    /**
     * 查询微信用户列表
     */
    @RequiresPermissions("abucoder:wxuser:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(AbucoderWxuser abucoderWxuser)
    {
        startPage();
        List<AbucoderWxuser> list = abucoderWxuserService.selectAbucoderWxuserList(abucoderWxuser);
        return getDataTable(list);
    }

    /**
     * 导出微信用户列表
     */
    @RequiresPermissions("abucoder:wxuser:export")
    @Log(title = "微信用户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(AbucoderWxuser abucoderWxuser)
    {
        List<AbucoderWxuser> list = abucoderWxuserService.selectAbucoderWxuserList(abucoderWxuser);
        ExcelUtil<AbucoderWxuser> util = new ExcelUtil<AbucoderWxuser>(AbucoderWxuser.class);
        return util.exportExcel(list, "微信用户数据");
    }

    /**
     * 新增微信用户
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存微信用户
     */
    @RequiresPermissions("abucoder:wxuser:add")
    @Log(title = "微信用户", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(AbucoderWxuser abucoderWxuser)
    {
        return toAjax(abucoderWxuserService.insertAbucoderWxuser(abucoderWxuser));
    }

    /**
     * 修改微信用户
     */
    @RequiresPermissions("abucoder:wxuser:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap)
    {
        AbucoderWxuser abucoderWxuser = abucoderWxuserService.selectAbucoderWxuserById(id);
        mmap.put("abucoderWxuser", abucoderWxuser);
        return prefix + "/edit";
    }

    /**
     * 修改保存微信用户
     */
    @RequiresPermissions("abucoder:wxuser:edit")
    @Log(title = "微信用户", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(AbucoderWxuser abucoderWxuser)
    {
        return toAjax(abucoderWxuserService.updateAbucoderWxuser(abucoderWxuser));
    }

    /**
     * 删除微信用户
     */
    @RequiresPermissions("abucoder:wxuser:remove")
    @Log(title = "微信用户", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(abucoderWxuserService.deleteAbucoderWxuserByIds(ids));
    }
}
