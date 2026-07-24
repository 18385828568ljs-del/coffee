package com.ruoyi.project.coffee.image.controller;

import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.image.domain.ImageAiApplyRequest;
import com.ruoyi.project.coffee.image.domain.ImageAiBatchStartRequest;
import com.ruoyi.project.coffee.image.domain.ImageAiBatchStatusRequest;
import com.ruoyi.project.coffee.image.domain.ImageAiBatchStatusResult;
import com.ruoyi.project.coffee.image.domain.ImageAiGenerateRequest;
import com.ruoyi.project.coffee.image.domain.ImageAiGenerateResult;
import com.ruoyi.project.coffee.image.service.ImageAiBatchService;
import com.ruoyi.project.coffee.image.service.ImageAiService;

@Controller
@RequestMapping("/coffee/imageAi")
public class ImageAiController
{
    private final ImageAiService imageAiService;

    private final ImageAiBatchService imageAiBatchService;

    public ImageAiController(ImageAiService imageAiService, ImageAiBatchService imageAiBatchService)
    {
        this.imageAiService = imageAiService;
        this.imageAiBatchService = imageAiBatchService;
    }

    @RequiresPermissions(value = { "coffee:product:edit", "coffee:scanProduct:edit" }, logical = Logical.OR)
    @PostMapping("/generate")
    @ResponseBody
    public AjaxResult generate(@RequestBody ImageAiGenerateRequest request)
    {
        checkProductPermission(request.getProductType());
        ImageAiGenerateResult result = imageAiService.generate(
            request.getProductType(),
            request.getProductId(),
            request.getImageUrl(),
            request.getPrompt());
        return AjaxResult.success(result)
            .put("originalUrl", result.getOriginalUrl())
            .put("generatedUrl", result.getGeneratedUrl());
    }

    @RequiresPermissions(value = { "coffee:product:edit", "coffee:scanProduct:edit" }, logical = Logical.OR)
    @PostMapping("/apply")
    @ResponseBody
    public AjaxResult apply(@RequestBody ImageAiApplyRequest request)
    {
        checkProductPermission(request.getProductType());
        return AjaxResult.success().put("rows", imageAiService.apply(
            request.getProductType(),
            request.getProductId(),
            request.getGeneratedUrl()));
    }

    @RequiresPermissions(value = { "coffee:product:edit", "coffee:scanProduct:edit" }, logical = Logical.OR)
    @PostMapping("/batch/start")
    @ResponseBody
    public AjaxResult batchStart(@RequestBody ImageAiBatchStartRequest request)
    {
        checkProductPermission(request.getProductType());
        ImageAiBatchStatusResult result = imageAiBatchService.start(
            request.getProductType(),
            request.getPrompt(),
            request.getItems());
        return toBatchAjaxResult(result);
    }

    @RequiresPermissions(value = { "coffee:product:edit", "coffee:scanProduct:edit" }, logical = Logical.OR)
    @PostMapping("/batch/status")
    @ResponseBody
    public AjaxResult batchStatus(@RequestBody ImageAiBatchStatusRequest request)
    {
        ImageAiBatchStatusResult result = imageAiBatchService.status(request.getBatchId());
        checkProductPermission(result.getProductType());
        return toBatchAjaxResult(result);
    }

    private AjaxResult toBatchAjaxResult(ImageAiBatchStatusResult result)
    {
        return AjaxResult.success(result)
            .put("batchId", result.getBatchId())
            .put("productType", result.getProductType())
            .put("running", result.isRunning())
            .put("total", result.getTotal())
            .put("completed", result.getCompleted())
            .put("success", result.getSuccess())
            .put("failed", result.getFailed())
            .put("items", result.getItems());
    }

    private void checkProductPermission(String productType)
    {
        if (ImageAiService.PRODUCT_TYPE_MALL.equals(productType)
            && !SecurityUtils.getSubject().isPermitted("coffee:product:edit"))
        {
            throw new org.apache.shiro.authz.AuthorizationException("没有商城商品修改权限");
        }
        if (ImageAiService.PRODUCT_TYPE_SCAN.equals(productType)
            && !SecurityUtils.getSubject().isPermitted("coffee:scanProduct:edit"))
        {
            throw new org.apache.shiro.authz.AuthorizationException("没有点单商品修改权限");
        }
    }
}
