package com.ruoyi.project.coffee.profile.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.project.coffee.profile.domain.ProfileEvidence;
import com.ruoyi.project.coffee.profile.domain.ProfileTag;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpec;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;

/** Builds the same stable tags for profile evidence and recommendation candidates. */
public final class ProfileTagUtils
{
    private ProfileTagUtils() { }

    public static List<ProfileTag> mall(ProfileEvidence evidence)
    {
        return mall(evidence.getProductId(), evidence.getProductName(), evidence.getCategoryId(),
            evidence.getCategoryName(), evidence.getOrigin(), evidence.getProcessingMethod(),
            evidence.getRoastLevel(), evidence.getFlavorNotes());
    }

    public static List<ProfileTag> mall(TProduct product)
    {
        return mall(product.getProductId(), product.getProductName(), product.getCategoryId(),
            null, product.getOrigin(), product.getProcessingMethod(), product.getRoastLevel(),
            product.getFlavorNotes());
    }

    private static List<ProfileTag> mall(Long productId, String productName, Long categoryId,
        String categoryName, String origin, String processingMethod, String roastLevel, String flavorNotes)
    {
        List<ProfileTag> tags = new ArrayList<ProfileTag>();
        add(tags, "product", productId, productName);
        add(tags, "category", categoryId, categoryName == null ? String.valueOf(categoryId) : categoryName);
        add(tags, "origin", origin);
        add(tags, "processing", processingMethod);
        add(tags, "roast", roastLevel);
        addSplit(tags, "flavor", flavorNotes);
        return tags;
    }

    public static List<ProfileTag> scanBase(ProfileEvidence evidence)
    {
        return scanBase(evidence.getProductId(), evidence.getProductName(), evidence.getCategoryId(),
            evidence.getCategoryName(), evidence.getProductType());
    }

    public static List<ProfileTag> scanBase(ScanProduct product)
    {
        return scanBase(product.getProductId(), product.getProductName(), product.getCategoryId(),
            null, product.getProductType());
    }

    private static List<ProfileTag> scanBase(Long productId, String productName, Long categoryId,
        String categoryName, String productType)
    {
        List<ProfileTag> tags = new ArrayList<ProfileTag>();
        add(tags, "product", productId, productName);
        add(tags, "category", categoryId, categoryName == null ? String.valueOf(categoryId) : categoryName);
        add(tags, "type", productType);
        return tags;
    }

    public static List<ProfileTag> scanSpecs(List<ScanProductSpec> specs)
    {
        if (specs == null || specs.isEmpty())
        {
            return Collections.emptyList();
        }
        List<ProfileTag> tags = new ArrayList<ProfileTag>();
        for (ScanProductSpec spec : specs)
        {
            if (spec == null || spec.getOptions() == null)
            {
                continue;
            }
            String dimension = dimension(spec.getSpecName());
            if (dimension == null)
            {
                continue;
            }
            for (ScanProductSpecOption option : spec.getOptions())
            {
                if (option != null)
                {
                    add(tags, dimension, option.getOptionName());
                }
            }
        }
        return tags;
    }

    public static List<ProfileTag> selectedScanSpecs(String specJson)
    {
        if (specJson == null || specJson.trim().isEmpty())
        {
            return Collections.emptyList();
        }
        try
        {
            JSONArray selections = JSON.parseArray(specJson);
            if (selections == null)
            {
                return Collections.emptyList();
            }
            List<ProfileTag> tags = new ArrayList<ProfileTag>();
            for (Object raw : selections)
            {
                if (!(raw instanceof JSONObject))
                {
                    continue;
                }
                JSONObject selection = (JSONObject) raw;
                String dimension = dimension(selection.getString("specName"));
                JSONArray names = selection.getJSONArray("optionNames");
                if (dimension == null || names == null)
                {
                    continue;
                }
                for (Object name : names)
                {
                    add(tags, dimension, name == null ? null : name.toString());
                }
            }
            return tags;
        }
        catch (RuntimeException ignored)
        {
            return Collections.emptyList();
        }
    }

    private static String dimension(String specName)
    {
        if (specName == null)
        {
            return null;
        }
        String value = specName.trim().toLowerCase();
        if (value.contains("杯") || value.contains("cup")) return "cup";
        if (value.contains("温") || value.contains("temperature")) return "temperature";
        if (value.contains("豆") || value.contains("bean")) return "bean";
        if (value.contains("浓") || value.contains("strength")) return "strength";
        if (value.contains("糖") || value.contains("sugar")) return "sugar";
        return null;
    }

    private static void addSplit(List<ProfileTag> tags, String dimension, String value)
    {
        if (value == null) return;
        for (String part : value.split("[,，/、]"))
        {
            add(tags, dimension, part);
        }
    }

    private static void add(List<ProfileTag> tags, String dimension, Long id, String name)
    {
        if (id != null)
        {
            add(tags, dimension, String.valueOf(id), name);
        }
    }

    private static void add(List<ProfileTag> tags, String dimension, String value)
    {
        add(tags, dimension, value, value);
    }

    private static void add(List<ProfileTag> tags, String dimension, String value, String name)
    {
        if (dimension == null || value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim()))
        {
            return;
        }
        String normalized = value.trim();
        tags.add(new ProfileTag(dimension + ":" + normalized, dimension,
            name == null || name.trim().isEmpty() ? normalized : name.trim()));
    }
}
