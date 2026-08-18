package com.ruoyi.project.coffee.scanOrder.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.text.Convert;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpec;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecOptionMapper;
import com.ruoyi.project.coffee.scanOrder.service.IScanProductService;

/**
 * 扫码点单-商品Service实现
 */
@Service
public class ScanProductServiceImpl implements IScanProductService
{
    @Autowired
    private ScanProductMapper scanProductMapper;

    @Autowired
    private ScanProductSpecMapper scanProductSpecMapper;

    @Autowired
    private ScanProductSpecOptionMapper scanProductSpecOptionMapper;

    @Override
    public ScanProduct selectScanProductById(Long productId)
    {
        ScanProduct product = scanProductMapper.selectScanProductById(productId);
        normalizeProductType(product);
        return product;
    }

    @Override
    public List<ScanProduct> selectScanProductList(ScanProduct scanProduct)
    {
        List<ScanProduct> products = scanProductMapper.selectScanProductList(scanProduct);
        if (products != null)
        {
            for (ScanProduct product : products)
            {
                normalizeProductType(product);
            }
        }
        return products;
    }

    @Override
    public ScanProduct selectScanProductWithSpecs(Long productId)
    {
        ScanProduct product = scanProductMapper.selectScanProductById(productId);
        if (product == null)
        {
            return null;
        }

        normalizeProductType(product);

        List<ScanProductSpec> specs = scanProductSpecMapper.selectSpecListByProductId(productId);
        if (specs == null || specs.isEmpty())
        {
            product.setSpecs(new ArrayList<ScanProductSpec>());
            return product;
        }

        List<ScanProductSpecOption> allOptions = scanProductSpecOptionMapper.selectOptionListByProductId(productId);
        Map<Long, List<ScanProductSpecOption>> optionsBySpec = new HashMap<Long, List<ScanProductSpecOption>>();
        if (allOptions != null)
        {
            for (ScanProductSpecOption option : allOptions)
            {
                if (option == null || option.getSpecId() == null)
                {
                    continue;
                }
                List<ScanProductSpecOption> bucket = optionsBySpec.get(option.getSpecId());
                if (bucket == null)
                {
                    bucket = new ArrayList<ScanProductSpecOption>();
                    optionsBySpec.put(option.getSpecId(), bucket);
                }
                bucket.add(option);
            }
        }

        for (ScanProductSpec spec : specs)
        {
            List<ScanProductSpecOption> bucket = optionsBySpec.get(spec.getSpecId());
            spec.setOptions(bucket == null ? new ArrayList<ScanProductSpecOption>() : bucket);
        }

        product.setSpecs(specs);
        return product;
    }

