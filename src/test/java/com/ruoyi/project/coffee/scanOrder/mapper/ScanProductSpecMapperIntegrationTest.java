package com.ruoyi.project.coffee.scanOrder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpec;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProductSpecOption;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/ScanProductSpecMapper.xml,classpath:mybatis/coffee/ScanProductSpecOptionMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.scanOrder.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.scanOrder.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ScanProductSpecMapperIntegrationTest
{
    @Autowired
    private ScanProductSpecMapper scanProductSpecMapper;

    @Autowired
    private ScanProductSpecOptionMapper scanProductSpecOptionMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectSpecListByProductIdShouldFilterProductAndOrderNullSortLast()
    {
        insertSpec(101L, 7L, "糖度", "single", 1, 2);
        insertSpec(102L, 7L, "温度", "single", 1, 1);
        insertSpec(103L, 8L, "杯型", "single", 1, 1);
        insertSpec(104L, 7L, "加料", "multiple", 0, null);

        List<ScanProductSpec> specs = scanProductSpecMapper.selectSpecListByProductId(7L);

        assertEquals(3, specs.size());
        assertEquals("温度", specs.get(0).getSpecName());
        assertEquals("糖度", specs.get(1).getSpecName());
        assertEquals("加料", specs.get(2).getSpecName());
        assertEquals("multiple", specs.get(2).getSpecType());
        assertEquals(Integer.valueOf(0), specs.get(2).getRequired());
    }

    @Test
    void selectOptionListByProductIdShouldGroupBySpecAndOrderNullSortLast()
    {
        insertOption(201L, 12L, 7L, "少糖", "0.00", 0, 2);
        insertOption(202L, 11L, 7L, "冰", "0.00", 0, 2);
        insertOption(203L, 11L, 7L, "热", "0.00", 1, 1);
        insertOption(204L, 12L, 7L, "正常糖", "0.00", 1, 1);
        insertOption(205L, 11L, 8L, "大杯", "3.00", 0, 1);
        insertOption(206L, 11L, 7L, "常温", "0.00", 0, null);

        List<ScanProductSpecOption> options = scanProductSpecOptionMapper.selectOptionListByProductId(7L);

        assertEquals(5, options.size());
        assertEquals("热", options.get(0).getOptionName());
        assertEquals("冰", options.get(1).getOptionName());
        assertEquals("常温", options.get(2).getOptionName());
        assertEquals("正常糖", options.get(3).getOptionName());
        assertEquals("少糖", options.get(4).getOptionName());
        assertEquals(new BigDecimal("0.00"), options.get(0).getExtraPrice());
        assertEquals(Integer.valueOf(1), options.get(0).getIsDefault());
    }

    @Test
    void selectOptionListBySpecIdShouldFilterSpecAndKeepConfiguredOrder()
    {
        insertOption(301L, 21L, 7L, "燕麦奶", "2.00", 0, 2);
        insertOption(302L, 21L, 7L, "牛奶", "0.00", 1, 1);
        insertOption(303L, 22L, 7L, "小杯", "0.00", 1, 1);

        List<ScanProductSpecOption> options = scanProductSpecOptionMapper.selectOptionListBySpecId(21L);

        assertEquals(2, options.size());
        assertEquals("牛奶", options.get(0).getOptionName());
        assertEquals("燕麦奶", options.get(1).getOptionName());
        assertEquals(new BigDecimal("2.00"), options.get(1).getExtraPrice());
    }

    @Test
    void selectOptionListByIdsShouldReturnOnlyRequestedOptionsOrderedByOptionId()
    {
        insertOption(401L, 31L, 7L, "小杯", "0.00", 1, 1);
        insertOption(402L, 31L, 7L, "中杯", "2.00", 0, 2);
        insertOption(403L, 31L, 7L, "大杯", "4.00", 0, 3);

        List<ScanProductSpecOption> options =
            scanProductSpecOptionMapper.selectOptionListByIds(Arrays.asList(403L, 401L));

        assertEquals(2, options.size());
        assertEquals(Long.valueOf(401L), options.get(0).getOptionId());
        assertEquals(Long.valueOf(403L), options.get(1).getOptionId());
    }

    @Test
    void deleteByProductIdsShouldOnlyDeleteSpecsAndOptionsForTargetProducts()
    {
        insertSpec(501L, 7L, "温度", "single", 1, 1);
        insertSpec(502L, 8L, "糖度", "single", 1, 1);
        insertOption(601L, 501L, 7L, "热", "0.00", 1, 1);
        insertOption(602L, 502L, 8L, "正常糖", "0.00", 1, 1);

        assertEquals(1, scanProductSpecOptionMapper.deleteOptionByProductIds(new String[] { "7" }));
        assertEquals(1, scanProductSpecMapper.deleteSpecByProductIds(new String[] { "7" }));

        assertEquals(0, scanProductSpecMapper.selectSpecListByProductId(7L).size());
        assertEquals(1, scanProductSpecMapper.selectSpecListByProductId(8L).size());
        assertEquals(0, scanProductSpecOptionMapper.selectOptionListByProductId(7L).size());
        assertEquals(1, scanProductSpecOptionMapper.selectOptionListByProductId(8L).size());
    }

    private void insertSpec(Long specId, Long productId, String name, String type, Integer required, Integer sortOrder)
    {
        jdbcTemplate.update(
            "insert into t_scan_product_spec (spec_id, product_id, spec_name, spec_type, required, sort_order, create_time) "
                + "values (?, ?, ?, ?, ?, ?, now())",
            specId, productId, name, type, required, sortOrder);
    }

    private void insertOption(Long optionId, Long specId, Long productId, String name, String extraPrice,
        Integer isDefault, Integer sortOrder)
    {
        jdbcTemplate.update(
            "insert into t_scan_product_spec_option "
                + "(option_id, spec_id, product_id, option_name, extra_price, is_default, sort_order, create_time) "
                + "values (?, ?, ?, ?, ?, ?, ?, now())",
            optionId, specId, productId, name, new BigDecimal(extraPrice), isDefault, sortOrder);
    }
}
