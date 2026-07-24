package com.ruoyi.project.coffee.support;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public final class H2MysqlFunctions
{
    private H2MysqlFunctions()
    {
    }

    public static String dateFormat(Timestamp value, String pattern)
    {
        if (value == null)
        {
            return null;
        }
        String javaPattern = pattern == null ? "yyyy-MM-dd" : pattern
            .replace("%Y", "yyyy")
            .replace("%m", "MM")
            .replace("%d", "dd")
            .replace("%H", "HH")
            .replace("%i", "mm")
            .replace("%s", "ss");
        return new SimpleDateFormat(javaPattern).format(value);
    }
}