    @Override
    public BigDecimal calculatePriceBySpecJson(Long productId, String specJson)
    {
        ScanProduct product = scanProductMapper.selectScanProductById(productId);
        if (product == null)
        {
            throw new IllegalArgumentException("商品不存在");
        }

        BigDecimal basePrice = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        List<ScanProductSpec> specs = scanProductSpecMapper.selectSpecListByProductId(productId);
        if (specs == null)
        {
            specs = new ArrayList<ScanProductSpec>();
        }

        JSONArray selections = parseSelections(specJson);
        if (specs.isEmpty())
        {
            if (selections != null && !selections.isEmpty())
            {
                throw new IllegalArgumentException("商品没有可用规格");
            }
            return basePrice;
        }

        Map<Long, ScanProductSpec> specMap = new HashMap<Long, ScanProductSpec>();
        for (ScanProductSpec spec : specs)
        {
            if (spec != null && spec.getSpecId() != null)
            {
                specMap.put(spec.getSpecId(), spec);
            }
        }

        List<Long> optionIds = new ArrayList<Long>();
        Map<Long, Integer> selectedCountBySpec = new HashMap<Long, Integer>();
        Set<Long> seenOptionIds = new HashSet<Long>();
        if (selections != null)
        {
            for (Object value : selections)
            {
                if (!(value instanceof JSONObject))
                {
                    throw new IllegalArgumentException("规格格式错误");
                }
                JSONObject selection = (JSONObject) value;
                Long specId = selection.getLong("specId");
                JSONArray ids = selection.getJSONArray("optionIds");
                if (specId == null || !specMap.containsKey(specId) || ids == null || ids.isEmpty())
                {
                    throw new IllegalArgumentException("规格选项不能为空");
                }
                for (Object rawId : ids)
                {
                    Long optionId = rawId instanceof Number
                        ? ((Number) rawId).longValue() : parseLong(rawId == null ? null : rawId.toString());
                    if (optionId == null || optionId <= 0 || !seenOptionIds.add(optionId))
                    {
                        throw new IllegalArgumentException("规格选项格式错误");
                    }
                    optionIds.add(optionId);
                    Integer count = selectedCountBySpec.get(specId);
                    selectedCountBySpec.put(specId, count == null ? 1 : count + 1);
                }
            }
        }

        List<ScanProductSpecOption> allOptions = scanProductSpecOptionMapper.selectOptionListByProductId(productId);
        Map<Long, ScanProductSpecOption> optionMap = new HashMap<Long, ScanProductSpecOption>();
        if (allOptions != null)
        {
            for (ScanProductSpecOption option : allOptions)
            {
                if (option != null && option.getOptionId() != null)
                {
                    optionMap.put(option.getOptionId(), option);
                }
            }
        }

        BigDecimal extraPrice = BigDecimal.ZERO;
        for (Long optionId : optionIds)
        {
            ScanProductSpecOption option = optionMap.get(optionId);
            if (option == null || option.getProductId() == null || !productId.equals(option.getProductId()))
            {
                throw new IllegalArgumentException("规格选项不存在");
            }
            ScanProductSpec spec = specMap.get(option.getSpecId());
            if (spec == null)
            {
                throw new IllegalArgumentException("规格选项不属于当前商品");
            }
            Integer count = selectedCountBySpec.get(option.getSpecId());
            if (count == null)
            {
                throw new IllegalArgumentException("规格参数不一致");
            }
            if (option.getExtraPrice() != null)
            {
                extraPrice = extraPrice.add(option.getExtraPrice());
            }
        }

        for (ScanProductSpec spec : specs)
        {
            if (spec == null || spec.getSpecId() == null)
            {
                continue;
            }
            int selectedCount = selectedCountBySpec.containsKey(spec.getSpecId())
                ? selectedCountBySpec.get(spec.getSpecId()) : 0;
            if (spec.getRequired() != null && spec.getRequired() == 1 && selectedCount == 0)
            {
                throw new IllegalArgumentException("请选择" + spec.getSpecName());
            }
            if (!"multiple".equalsIgnoreCase(spec.getSpecType()) && selectedCount > 1)
            {
                throw new IllegalArgumentException(spec.getSpecName() + "只能选择一项");
            }
        }
        return basePrice.add(extraPrice);
    }

    @Override
    public String buildDefaultSpecJson(Long productId)
    {
        ScanProduct product = selectScanProductWithSpecs(productId);
        if (product == null || product.getSpecs() == null || product.getSpecs().isEmpty())
        {
            return "";
        }

        JSONArray result = new JSONArray();
        for (ScanProductSpec spec : product.getSpecs())
        {
            if (spec == null || spec.getOptions() == null || spec.getOptions().isEmpty())
            {
                continue;
            }
            ScanProductSpecOption selected = null;
            for (ScanProductSpecOption option : spec.getOptions())
            {
                if (option != null && option.getIsDefault() != null && option.getIsDefault() == 1)
                {
                    selected = option;
                    break;
                }
            }
            if (selected == null && spec.getRequired() != null && spec.getRequired() == 1)
            {
                selected = spec.getOptions().get(0);
            }
            if (selected == null || selected.getOptionId() == null)
            {
                continue;
            }

            JSONObject selection = new JSONObject();
            selection.put("specId", spec.getSpecId());
            selection.put("specName", spec.getSpecName());
            selection.put("specType", spec.getSpecType());
            selection.put("optionIds", Arrays.asList(selected.getOptionId()));
            selection.put("optionNames", Arrays.asList(selected.getOptionName()));
            result.add(selection);
        }
        return result.toJSONString();
    }

