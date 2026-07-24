package com.ruoyi.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 兼容小程序端残留的 undefined/null 字符串参数，避免请求在 Spring 参数绑定阶段直接失败。
 */
@Configuration
public class RequestParamSanitizerConfig implements WebMvcConfigurer
{
    @Override
    public void addFormatters(FormatterRegistry registry)
    {
        registry.addConverter(new SafeStringToLongConverter());
    }

    private static class SafeStringToLongConverter implements Converter<String, Long>
    {
        @Override
        public Long convert(String source)
        {
            if (source == null)
            {
                return null;
            }

            String value = source.trim();
            if (value.isEmpty()
                || "undefined".equalsIgnoreCase(value)
                || "null".equalsIgnoreCase(value))
            {
                return null;
            }

            return Long.valueOf(value);
        }
    }
}
