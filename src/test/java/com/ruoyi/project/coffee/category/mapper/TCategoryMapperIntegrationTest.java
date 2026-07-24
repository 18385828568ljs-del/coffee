package com.ruoyi.project.coffee.category.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ruoyi.project.coffee.category.domain.TCategory;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/TCategoryMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.category.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.category.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TCategoryMapperIntegrationTest
{
    @Autowired
    private TCategoryMapper categoryMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectCategoryListShouldFilterNameAndSortNullLast()
    {
        insertCategory(1L, "咖啡豆", 20L, "2026-06-17 10:00:00");
        insertCategory(2L, "咖啡器具", 10L, "2026-06-17 11:00:00");
        insertCategory(3L, "周边", null, "2026-06-17 12:00:00");

        TCategory query = new TCategory();
        query.setCategoryName("咖啡");
        List<TCategory> categories = categoryMapper.selectTCategoryList(query);

        assertEquals(2, categories.size());
        assertEquals(Long.valueOf(2L), categories.get(0).getCategoryId());
        assertEquals(Long.valueOf(1L), categories.get(1).getCategoryId());
    }

    @Test
    void countByCategoryNameShouldExcludeCurrentCategoryWhenProvided()
    {
        insertCategory(1L, "咖啡豆", 10L, "2026-06-17 10:00:00");
        insertCategory(2L, "咖啡豆", 20L, "2026-06-17 11:00:00");

        assertEquals(2, categoryMapper.countByCategoryName("咖啡豆", null));
        assertEquals(1, categoryMapper.countByCategoryName("咖啡豆", 1L));
    }

    @Test
    void insertUpdateDeleteShouldRoundTripCategoryFields()
    {
        TCategory category = new TCategory();
        category.setCategoryName("手冲专区");
        category.setSortOrder(3L);
        category.setCreateBy("tester");
        category.setRemark("精品豆分类");

        assertEquals(1, categoryMapper.insertTCategory(category));
        assertNotNull(category.getCategoryId());

        category.setCategoryName("手冲咖啡专区");
        category.setSortOrder(1L);
        category.setUpdateBy("admin");
        assertEquals(1, categoryMapper.updateTCategory(category));

        TCategory saved = categoryMapper.selectTCategoryByCategoryId(category.getCategoryId());
        assertEquals("手冲咖啡专区", saved.getCategoryName());
        assertEquals(Long.valueOf(1L), saved.getSortOrder());
        assertEquals("精品豆分类", saved.getRemark());

        assertEquals(1, categoryMapper.deleteTCategoryByCategoryId(category.getCategoryId()));
        assertNull(categoryMapper.selectTCategoryByCategoryId(category.getCategoryId()));
    }

    @Test
    void batchDeleteShouldDeleteOnlyTargetCategories()
    {
        insertCategory(1L, "手冲专区", 10L, "2026-06-17 10:00:00");
        insertCategory(2L, "意式咖啡", 20L, "2026-06-17 11:00:00");
        insertCategory(3L, "咖啡滤杯", 30L, "2026-06-17 12:00:00");

        assertEquals(2, categoryMapper.deleteTCategoryByCategoryIds(new String[] {"1", "3"}));

        assertNull(categoryMapper.selectTCategoryByCategoryId(1L));
        assertNotNull(categoryMapper.selectTCategoryByCategoryId(2L));
        assertNull(categoryMapper.selectTCategoryByCategoryId(3L));
    }

    private void insertCategory(Long categoryId, String categoryName, Long sortOrder, String createTime)
    {
        jdbcTemplate.update(
            "insert into t_category(category_id, category_name, sort_order, create_by, create_time, remark) values (?, ?, ?, ?, ?, ?)",
            categoryId, categoryName, sortOrder, "tester", Timestamp.valueOf(createTime), "测试分类"
        );
    }
}
