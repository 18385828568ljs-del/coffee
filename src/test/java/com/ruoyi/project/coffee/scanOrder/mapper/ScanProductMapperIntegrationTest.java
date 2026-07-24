package com.ruoyi.project.coffee.scanOrder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.scanOrder.domain.ScanProduct;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/ScanProductMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.scanOrder.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.scanOrder.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ScanProductMapperIntegrationTest
{
    @Autowired
    private ScanProductMapper scanProductMapper;

    @Test
    void insertAndSelectByIdShouldPersistProductSnapshotFields()
    {
        ScanProduct product = product(1L, "招牌奶茶", "经典红茶 + 鲜奶", "15.00", 1, 2);
        product.setImageUrl("https://example.com/milk-tea.png");
        product.setVideoUrl("https://example.com/milk-tea.mp4");
        product.setMonthSales(520);
        product.setTag("招牌");
        product.setCreateBy("admin");
        product.setCreateTime(new Date());
        product.setRemark("适合热饮");

        assertEquals(1, scanProductMapper.insertScanProduct(product));
        assertNotNull(product.getProductId());

        ScanProduct saved = scanProductMapper.selectScanProductById(product.getProductId());
        assertEquals(Long.valueOf(1L), saved.getCategoryId());
        assertEquals("招牌奶茶", saved.getProductName());
        assertEquals("经典红茶 + 鲜奶", saved.getSubTitle());
        assertEquals("https://example.com/milk-tea.png", saved.getImageUrl());
        assertEquals("https://example.com/milk-tea.mp4", saved.getVideoUrl());
        assertEquals(new BigDecimal("15.00"), saved.getPrice());
        assertEquals(Integer.valueOf(520), saved.getMonthSales());
        assertEquals("招牌", saved.getTag());
        assertEquals(Integer.valueOf(1), saved.getStatus());
        assertEquals(Integer.valueOf(2), saved.getSortOrder());
        assertEquals("admin", saved.getCreateBy());
        assertEquals("适合热饮", saved.getRemark());
    }

    @Test
    void selectScanProductListShouldFilterCategoryNameStatusAndOrderNullSortLast()
    {
        scanProductMapper.insertScanProduct(product(1L, "美式咖啡", "精选阿拉比卡豆", "12.00", 1, 2));
        scanProductMapper.insertScanProduct(product(1L, "冰美式", "低温萃取", "13.00", 1, 1));
        scanProductMapper.insertScanProduct(product(2L, "美式拿铁", "牛奶咖啡", "18.00", 1, 1));
        scanProductMapper.insertScanProduct(product(1L, "下架美式", "暂不售卖", "9.00", 0, 3));
        scanProductMapper.insertScanProduct(product(1L, "美式特调", "隐藏排序", "16.00", 1, null));

        ScanProduct query = new ScanProduct();
        query.setCategoryId(1L);
        query.setProductName("美式");
        query.setStatus(1);

        List<ScanProduct> products = scanProductMapper.selectScanProductList(query);

        assertEquals(3, products.size());
        assertEquals("冰美式", products.get(0).getProductName());
        assertEquals("美式咖啡", products.get(1).getProductName());
        assertEquals("美式特调", products.get(2).getProductName());
    }

    @Test
    void countScanProductByCategoryIdsShouldCountOnlyTargetCategories()
    {
        scanProductMapper.insertScanProduct(product(1L, "拿铁", "牛奶咖啡", "18.00", 1, 1));
        scanProductMapper.insertScanProduct(product(2L, "手冲", "浅烘豆", "28.00", 1, 1));
        scanProductMapper.insertScanProduct(product(3L, "甜点", "每日供应", "16.00", 1, 1));

        int count = scanProductMapper.countScanProductByCategoryIds(new String[] { "1", "2" });

        assertEquals(2, count);
    }

    @Test
    void updateScanProductShouldOnlyChangeProvidedFieldsAndIgnoreBlankName()
    {
        ScanProduct product = product(1L, "原味拿铁", "热饮", "18.00", 1, 1);
        scanProductMapper.insertScanProduct(product);

        ScanProduct update = new ScanProduct();
        update.setProductId(product.getProductId());
        update.setProductName("");
        update.setPrice(new BigDecimal("20.00"));
        update.setStatus(0);
        update.setSortOrder(9);
        update.setUpdateBy("tester");
        update.setUpdateTime(new Date());
        update.setRemark("临时下架");

        assertEquals(1, scanProductMapper.updateScanProduct(update));

        ScanProduct saved = scanProductMapper.selectScanProductById(product.getProductId());
        assertEquals("原味拿铁", saved.getProductName());
        assertEquals(new BigDecimal("20.00"), saved.getPrice());
        assertEquals(Integer.valueOf(0), saved.getStatus());
        assertEquals(Integer.valueOf(9), saved.getSortOrder());
        assertEquals("tester", saved.getUpdateBy());
        assertEquals("临时下架", saved.getRemark());
    }

    @Test
    void deleteScanProductByIdsShouldRemoveOnlyTargetProducts()
    {
        ScanProduct first = product(1L, "香草拿铁", "香草风味", "19.00", 1, 1);
        ScanProduct second = product(1L, "榛果拿铁", "榛果风味", "19.00", 1, 2);
        ScanProduct third = product(2L, "手冲瑰夏", "花香调", "36.00", 1, 3);
        scanProductMapper.insertScanProduct(first);
        scanProductMapper.insertScanProduct(second);
        scanProductMapper.insertScanProduct(third);

        assertEquals(2, scanProductMapper.deleteScanProductByIds(
            new String[] { String.valueOf(first.getProductId()), String.valueOf(second.getProductId()) }));

        assertNull(scanProductMapper.selectScanProductById(first.getProductId()));
        assertNull(scanProductMapper.selectScanProductById(second.getProductId()));
        assertEquals("手冲瑰夏", scanProductMapper.selectScanProductById(third.getProductId()).getProductName());
    }

    private static ScanProduct product(Long categoryId, String name, String subTitle, String price, Integer status,
        Integer sortOrder)
    {
        ScanProduct product = new ScanProduct();
        product.setCategoryId(categoryId);
        product.setProductName(name);
        product.setSubTitle(subTitle);
        product.setPrice(new BigDecimal(price));
        product.setStatus(status);
        product.setSortOrder(sortOrder);
        return product;
    }
}
