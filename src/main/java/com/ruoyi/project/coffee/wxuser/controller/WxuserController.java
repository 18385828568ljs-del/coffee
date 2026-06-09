package com.ruoyi.project.coffee.wxuser.controller;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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
 * @author 阿卜 QQ932696181
 * @date 2026-03-12
 */
@Controller
@RequestMapping("/coffee/wxuser")
public class WxuserController extends BaseController
{
    private String prefix = "coffee/wxuser";

    @Autowired
    private IAbucoderWxuserService abucoderWxuserService;

    @RequiresPermissions("coffee:wxuser:view")
    @GetMapping()
    public String wxuser()
    {
        return prefix + "/wxuser";
    }

    /**
     * 查询微信用户列表
     */
    @RequiresPermissions("coffee:wxuser:query")
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
    @RequiresPermissions("coffee:wxuser:export")
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
     * 删除微信用户
     */
    @RequiresPermissions("coffee:wxuser:remove")
    @Log(title = "微信用户", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(abucoderWxuserService.deleteAbucoderWxuserByIds(ids));
    }
}
