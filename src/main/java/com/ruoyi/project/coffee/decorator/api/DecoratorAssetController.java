package com.ruoyi.project.coffee.decorator.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.decorator.asset.DecoratorAssetService;
import com.ruoyi.project.coffee.decorator.context.TenantContextHolder;

@RestController
@RequestMapping("/coffee/decorator/assets")
public class DecoratorAssetController
{
    @Autowired
    private DecoratorAssetService assetService;

    @GetMapping
    public AjaxResult list(@RequestParam(required = false) String assetType)
    {
        return AjaxResult.success(assetService.list(TenantContextHolder.require(), assetType));
    }

    @PostMapping
    public AjaxResult upload(@RequestParam MultipartFile file, @RequestParam String assetType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String slotKey,
            @RequestParam(required = false) Long storeId)
    {
        return AjaxResult.success(assetService.upload(TenantContextHolder.require(), assetType, name, storeId, slotKey, file));
    }

    @DeleteMapping("/{assetId}")
    public AjaxResult archive(@PathVariable Long assetId)
    {
        assetService.archive(TenantContextHolder.require(), assetId);
        return AjaxResult.success();
    }

    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleService(ServiceException exception)
    {
        return AjaxResult.error(exception.getMessage());
    }
}
