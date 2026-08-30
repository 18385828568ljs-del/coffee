package com.ruoyi.project.coffee.scanOrder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpec;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductSpecOptionMapper;
import com.ruoyi.project.coffee.scanOrder.service.impl.ScanProductServiceImpl;

class ScanProductServiceImplTest
{
    @Mock
    private ScanProductMapper scanProductMapper;

    @Mock
    private ScanProductSpecMapper scanProductSpecMapper;

    @Mock
    private ScanProductSpecOptionMapper scanProductSpecOptionMapper;

    private ScanProductServiceImpl scanProductService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        scanProductService = new ScanProductServiceImpl();
        ReflectionTestUtils.setField(scanProductService, "scanProductMapper", scanProductMapper);
        ReflectionTestUtils.setField(scanProductService, "scanProductSpecMapper", scanProductSpecMapper);
        ReflectionTestUtils.setField(scanProductService, "scanProductSpecOptionMapper", scanProductSpecOptionMapper);
    }

    @Test
    void selectScanProductWithSpecsShouldReturnNullWhenProductMissing()
    {
        when(scanProductMapper.selectScanProductById(7L)).thenReturn(null);

        ScanProduct result = scanProductService.selectScanProductWithSpecs(7L);

        assertNull(result);
        verify(scanProductSpecMapper, never()).selectSpecListByProductId(any());
        verify(scanProductSpecOptionMapper, never()).selectOptionListByProductId(any());
    }

    @Test
    void selectScanProductWithSpecsShouldAttachEmptySpecsWhenProductHasNoSpecs()
    {
        ScanProduct product = product(7L);
        when(scanProductMapper.selectScanProductById(7L)).thenReturn(product);
        when(scanProductSpecMapper.selectSpecListByProductId(7L)).thenReturn(Collections.<ScanProductSpec>emptyList());

        ScanProduct result = scanProductService.selectScanProductWithSpecs(7L);

        assertSame(product, result);
        assertNotNull(result.getSpecs());
        assertEquals(0, result.getSpecs().size());
        verify(scanProductSpecOptionMapper, never()).selectOptionListByProductId(any());
    }

    @Test
    void selectScanProductWithSpecsShouldGroupOptionsBySpecAndIgnoreInvalidOptions()
    {
        ScanProduct product = product(7L);
        ScanProductSpec temperature = spec(11L, "温度");
        ScanProductSpec sugar = spec(12L, "糖度");
        ScanProductSpec size = spec(13L, "杯型");
        when(scanProductMapper.selectScanProductById(7L)).thenReturn(product);
        when(scanProductSpecMapper.selectSpecListByProductId(7L)).thenReturn(Arrays.asList(temperature, sugar, size));
        when(scanProductSpecOptionMapper.selectOptionListByProductId(7L))
                .thenReturn(Arrays.asList(option(101L, 11L, "热"), option(102L, 11L, "冰"), option(201L, 12L, "少糖"),
                        option(301L, 99L, "无效规格"), option(401L, null, "无规格"), null));

        ScanProduct result = scanProductService.selectScanProductWithSpecs(7L);

        assertSame(product, result);
        assertEquals(Arrays.asList(temperature, sugar, size), result.getSpecs());
        assertEquals(Arrays.asList("热", "冰"), optionNames(temperature.getOptions()));
        assertEquals(Arrays.asList("少糖"), optionNames(sugar.getOptions()));
        assertNotNull(size.getOptions());
        assertEquals(0, size.getOptions().size());
    }

    @Test
    void calculatePriceBySpecJsonShouldUseCurrentLargeCupExtraPrice()
    {
        ScanProduct product = product(101L);
        product.setPrice(new BigDecimal("12.00"));
        ScanProductSpec cup = spec(2L, "杯型");
        cup.setRequired(1);
        cup.setSpecType("single");
        ScanProductSpecOption medium = option(3L, 2L, "中杯");
        medium.setProductId(101L);
        medium.setExtraPrice(BigDecimal.ZERO);
        ScanProductSpecOption large = option(4L, 2L, "大杯");
        large.setProductId(101L);
        large.setExtraPrice(new BigDecimal("3.00"));
        when(scanProductMapper.selectScanProductById(101L)).thenReturn(product);
        when(scanProductSpecMapper.selectSpecListByProductId(101L)).thenReturn(Arrays.asList(cup));
        when(scanProductSpecOptionMapper.selectOptionListByProductId(101L))
            .thenReturn(Arrays.asList(medium, large));

        BigDecimal price = scanProductService.calculatePriceBySpecJson(
            101L, "[{\"specId\":2,\"optionIds\":[4]}]");

        assertEquals(new BigDecimal("15.00"), price);
    }

    @Test
    void buildDefaultSpecJsonShouldSelectRequiredDefaultOption()
    {
        ScanProduct product = product(101L);
        ScanProductSpec cup = spec(2L, "杯型");
        cup.setRequired(1);
        cup.setSpecType("single");
        ScanProductSpecOption medium = option(3L, 2L, "中杯");
        medium.setProductId(101L);
        medium.setIsDefault(1);
        medium.setExtraPrice(BigDecimal.ZERO);
        cup.setOptions(Arrays.asList(medium));
        product.setSpecs(Arrays.asList(cup));
        when(scanProductMapper.selectScanProductById(101L)).thenReturn(product);
        when(scanProductSpecMapper.selectSpecListByProductId(101L)).thenReturn(Arrays.asList(cup));
        when(scanProductSpecOptionMapper.selectOptionListByProductId(101L))
            .thenReturn(Arrays.asList(medium));

        String specJson = scanProductService.buildDefaultSpecJson(101L);

        org.junit.jupiter.api.Assertions.assertTrue(specJson.contains("\"optionIds\":[3]"));
    }

    @Test
    void insertScanProductShouldFillDefaultStatusSortAndMonthSales()
    {
        ScanProduct product = product(7L);
        when(scanProductMapper.insertScanProduct(product)).thenReturn(1);

        int rows = scanProductService.insertScanProduct(product);

        assertEquals(1, rows);
        assertEquals(1, product.getStatus());
        assertEquals(0, product.getSortOrder());
        assertEquals(0, product.getMonthSales());
        assertNotNull(product.getCreateTime());
        verify(scanProductMapper).insertScanProduct(product);
    }

    @Test
    void insertScanProductShouldKeepExplicitStatusSortAndMonthSales()
    {
        ScanProduct product = product(7L);
        product.setStatus(0);
        product.setSortOrder(9);
        product.setMonthSales(88);
        when(scanProductMapper.insertScanProduct(product)).thenReturn(1);

        int rows = scanProductService.insertScanProduct(product);

        assertEquals(1, rows);
        assertEquals(0, product.getStatus());
        assertEquals(9, product.getSortOrder());
        assertEquals(88, product.getMonthSales());
        assertNotNull(product.getCreateTime());
    }

    @Test
    void insertCoffeeProductShouldCreateDefaultSpecsWhenNoSpecsExist()
    {
        ScanProduct product = product(7L);
        product.setProductType(ScanProduct.PRODUCT_TYPE_COFFEE);
        when(scanProductMapper.insertScanProduct(product)).thenReturn(1);
        when(scanProductSpecMapper.selectSpecListByProductId(7L)).thenReturn(Collections.<ScanProductSpec>emptyList());

        scanProductService.insertScanProduct(product);

        ArgumentCaptor<ScanProductSpec> specCaptor = ArgumentCaptor.forClass(ScanProductSpec.class);
        verify(scanProductSpecMapper, times(5)).insertSpec(specCaptor.capture());
        assertEquals(Arrays.asList("杯型", "温度", "糖度", "咖啡豆", "咖啡浓度"),
            specNames(specCaptor.getAllValues()));
        verify(scanProductSpecOptionMapper, times(13)).insertOption(any(ScanProductSpecOption.class));
    }

    @Test
    void insertDrinkProductShouldCreateOnlyDrinkSpecs()
    {
        ScanProduct product = product(8L);
        product.setProductType(ScanProduct.PRODUCT_TYPE_DRINK);
        when(scanProductMapper.insertScanProduct(product)).thenReturn(1);
        when(scanProductSpecMapper.selectSpecListByProductId(8L)).thenReturn(Collections.<ScanProductSpec>emptyList());

        scanProductService.insertScanProduct(product);

        ArgumentCaptor<ScanProductSpec> specCaptor = ArgumentCaptor.forClass(ScanProductSpec.class);
        verify(scanProductSpecMapper, times(3)).insertSpec(specCaptor.capture());
        assertEquals(Arrays.asList("杯型", "温度", "糖度"), specNames(specCaptor.getAllValues()));
        verify(scanProductSpecOptionMapper, times(7)).insertOption(any(ScanProductSpecOption.class));
    }

    @Test
    void insertFoodProductShouldNotCreateDrinkSpecs()
    {
        ScanProduct product = product(9L);
        product.setProductType(ScanProduct.PRODUCT_TYPE_FOOD);
        when(scanProductMapper.insertScanProduct(product)).thenReturn(1);
        when(scanProductSpecMapper.selectSpecListByProductId(9L)).thenReturn(Collections.<ScanProductSpec>emptyList());

        scanProductService.insertScanProduct(product);

        verify(scanProductSpecMapper, never()).insertSpec(any(ScanProductSpec.class));
        verify(scanProductSpecOptionMapper, never()).insertOption(any(ScanProductSpecOption.class));
    }

    @Test
    void updateProductShouldKeepExistingSpecs()
    {
        ScanProduct product = product(7L);
        product.setProductType(ScanProduct.PRODUCT_TYPE_COFFEE);
        when(scanProductMapper.updateScanProduct(product)).thenReturn(1);
        when(scanProductSpecMapper.selectSpecListByProductId(7L))
            .thenReturn(Arrays.asList(spec(11L, "杯型"), spec(12L, "温度"), spec(13L, "糖度"),
                spec(14L, "咖啡豆"), spec(15L, "咖啡浓度")));

        scanProductService.updateScanProduct(product);

        verify(scanProductSpecMapper, never()).insertSpec(any(ScanProductSpec.class));
        verify(scanProductSpecOptionMapper, never()).insertOption(any(ScanProductSpecOption.class));
    }

    @Test
    void updateProductShouldSaveConfiguredOptionPriceAndDefault()
    {
        ScanProduct product = product(7L);
        product.setProductType(ScanProduct.PRODUCT_TYPE_COFFEE);
        ScanProductSpec cup = spec(11L, "杯型");
        ScanProductSpecOption large = option(null, 11L, "超大杯");
        large.setExtraPrice(new BigDecimal("5.00"));
        large.setIsDefault(1);
        cup.setOptions(Arrays.asList(large));
        product.setSpecs(Arrays.asList(cup));
        when(scanProductMapper.updateScanProduct(product)).thenReturn(1);
        when(scanProductSpecMapper.selectSpecListByProductId(7L))
            .thenReturn(Arrays.asList(spec(11L, "杯型"), spec(12L, "温度"), spec(13L, "糖度"),
                spec(14L, "咖啡豆"), spec(15L, "咖啡浓度")));

        scanProductService.updateScanProduct(product);

        ArgumentCaptor<ScanProductSpecOption> optionCaptor = ArgumentCaptor.forClass(ScanProductSpecOption.class);
        verify(scanProductSpecOptionMapper, times(12)).insertOption(optionCaptor.capture());
        ScanProductSpecOption savedLarge = optionCaptor.getAllValues().get(0);
        assertEquals("超大杯", savedLarge.getOptionName());
        assertEquals(new BigDecimal("5.00"), savedLarge.getExtraPrice());
        assertEquals(1, savedLarge.getIsDefault());
    }

    @Test
    void updateProductShouldUseFirstConfiguredOptionAsDefault()
    {
        ScanProduct product = product(7L);
        product.setProductType(ScanProduct.PRODUCT_TYPE_COFFEE);
        ScanProductSpec cup = spec(11L, "杯型");
        ScanProductSpecOption first = option(null, 11L, "中杯");
        first.setIsDefault(0);
        ScanProductSpecOption second = option(null, 11L, "大杯");
        second.setIsDefault(1);
        cup.setOptions(Arrays.asList(first, second));
        product.setSpecs(Arrays.asList(cup));
        when(scanProductMapper.updateScanProduct(product)).thenReturn(1);
        when(scanProductSpecMapper.selectSpecListByProductId(7L))
            .thenReturn(Arrays.asList(spec(11L, "杯型"), spec(12L, "温度"), spec(13L, "糖度"),
                spec(14L, "咖啡豆"), spec(15L, "咖啡浓度")));

        scanProductService.updateScanProduct(product);

        ArgumentCaptor<ScanProductSpecOption> optionCaptor = ArgumentCaptor.forClass(ScanProductSpecOption.class);
        verify(scanProductSpecOptionMapper, times(13)).insertOption(optionCaptor.capture());
        assertEquals("中杯", optionCaptor.getAllValues().get(0).getOptionName());
        assertEquals(1, optionCaptor.getAllValues().get(0).getIsDefault());
        assertEquals("大杯", optionCaptor.getAllValues().get(1).getOptionName());
        assertEquals(0, optionCaptor.getAllValues().get(1).getIsDefault());
    }

    @Test
    void updateProductShouldReplaceCoffeeSpecsWhenTypeChangesToDrink()
    {
        ScanProduct product = product(7L);
        product.setProductType(ScanProduct.PRODUCT_TYPE_DRINK);
        when(scanProductMapper.updateScanProduct(product)).thenReturn(1);
        when(scanProductSpecMapper.selectSpecListByProductId(7L))
            .thenReturn(Arrays.asList(spec(11L, "杯型"), spec(12L, "温度"), spec(13L, "糖度"),
                spec(14L, "咖啡豆"), spec(15L, "咖啡浓度")));

        scanProductService.updateScanProduct(product);

        verify(scanProductSpecOptionMapper).deleteOptionByProductIds(new String[] { "7" });
        verify(scanProductSpecMapper).deleteSpecByProductIds(new String[] { "7" });
        verify(scanProductSpecMapper, times(3)).insertSpec(any(ScanProductSpec.class));
        verify(scanProductSpecOptionMapper, times(7)).insertOption(any(ScanProductSpecOption.class));
    }

    @Test
    void updateScanProductShouldFillDefaultsAndUpdateTime()
    {
        ScanProduct product = product(7L);
        when(scanProductMapper.updateScanProduct(product)).thenReturn(1);

        int rows = scanProductService.updateScanProduct(product);

        assertEquals(1, rows);
        assertEquals(1, product.getStatus());
        assertEquals(0, product.getSortOrder());
        assertEquals(0, product.getMonthSales());
        assertNotNull(product.getUpdateTime());
        verify(scanProductMapper).updateScanProduct(product);
    }

    @Test
    void deleteScanProductByIdsShouldDeleteOptionsSpecsThenProducts()
    {
        when(scanProductMapper.deleteScanProductByIds(any(String[].class))).thenReturn(2);

        int rows = scanProductService.deleteScanProductByIds("7,8");

        assertEquals(2, rows);
        ArgumentCaptor<String[]> idsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(scanProductSpecOptionMapper).deleteOptionByProductIds(idsCaptor.capture());
        assertEquals(Arrays.asList("7", "8"), Arrays.asList(idsCaptor.getValue()));
        verify(scanProductSpecMapper).deleteSpecByProductIds(idsCaptor.getValue());
        verify(scanProductMapper).deleteScanProductByIds(idsCaptor.getValue());
    }

    private static ScanProduct product(Long productId)
    {
        ScanProduct product = new ScanProduct();
        product.setProductId(productId);
        product.setProductName("测试商品");
        return product;
    }

    private static ScanProductSpec spec(Long specId, String specName)
    {
        ScanProductSpec spec = new ScanProductSpec();
        spec.setSpecId(specId);
        spec.setSpecName(specName);
        return spec;
    }

    private static ScanProductSpecOption option(Long optionId, Long specId, String optionName)
    {
        ScanProductSpecOption option = new ScanProductSpecOption();
        option.setOptionId(optionId);
        option.setSpecId(specId);
        option.setOptionName(optionName);
        return option;
    }

    private static List<String> optionNames(List<ScanProductSpecOption> options)
    {
        List<String> names = new ArrayList<String>();
        for (ScanProductSpecOption option : options)
        {
            names.add(option.getOptionName());
        }
        return names;
    }

    private static List<String> specNames(List<ScanProductSpec> specs)
    {
        List<String> names = new ArrayList<String>();
        for (ScanProductSpec spec : specs)
        {
            names.add(spec.getSpecName());
        }
        return names;
    }
}
