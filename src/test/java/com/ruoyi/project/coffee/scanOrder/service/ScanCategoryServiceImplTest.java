package com.ruoyi.project.coffee.scanOrder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCategory;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanCategoryMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanProductMapper;
import com.ruoyi.project.coffee.scanOrder.service.impl.ScanCategoryServiceImpl;

class ScanCategoryServiceImplTest
{
    @Mock
    private ScanCategoryMapper scanCategoryMapper;

    @Mock
    private ScanProductMapper scanProductMapper;

    private ScanCategoryServiceImpl scanCategoryService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        scanCategoryService = new ScanCategoryServiceImpl();
        ReflectionTestUtils.setField(scanCategoryService, "scanCategoryMapper", scanCategoryMapper);
        ReflectionTestUtils.setField(scanCategoryService, "scanProductMapper", scanProductMapper);
    }

    @Test
    void selectScanCategoryByIdShouldDelegateToMapper()
    {
        ScanCategory category = category(3L, "咖啡");
        when(scanCategoryMapper.selectScanCategoryById(3L)).thenReturn(category);

        ScanCategory result = scanCategoryService.selectScanCategoryById(3L);

        assertSame(category, result);
        verify(scanCategoryMapper).selectScanCategoryById(3L);
    }

    @Test
    void selectScanCategoryListShouldDelegateQueryToMapper()
    {
        ScanCategory query = category(null, "茶");
        List<ScanCategory> categories = Collections.singletonList(category(4L, "茶饮"));
        when(scanCategoryMapper.selectScanCategoryList(query)).thenReturn(categories);

        List<ScanCategory> result = scanCategoryService.selectScanCategoryList(query);

        assertSame(categories, result);
        verify(scanCategoryMapper).selectScanCategoryList(query);
    }

    @Test
    void insertScanCategoryShouldTrimNameFillDefaultsAndCreateTime()
    {
        ScanCategory category = category(null, "  拿铁  ");
        when(scanCategoryMapper.insertScanCategory(category)).thenReturn(1);

        int rows = scanCategoryService.insertScanCategory(category);

        assertEquals(1, rows);
        assertEquals("拿铁", category.getCategoryName());
        assertEquals(1, category.getStatus());
        assertEquals(0, category.getSortOrder());
        assertNotNull(category.getCreateTime());
        verify(scanCategoryMapper).insertScanCategory(category);
    }

    @Test
    void insertScanCategoryShouldKeepExplicitStatusAndSortOrder()
    {
        ScanCategory category = category(null, "冰饮");
        category.setStatus(0);
        category.setSortOrder(9);
        when(scanCategoryMapper.insertScanCategory(category)).thenReturn(1);

        int rows = scanCategoryService.insertScanCategory(category);

        assertEquals(1, rows);
        assertEquals(0, category.getStatus());
        assertEquals(9, category.getSortOrder());
        assertNotNull(category.getCreateTime());
    }

    @Test
    void insertScanCategoryShouldRejectBlankName()
    {
        ScanCategory category = category(null, "  ");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scanCategoryService.insertScanCategory(category));

        assertEquals("点单分类名称不能为空", exception.getMessage());
        verify(scanCategoryMapper, never()).insertScanCategory(any(ScanCategory.class));
    }

    @Test
    void updateScanCategoryShouldTrimNameKeepExplicitValuesAndUpdateTime()
    {
        ScanCategory category = category(5L, "  手冲  ");
        category.setStatus(0);
        category.setSortOrder(12);
        when(scanCategoryMapper.updateScanCategory(category)).thenReturn(1);

        int rows = scanCategoryService.updateScanCategory(category);

        assertEquals(1, rows);
        assertEquals("手冲", category.getCategoryName());
        assertEquals(0, category.getStatus());
        assertEquals(12, category.getSortOrder());
        assertNotNull(category.getUpdateTime());
        verify(scanCategoryMapper).updateScanCategory(category);
    }

    @Test
    void updateScanCategoryShouldRejectBlankName()
    {
        ScanCategory category = category(5L, " ");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scanCategoryService.updateScanCategory(category));

        assertEquals("点单分类名称不能为空", exception.getMessage());
        verify(scanCategoryMapper, never()).updateScanCategory(any(ScanCategory.class));
    }

    @Test
    void deleteScanCategoryByIdsShouldDeleteWhenNoLinkedProducts()
    {
        when(scanProductMapper.countScanProductByCategoryIds(any(String[].class))).thenReturn(0);
        when(scanCategoryMapper.deleteScanCategoryByIds(any(String[].class))).thenReturn(2);

        int rows = scanCategoryService.deleteScanCategoryByIds("10,20");

        assertEquals(2, rows);
        ArgumentCaptor<String[]> idsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(scanProductMapper).countScanProductByCategoryIds(idsCaptor.capture());
        assertEquals(Arrays.asList("10", "20"), Arrays.asList(idsCaptor.getValue()));
        verify(scanCategoryMapper).deleteScanCategoryByIds(idsCaptor.getValue());
    }

    @Test
    void deleteScanCategoryByIdsShouldRejectWhenProductsStillLinked()
    {
        when(scanProductMapper.countScanProductByCategoryIds(any(String[].class))).thenReturn(1);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> scanCategoryService.deleteScanCategoryByIds("10,20"));

        assertEquals("该点单分类下仍有关联商品，不能删除", exception.getMessage());
        verify(scanCategoryMapper, never()).deleteScanCategoryByIds(any(String[].class));
    }

    private static ScanCategory category(Long categoryId, String categoryName)
    {
        ScanCategory category = new ScanCategory();
        category.setCategoryId(categoryId);
        category.setCategoryName(categoryName);
        return category;
    }
}
