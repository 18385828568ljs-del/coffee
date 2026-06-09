package com.ruoyi.project.coffee.common.util;

/**
 * 字符串处理工具类
 *
 * 补充框架StringUtils的常用方法
 */
public class StringHelper
{
    /**
     * 去除首尾空格,空字符串转为null
     *
     * @param str 待处理字符串
     * @return 处理后的字符串,空白字符串返回null
     */
    public static String trimToNull(String str)
    {
        if (str == null)
        {
            return null;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 判断字符串是否为空或仅包含空白字符
     *
     * @param str 待判断字符串
     * @return true=空白, false=非空
     */
    public static boolean isEffectivelyEmpty(String str)
    {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否有实际内容(非空且非空白)
     *
     * @param str 待判断字符串
     * @return true=有内容, false=空白
     */
    public static boolean isNotEffectivelyEmpty(String str)
    {
        return !isEffectivelyEmpty(str);
    }

    /**
     * 批量转换:空白字符串转null
     *
     * @param strings 字符串数组
     * @return 转换后的数组
     */
    public static String[] trimToNull(String... strings)
    {
        if (strings == null)
        {
            return null;
        }
        String[] result = new String[strings.length];
        for (int i = 0; i < strings.length; i++)
        {
            result[i] = trimToNull(strings[i]);
        }
        return result;
    }
}
