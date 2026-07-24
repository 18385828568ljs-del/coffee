package com.ruoyi.project.coffee.scanOrder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCategory;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/ScanCategoryMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.scanOrder.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.scanOrder.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ScanCategoryMapperIntegrationTest
{
    @Autowired
    private ScanCategoryMapper scanCategoryMapper;

    @Test
    void insertAndSelectByIdShouldPersistCategoryFields()
    {
        ScanCategory category = category("手冲专区", "pour-over.png", 2, 1);
        category.setCreateBy("admin");
        category.setCreateTime(new Date());
        category.setRemark("适合精品咖啡");

        assertEquals(1, scanCategoryMapper.insertScanCategory(category));
        assertNotNull(category.getCategoryId());

        ScanCategory saved = scanCategoryMapper.selectScanCategoryById(category.getCategoryId());
        assertEquals("手冲专区", saved.getCategoryName());
        assertEquals("pour-over.png", saved.getIcon());
        assertEquals(Integer.valueOf(2), saved.getSortOrder());
        assertEquals(Integer.valueOf(1), saved.getStatus());
        assertEquals("admin", saved.getCreateBy());
        assertEquals("适合精品咖啡", saved.getRemark());
    }

    @Test
    void selectScanCategoryListShouldFilterNameStatusAndOrderNullSortLast()
    {
        scanCategoryMapper.insertScanCategory(category("手冲专区", "pour-over.png", 2, 1));
        scanCategoryMapper.insertScanCategory(category("手冲冷萃", "cold-brew.png", 1, 1));
        scanCategoryMapper.insertScanCategory(category("停用手冲", "off.png", 3, 0));
        scanCategoryMapper.insertScanCategory(category("意式咖啡", "espresso.png", 1, 1));
        scanCategoryMapper.insertScanCategory(category("手冲隐藏排序", "hidden.png", null, 1));

        ScanCategory query = new ScanCategory();
        query.setCategoryName("手冲");
        query.setStatus(1);

        List<ScanCategory> categories = scanCategoryMapper.selectScanCategoryList(query);

        assertEquals(3, categories.size());
        assertEquals("手冲冷萃", categories.get(0).getCategoryName());
        assertEquals("手冲专区", categories.get(1).getCategoryName());
        assertEquals("手冲隐藏排序", categories.get(2).getCategoryName());
    }

    @Test
    void insertShouldIgnoreBlankIconButKeepOtherRequiredFields()
    {
        ScanCategory category = category("咖啡甜点", "", 5, 1);

        assertEquals(1, scanCategoryMapper.insertScanCategory(category));

        ScanCategory saved = scanCategoryMapper.selectScanCategoryById(category.getCategoryId());
        assertEquals("咖啡甜点", saved.getCategoryName());
        assertNull(saved.getIcon());
        assertEquals(Integer.valueOf(5), saved.getSortOrder());
    }

    @Test
    void updateScanCategoryShouldOnlyChangeProvidedFieldsAndIgnoreBlankName()
    {
        ScanCategory category = category("原始分类", "old.png", 1, 1);
        scanCategoryMapper.insertScanCategory(category);

        ScanCategory update = new ScanCategory();
        update.setCategoryId(category.getCategoryId());
        update.setCategoryName("");
        update.setIcon("");
        update.setSortOrder(9);
        update.setStatus(0);
        update.setUpdateBy("tester");
        update.setUpdateTime(new Date());
        update.setRemark("临时停用");

        assertEquals(1, scanCategoryMapper.updateScanCategory(update));

        ScanCategory saved = scanCategoryMapper.selectScanCategoryById(category.getCategoryId());
        assertEquals("原始分类", saved.getCategoryName());
        assertEquals("", saved.getIcon());
        assertEquals(Integer.valueOf(9), saved.getSortOrder());
        assertEquals(Integer.valueOf(0), saved.getStatus());
        assertEquals("tester", saved.getUpdateBy());
        assertEquals("临时停用", saved.getRemark());
    }

    @Test
    void deleteScanCategoryByIdsShouldRemoveOnlyTargetCategories()
    {
        ScanCategory first = category("咖啡", "coffee.png", 1, 1);
        ScanCategory second = category("茶饮", "tea.png", 2, 1);
        ScanCategory third = category("甜点", "dessert.png", 3, 1);
        scanCategoryMapper.insertScanCategory(first);
        scanCategoryMapper.insertScanCategory(second);
        scanCategoryMapper.insertScanCategory(third);

        assertEquals(2, scanCategoryMapper.deleteScanCategoryByIds(
            new String[] { String.valueOf(first.getCategoryId()), String.valueOf(third.getCategoryId()) }));

        assertNull(scanCategoryMapper.selectScanCategoryById(first.getCategoryId()));
        assertEquals("茶饮", scanCategoryMapper.selectScanCategoryById(second.getCategoryId()).getCategoryName());
        assertNull(scanCategoryMapper.selectScanCategoryById(third.getCategoryId()));
    }

    private static ScanCategory category(String name, String icon, Integer sortOrder, Integer status)
    {
        ScanCategory category = new ScanCategory();
        category.setCategoryName(name);
        category.setIcon(icon);
        category.setSortOrder(sortOrder);
        category.setStatus(status);
        return category;
    }
}
