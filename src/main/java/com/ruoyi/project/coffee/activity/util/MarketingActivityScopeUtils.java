package com.ruoyi.project.coffee.activity.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.text.Convert;

public final class MarketingActivityScopeUtils
{
    private MarketingActivityScopeUtils()
    {
    }

    public static List<Long> parseScopeIds(String scopeIdsText)
    {
        if (StringUtils.isEmpty(scopeIdsText))
        {
            return Collections.emptyList();
        }

        String[] parts = scopeIdsText.split(",");
        List<Long> values = new ArrayList<Long>();
        for (String part : parts)
        {
            Long scopeId = Convert.toLong(StringUtils.trim(part));
            if (scopeId != null && scopeId > 0)
            {
                values.add(scopeId);
            }
        }
        return dedupeScopeIds(values);
    }

    public static List<Long> dedupeScopeIds(List<Long> scopeIds)
    {
        if (scopeIds == null || scopeIds.isEmpty())
        {
            return Collections.emptyList();
        }

        Set<Long> values = new LinkedHashSet<Long>();
        for (Long scopeId : scopeIds)
        {
            if (scopeId != null && scopeId > 0)
            {
                values.add(scopeId);
            }
        }
        return values.isEmpty() ? Collections.<Long>emptyList() : new ArrayList<Long>(values);
    }

    public static String joinScopeIds(List<Long> scopeIds)
    {
        if (scopeIds == null || scopeIds.isEmpty())
        {
            return "";
        }

        List<String> values = new ArrayList<String>();
        for (Long scopeId : scopeIds)
        {
            if (scopeId != null && scopeId > 0)
            {
                values.add(String.valueOf(scopeId));
            }
        }
        return values.isEmpty() ? "" : StringUtils.join(values, ",");
    }
}
