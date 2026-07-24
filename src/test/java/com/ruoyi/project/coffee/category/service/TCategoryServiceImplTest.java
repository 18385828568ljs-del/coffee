package com.ruoyi.project.coffee.category.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.category.domain.TCategory;
import com.ruoyi.project.coffee.category.mapper.TCategoryMapper;
import com.ruoyi.project.coffee.category.service.impl.TCategoryServiceImpl;
import com.ruoyi.project.coffee.product.mapper.TProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class TCategoryServiceImplTest
{
    private TCategoryServiceImpl categoryService;

    @Mock
    private TCategoryMapper categoryMapper;

    @Mock
    private TProductMapper productMapper;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        categoryService = new TCategoryServiceImpl();
        ReflectionTestUtils.setField(categoryService, "tCategoryMapper", categoryMapper);
        ReflectionTestUtils.setField(categoryService, "tProductMapper", productMapper);
    }

    @Test
    void insertCategoryShouldTrimNameValidateDuplicateAndFillCreateTime()
    {
        TCategory category = new TCategory();
        category.setCategoryName("  咖啡豆  ");
        when(categoryMapper.countByCategoryName("咖啡豆", null)).thenReturn(0);
        when(categoryMapper.insertTCategory(category)).thenReturn(1);

        int rows = categoryService.insertTCategory(category);

        assertEquals(1, rows);
        assertEquals("咖啡豆", category.getCategoryName());
        assertNotNull(category.getCreateTime());
        verify(categoryMapper).insertTCategory(category);
    }

    @Test
    void insertCategoryShouldRejectBlankName()
    {
        TCategory category = new TCategory();
        category.setCategoryName("   ");

        ServiceException exception = assertThrows(ServiceException.class,
            () -> categoryService.insertTCategory(category));

        assertEquals("分类名称不能为空", exception.getMessage());
        verify(categoryMapper, never()).insertTCategory(category);
    }

    @Test
    void updateCategoryShouldExcludeCurrentIdWhenCheckingDuplicateName()
    {
        TCategory category = new TCategory();
        category.setCategoryId(8L);
        category.setCategoryName(" 意式咖啡 ");
        when(categoryMapper.countByCategoryName("意式咖啡", 8L)).thenReturn(0);
        when(categoryMapper.updateTCategory(category)).thenReturn(1);

        int rows = categoryService.updateTCategory(category);

        assertEquals(1, rows);
        assertEquals("意式咖啡", category.getCategoryName());
        assertNotNull(category.getUpdateTime());
        verify(categoryMapper).countByCategoryName("意式咖啡", 8L);
        verify(categoryMapper).updateTCategory(category);
    }

    @Test
    void updateCategoryShouldRejectDuplicateName()
    {
        TCategory category = new TCategory();
        category.setCategoryId(8L);
        category.setCategoryName("咖啡豆");
        when(categoryMapper.countByCategoryName("咖啡豆", 8L)).thenReturn(1);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> categoryService.updateTCategory(category));

        assertEquals("分类名称已存在，请重新填写", exception.getMessage());
        verify(categoryMapper, never()).updateTCategory(category);
    }

    @Test
    void deleteCategoryShouldRejectWhenCategoryStillHasProducts()
    {
        when(productMapper.countProductByCategoryIds(new String[] {"1", "2"})).thenReturn(3);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> categoryService.deleteTCategoryByCategoryIds("1,2"));

        assertEquals("该分类下仍有商品，不能删除", exception.getMessage());
        verify(categoryMapper, never()).deleteTCategoryByCategoryIds(new String[] {"1", "2"});
    }

    @Test
    void deleteCategoryShouldCheckProductsBeforeDeleting()
    {
        when(productMapper.countProductByCategoryIds(new String[] {"6"})).thenReturn(0);
        when(categoryMapper.deleteTCategoryByCategoryId(6L)).thenReturn(1);

        int rows = categoryService.deleteTCategoryByCategoryId(6L);

        assertEquals(1, rows);
        verify(productMapper).countProductByCategoryIds(new String[] {"6"});
        verify(categoryMapper).deleteTCategoryByCategoryId(eq(6L));
    }
}
