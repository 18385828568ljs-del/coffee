package com.ruoyi.project.coffee.decorator.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.ruoyi.project.coffee.decorator.context.TenantContextInterceptor;

@Configuration
public class DecoratorWebConfig implements WebMvcConfigurer
{
    @Autowired
    private TenantContextInterceptor tenantContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(tenantContextInterceptor)
                .addPathPatterns("/coffee/decorator/**")
                .excludePathPatterns("/coffee/decorator/context/**", "/coffee/decorator/workbench");
    }
}
