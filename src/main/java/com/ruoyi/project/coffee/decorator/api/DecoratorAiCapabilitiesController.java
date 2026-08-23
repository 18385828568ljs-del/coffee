package com.ruoyi.project.coffee.decorator.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.decorator.ai.profile.ComponentAiProfileService;
import com.ruoyi.project.coffee.decorator.context.DecoratorPermission;
import com.ruoyi.project.coffee.decorator.context.TenantContextHolder;
import com.ruoyi.project.coffee.decorator.context.TenantContextService;

@RestController
@RequestMapping("/coffee/decorator/ai")
public class DecoratorAiCapabilitiesController
{
    @Autowired private ComponentAiProfileService profileService;
    @Autowired private TenantContextService contextService;

    @GetMapping("/components")
    public AjaxResult components()
    {
        contextService.requirePermission(TenantContextHolder.require(), DecoratorPermission.ASSET_VIEW);
        return AjaxResult.success(profileService.capabilities());
    }

    @GetMapping("/components/{slotKey}/capabilities")
    public AjaxResult capabilities(@PathVariable String slotKey)
    {
        contextService.requirePermission(TenantContextHolder.require(), DecoratorPermission.ASSET_VIEW);
        return AjaxResult.success(profileService.capabilities(slotKey));
    }
}