    @Override
    public String buildSpecText(Long productId, String specJson)
    {
        JSONArray selections = parseSelections(specJson);
        if (selections == null || selections.isEmpty())
        {
            return "";
        }
        ScanProduct product = selectScanProductWithSpecs(productId);
        if (product == null || product.getSpecs() == null)
        {
            return "";
        }
        Map<Long, String> optionNames = new HashMap<Long, String>();
        for (ScanProductSpec spec : product.getSpecs())
        {
            if (spec == null || spec.getOptions() == null)
            {
                continue;
            }
            for (ScanProductSpecOption option : spec.getOptions())
            {
                if (option != null && option.getOptionId() != null)
                {
                    optionNames.put(option.getOptionId(), option.getOptionName());
                }
            }
        }
        List<String> names = new ArrayList<String>();
        for (Object value : selections)
        {
            if (!(value instanceof JSONObject))
            {
                continue;
            }
            JSONArray ids = ((JSONObject) value).getJSONArray("optionIds");
            if (ids == null)
            {
                continue;
            }
            for (Object rawId : ids)
            {
                Long optionId = rawId instanceof Number
                    ? ((Number) rawId).longValue() : parseLong(rawId == null ? null : rawId.toString());
                String name = optionNames.get(optionId);
                if (name != null && !name.trim().isEmpty())
                {
                    names.add(name.trim());
                }
            }
        }
        return String.join(" / ", names);
    }

    private JSONArray parseSelections(String specJson)
    {
        if (specJson == null || specJson.trim().isEmpty())
        {
            return null;
        }
        try
        {
            JSONArray selections = JSON.parseArray(specJson);
            if (selections == null)
            {
                throw new IllegalArgumentException("规格格式错误");
            }
            return selections;
        }
        catch (IllegalArgumentException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("规格格式错误", e);
        }
    }

