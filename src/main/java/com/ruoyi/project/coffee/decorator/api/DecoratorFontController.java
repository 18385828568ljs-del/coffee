package com.ruoyi.project.coffee.decorator.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.decorator.context.TenantContextHolder;
import com.ruoyi.project.coffee.decorator.font.DecoratorFontService;

@RestController
@RequestMapping("/coffee/decorator/fonts")
public class DecoratorFontController
{
    @Autowired
    private DecoratorFontService fontService;

    @GetMapping
    public AjaxResult list()
    {
        return AjaxResult.success(fontService.list(TenantContextHolder.require()));
    }
}
