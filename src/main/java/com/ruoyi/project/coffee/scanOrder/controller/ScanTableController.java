package com.ruoyi.project.coffee.scanOrder.controller;

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
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;
import com.ruoyi.project.coffee.scanOrder.service.IScanTableQrcodeService;

@Controller
@RequestMapping("/coffee/scanTable")
public class ScanTableController extends BaseController
{
    private String prefix = "coffee/scanTable";

    @Autowired
    private IScanTableQrcodeService scanTableQrcodeService;

    @RequiresPermissions("coffee:scanTable:view")
    @GetMapping()
    public String scanTable()
    {
        return prefix + "/scanTable";
    }

    @RequiresPermissions("coffee:scanTable:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ScanTableQrcode scanTableQrcode)
    {
        startPage();
        List<ScanTableQrcode> list = scanTableQrcodeService.selectList(scanTableQrcode);
        return getDataTable(list);
    }

    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @RequiresPermissions("coffee:scanTable:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(ScanTableQrcode scanTableQrcode)
    {
        if (scanTableQrcode.getTableId() != null)
        {
            scanTableQrcode.setUpdateBy(getLoginName());
            return toAjax(scanTableQrcodeService.updateScanTableQrcode(scanTableQrcode));
        }
        scanTableQrcode.setCreateBy(getLoginName());
        return toAjax(scanTableQrcodeService.insertScanTableQrcode(scanTableQrcode));
    }

    @RequiresPermissions("coffee:scanTable:edit")
    @GetMapping("/edit/{tableId}")
    public String edit(@PathVariable("tableId") Long tableId, ModelMap mmap)
    {
        mmap.put("scanTable", scanTableQrcodeService.selectById(tableId));
        return prefix + "/edit";
    }

    @RequiresPermissions("coffee:scanTable:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(ScanTableQrcode scanTableQrcode)
    {
        scanTableQrcode.setUpdateBy(getLoginName());
        return toAjax(scanTableQrcodeService.updateScanTableQrcode(scanTableQrcode));
    }

    @RequiresPermissions("coffee:scanTable:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(scanTableQrcodeService.deleteScanTableQrcodeByIds(ids));
    }
}
