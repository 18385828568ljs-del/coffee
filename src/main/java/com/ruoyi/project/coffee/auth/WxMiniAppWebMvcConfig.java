package com.ruoyi.project.coffee.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 小程序接口鉴权配置。
 */
@Configuration
public class WxMiniAppWebMvcConfig implements WebMvcConfigurer
{
    @Autowired
    private WxUserAuthInterceptor wxUserAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(wxUserAuthInterceptor)
            .addPathPatterns("/api/cart/**", "/api/order/**", "/api/address/**", "/api/activity/preview",
                "/api/scanCart/**", "/api/scanOrder/**", "/api/wallet/**", "/api/member/**",
                "/api/offlineActivity/signup", "/api/offlineActivity/cancel", "/api/offlineActivity/my");
    }
}
