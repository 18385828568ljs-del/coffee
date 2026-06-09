package com.ruoyi.project.coffee.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit
{
    /**
     * 限流key前缀
     */
    String key() default "rate_limit";

    /**
     * 时间窗口（秒）
     */
    int time() default 60;

    /**
     * 时间窗口内最大请求次数
     */
    int count() default 10;

    /**
     * 限流类型（默认按用户限流）
     */
    LimitType limitType() default LimitType.USER;

    public enum LimitType
    {
        /**
         * 按用户限流
         */
        USER,

        /**
         * 按IP限流
         */
        IP,

        /**
         * 全局限流
         */
        GLOBAL
    }
}
