package com.ruoyi.project.coffee.decorator.api;

import java.net.URI;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/coffee/decorator")
public class DecoratorWorkbenchController
{
    @Value("${decorator.preview.h5-url:}")
    private String h5PreviewUrl;

    @GetMapping("/workbench")
    @RequiresPermissions("coffee:decorator:view")
    public String workbench(ModelMap model)
    {
        model.put("h5PreviewUrl", safePreviewUrl(h5PreviewUrl));
        return "coffee/decorator/workbench";
    }

    private String safePreviewUrl(String configuredUrl)
    {
        String value = configuredUrl == null ? "" : configuredUrl.trim();
        if (value.isEmpty()) return "";
        try
        {
            URI uri = URI.create(value);
            if (!uri.isAbsolute()) return value.startsWith("/") && !value.startsWith("//") ? value : "";
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme) ? value : "";
        }
        catch (IllegalArgumentException e)
        {
            return "";
        }
    }
}