    private Long parseLong(String value)
    {
        if (value == null || value.trim().isEmpty())
        {
            return null;
        }
        try
        {
            return Long.valueOf(value.trim());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertScanProduct(ScanProduct scanProduct)
    {
        normalizeProductType(scanProduct);
        if (scanProduct.getStatus() == null)
        {
            scanProduct.setStatus(1);
        }
        if (scanProduct.getSortOrder() == null)
        {
            scanProduct.setSortOrder(0);
        }
        if (scanProduct.getMonthSales() == null)
        {
            scanProduct.setMonthSales(0);
        }
        scanProduct.setCreateTime(DateUtils.getNowDate());
        int rows = scanProductMapper.insertScanProduct(scanProduct);
        if (rows > 0)
        {
            ensureDefaultSpecs(scanProduct);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateScanProduct(ScanProduct scanProduct)
    {
        normalizeProductType(scanProduct);
        if (scanProduct.getStatus() == null)
        {
            scanProduct.setStatus(1);
        }
        if (scanProduct.getSortOrder() == null)
        {
            scanProduct.setSortOrder(0);
        }
        if (scanProduct.getMonthSales() == null)
        {
            scanProduct.setMonthSales(0);
        }
        scanProduct.setUpdateTime(DateUtils.getNowDate());
        int rows = scanProductMapper.updateScanProduct(scanProduct);
        if (rows > 0)
        {
            if (scanProduct.getSpecs() != null)
            {
                replaceConfiguredSpecs(scanProduct, scanProduct.getSpecs());
            }
            else
            {
                ensureDefaultSpecs(scanProduct);
            }
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteScanProductByIds(String ids)
    {
        String[] productIds = Convert.toStrArray(ids);
        scanProductSpecOptionMapper.deleteOptionByProductIds(productIds);
        scanProductSpecMapper.deleteSpecByProductIds(productIds);
        return scanProductMapper.deleteScanProductByIds(productIds);
    }

    private void normalizeProductType(ScanProduct product)
    {
        if (product == null)
        {
            return;
        }
        String type = product.getProductType();
        if (type == null || type.trim().isEmpty())
        {
            product.setProductType(ScanProduct.PRODUCT_TYPE_DRINK);
            return;
        }
        type = type.trim().toUpperCase(Locale.ROOT);
        if (!ScanProduct.PRODUCT_TYPE_COFFEE.equals(type)
            && !ScanProduct.PRODUCT_TYPE_DRINK.equals(type)
            && !ScanProduct.PRODUCT_TYPE_FOOD.equals(type))
        {
            throw new ServiceException("商品类型只能是咖啡、普通饮料或小食");
        }
        product.setProductType(type);
    }

    private void ensureDefaultSpecs(ScanProduct product)
    {
        if (product == null || product.getProductId() == null)
        {
            return;
        }
        List<String> expectedNames = expectedSpecNames(product.getProductType());

        List<ScanProductSpec> existingSpecs = scanProductSpecMapper.selectSpecListByProductId(product.getProductId());
        if (hasExpectedSpecGroups(existingSpecs, expectedNames))
        {
            return;
        }
        if (existingSpecs != null && !existingSpecs.isEmpty())
        {
            String[] productIds = new String[] { String.valueOf(product.getProductId()) };
            scanProductSpecOptionMapper.deleteOptionByProductIds(productIds);
            scanProductSpecMapper.deleteSpecByProductIds(productIds);
        }
        if (expectedNames.isEmpty())
        {
            return;
        }

        for (String expectedName : expectedNames)
        {
            createDefaultSpecByName(product, expectedName);
        }
    }

    private boolean hasExpectedSpecGroups(List<ScanProductSpec> existingSpecs, List<String> expectedNames)
    {
        if (existingSpecs == null || existingSpecs.size() != expectedNames.size())
        {
            return expectedNames.isEmpty() && (existingSpecs == null || existingSpecs.isEmpty());
        }
        for (String expectedName : expectedNames)
        {
            boolean found = false;
            for (ScanProductSpec existingSpec : existingSpecs)
            {
                if (existingSpec != null && expectedName.equals(existingSpec.getSpecName()))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                return false;
            }
        }
        return true;
    }

    private void createDefaultSpec(ScanProduct product, String name, int required, List<String> optionNames,
        List<BigDecimal> extraPrices, int defaultIndex)
    {
        ScanProductSpec spec = new ScanProductSpec();
        spec.setProductId(product.getProductId());
        spec.setSpecName(name);
        spec.setSpecType("single");
        spec.setRequired(required);
        spec.setSortOrder(0);
        spec.setCreateTime(DateUtils.getNowDate());
        scanProductSpecMapper.insertSpec(spec);

        for (int i = 0; i < optionNames.size(); i++)
        {
            ScanProductSpecOption option = new ScanProductSpecOption();
            option.setSpecId(spec.getSpecId());
            option.setProductId(product.getProductId());
            option.setOptionName(optionNames.get(i));
            option.setExtraPrice(extraPrices.get(i));
            option.setIsDefault(i == defaultIndex ? 1 : 0);
            option.setSortOrder(i + 1);
            option.setCreateTime(DateUtils.getNowDate());
            scanProductSpecOptionMapper.insertOption(option);
        }
    }

    /**
     * 保存后台编辑的规格选项。规格组由商品类型决定，选项名称和加价由商家维护；每组第一项为默认项。
     */
    private void replaceConfiguredSpecs(ScanProduct product, List<ScanProductSpec> configuredSpecs)
    {
        List<String> expectedNames = expectedSpecNames(product.getProductType());
        String[] productIds = new String[] { String.valueOf(product.getProductId()) };
        scanProductSpecOptionMapper.deleteOptionByProductIds(productIds);
        scanProductSpecMapper.deleteSpecByProductIds(productIds);

        for (int i = 0; i < expectedNames.size(); i++)
        {
            String expectedName = expectedNames.get(i);
            ScanProductSpec configured = findSpecByName(configuredSpecs, expectedName);
            List<ScanProductSpecOption> options = configured == null ? null : configured.getOptions();
            if (options == null || options.isEmpty())
            {
                createDefaultSpecByName(product, expectedName);
                continue;
            }

            List<ScanProductSpecOption> validOptions = new ArrayList<ScanProductSpecOption>();
            for (ScanProductSpecOption option : options)
            {
                if (option != null && option.getOptionName() != null
                    && !option.getOptionName().trim().isEmpty())
                {
                    validOptions.add(option);
                }
            }
            if (validOptions.isEmpty())
            {
                createDefaultSpecByName(product, expectedName);
                continue;
            }

            ScanProductSpec spec = new ScanProductSpec();
            spec.setProductId(product.getProductId());
            spec.setSpecName(expectedName);
            spec.setSpecType("single");
            spec.setRequired(1);
            spec.setSortOrder(i + 1);
            spec.setCreateTime(DateUtils.getNowDate());
            scanProductSpecMapper.insertSpec(spec);

            int optionIndex = 0;
            for (ScanProductSpecOption configuredOption : validOptions)
            {
                ScanProductSpecOption option = new ScanProductSpecOption();
                option.setSpecId(spec.getSpecId());
                option.setProductId(product.getProductId());
                option.setOptionName(configuredOption.getOptionName().trim());
                BigDecimal extraPrice = configuredOption.getExtraPrice();
                option.setExtraPrice(extraPrice == null || extraPrice.signum() < 0 ? BigDecimal.ZERO : extraPrice);
                option.setIsDefault(optionIndex == 0 ? 1 : 0);
                option.setSortOrder(optionIndex + 1);
                option.setCreateTime(DateUtils.getNowDate());
                scanProductSpecOptionMapper.insertOption(option);
                optionIndex++;
            }
        }
    }

    private List<String> expectedSpecNames(String productType)
    {
        List<String> names = new ArrayList<String>();
        if (!ScanProduct.PRODUCT_TYPE_FOOD.equals(productType))
        {
            names.add("杯型");
            names.add("温度");
            names.add("糖度");
            if (ScanProduct.PRODUCT_TYPE_COFFEE.equals(productType))
            {
                names.add("咖啡豆");
                names.add("咖啡浓度");
            }
        }
        return names;
    }

    private ScanProductSpec findSpecByName(List<ScanProductSpec> specs, String name)
    {
        if (specs != null)
        {
            for (ScanProductSpec spec : specs)
            {
                if (spec != null && name.equals(spec.getSpecName()))
                {
                    return spec;
                }
            }
        }
        return null;
    }

    private void createDefaultSpecByName(ScanProduct product, String name)
    {
        if ("杯型".equals(name))
        {
            createDefaultSpec(product, name, 1, Arrays.asList("中杯", "大杯"),
                Arrays.asList(BigDecimal.ZERO, new BigDecimal("3.00")), 0);
        }
        else if ("温度".equals(name))
        {
            createDefaultSpec(product, name, 1, Arrays.asList("热", "冰"),
                Arrays.asList(BigDecimal.ZERO, BigDecimal.ZERO), 0);
        }
        else if ("糖度".equals(name))
        {
            createDefaultSpec(product, name, 1, Arrays.asList("无糖", "少糖", "正常糖"),
                Arrays.asList(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), 2);
        }
        else if ("咖啡豆".equals(name))
        {
            createDefaultSpec(product, name, 1, Arrays.asList("门店拼配豆", "埃塞俄比亚", "哥伦比亚"),
                Arrays.asList(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), 0);
        }
        else if ("咖啡浓度".equals(name))
        {
            createDefaultSpec(product, name, 1, Arrays.asList("标准", "加一份浓缩", "加两份浓缩"),
                Arrays.asList(BigDecimal.ZERO, new BigDecimal("5.00"), new BigDecimal("10.00")), 0);
        }
    }
}
