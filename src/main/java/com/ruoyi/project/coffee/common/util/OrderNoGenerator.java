package com.ruoyi.project.coffee.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单号生成器
 *
 * 统一生成各类订单号,格式: 前缀 + yyyyMMddHHmmss + 4位随机数
 */
public class OrderNoGenerator
{
    private static final String DATETIME_PATTERN = "yyyyMMddHHmmss";

    /**
     * 订单类型枚举
     */
    public enum OrderType
    {
        /** 商城订单(Coffee) */
        MALL("CF"),

        /** 扫码点单订单(Scan Order) */
        SCAN("SO"),

        /** 充值订单(Recharge) */
        RECHARGE("RC");

        private final String prefix;

        OrderType(String prefix)
        {
            this.prefix = prefix;
        }

        public String getPrefix()
        {
            return prefix;
        }
    }

    /**
     * 生成订单号
     *
     * @param type 订单类型
     * @return 订单号,格式如: CF20260602153045{rand}
     */
    public static String generate(OrderType type)
    {
        String timestamp = new SimpleDateFormat(DATETIME_PATTERN).format(new Date());
        int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return type.getPrefix() + timestamp + randomSuffix;
    }

    /**
     * 生成带UUID后缀的订单号(用于需要更高唯一性的场景)
     *
     * @param type 订单类型
     * @return 订单号,格式如: RC20260602153045{uuid6位}
     */
    public static String generateWithUuid(OrderType type)
    {
        String timestamp = new SimpleDateFormat(DATETIME_PATTERN).format(new Date());
        String uuidSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return type.getPrefix() + timestamp + uuidSuffix;
    }
}
