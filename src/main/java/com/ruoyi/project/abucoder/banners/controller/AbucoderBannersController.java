package com.ruoyi.project.abucoder.banners.controller;

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
import com.ruoyi.project.abucoder.banners.domain.AbucoderBanners;
import com.ruoyi.project.abucoder.banners.service.IAbucoderBannersService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 轮播图Controller
 * 
 * @author 阿卜 QQ932696181
 * @date 2022-05-27
 */
@Controller
@RequestMapping("/abucoder/banners")
public class AbucoderBannersController extends BaseController
{
    private String prefix = "abucoder/banners";

    @Autowired
    private IAbucoderBannersService abucoderBannersService;

    @RequiresPermissions("abucoder:banners:view")
    @GetMapping()
    public String banners()
    {
        return prefix + "/banners";
    }

    /**
     * 查询轮播图列表
     */
    @RequiresPermissions("abucoder:banners:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(AbucoderBanners abucoderBanners)
    {
        startPage();
        List<AbucoderBanners> list = abucoderBannersService.selectAbucoderBannersList(abucoderBanners);
        return getDataTable(list);
    }

    /**
     * 导出轮播图列表
     */
    @RequiresPermissions("abucoder:banners:export")
    @Log(title = "轮播图", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(AbucoderBanners abucoderBanners)
    {
        List<AbucoderBanners> list = abucoderBannersService.selectAbucoderBannersList(abucoderBanners);
        ExcelUtil<AbucoderBanners> util = new ExcelUtil<AbucoderBanners>(AbucoderBanners.class);
        return util.exportExcel(list, "轮播图数据");
    }

    /**
     * 新增轮播图
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存轮播图
     */
    @RequiresPermissions("abucoder:banners:add")
    @Log(title = "轮播图", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(AbucoderBanners abucoderBanners)
    {
        return toAjax(abucoderBannersService.insertAbucoderBanners(abucoderBanners));
    }

    /**
     * 修改轮播图
     */
    @RequiresPermissions("abucoder:banners:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap)
    {
        AbucoderBanners abucoderBanners = abucoderBannersService.selectAbucoderBannersById(id);
        mmap.put("abucoderBanners", abucoderBanners);
        return prefix + "/edit";
    }

    /**
     * 修改保存轮播图
     */
    @RequiresPermissions("abucoder:banners:edit")
    @Log(title = "轮播图", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(AbucoderBanners abucoderBanners)
    {
        return toAjax(abucoderBannersService.updateAbucoderBanners(abucoderBanners));
    }

    /**
     * 删除轮播图
     */
    @RequiresPermissions("abucoder:banners:remove")
    @Log(title = "轮播图", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(abucoderBannersService.deleteAbucoderBannersByIds(ids));
    }
}
