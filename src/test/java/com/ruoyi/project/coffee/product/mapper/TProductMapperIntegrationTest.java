package com.ruoyi.project.coffee.product.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ruoyi.project.coffee.product.domain.TProduct;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
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
    "mybatis.mapper-locations=classpath:mybatis/coffee/TProductMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.product.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.product.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TProductMapperIntegrationTest
{
    @Autowired
    private TProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectProductListShouldFilterAndSortByCreateTimeThenId()
    {
        insertProduct(1001L, 1L, "手冲耶加雪菲", "埃塞俄比亚", "水洗", "浅烘",
            "柑橘 花香", "ethiopia.png", new BigDecimal("38.00"), 1, 8L,
            "2026-06-17 10:00:00");
        insertProduct(1002L, 1L, "手冲瑰夏", "巴拿马", "水洗", "浅烘",
            "茉莉 蜂蜜", "gesha.png", new BigDecimal("58.00"), 1, 5L,
            "2026-06-17 11:00:00");
        insertProduct(1003L, 2L, "意式拼配", "中国", "拼配", "深烘",
            "坚果 巧克力", "blend.png", new BigDecimal("28.00"), 1, 12L,
            "2026-06-17 12:00:00");

        TProduct query = new TProduct();
        query.setCategoryId(1L);
        query.setProductName("手冲");
        query.setProcessingMethod("水洗");
        query.setStatus(1);
        List<TProduct> products = productMapper.selectTProductList(query);

        assertEquals(2, products.size());
        assertEquals(Long.valueOf(1002L), products.get(0).getProductId());
        assertEquals("巴拿马", products.get(0).getOrigin());
        assertEquals("茉莉 蜂蜜", products.get(0).getFlavorNotes());
        assertEquals(Long.valueOf(1001L), products.get(1).getProductId());
    }

    @Test
    void stockOperationsShouldBeAtomicAndRespectStatusAndQuantity()
    {
        insertProduct(1001L, 1L, "招牌拿铁", "中国", "拼配", "中烘",
            "奶油 坚果", "latte.png", new BigDecimal("15.00"), 1, 3L,
            "2026-06-17 10:00:00");
        insertProduct(1002L, 1L, "下架美式", "中国", "拼配", "中烘",
            "黑巧", "americano.png", new BigDecimal("12.00"), 0, 10L,
            "2026-06-17 10:00:00");

        assertEquals(1, productMapper.decreaseStock(1001L, 2L));
        assertEquals(Long.valueOf(1L), productMapper.selectTProductByProductId(1001L).getStock());
        assertEquals(0, productMapper.decreaseStock(1001L, 2L));
        assertEquals(0, productMapper.decreaseStock(1002L, 1L));
        assertEquals(1, productMapper.increaseStock(1001L, 4L));
        assertEquals(Long.valueOf(5L), productMapper.selectTProductByProductId(1001L).getStock());
    }

    @Test
    void insertUpdateDeleteShouldRoundTripAllBusinessFields()
    {
        TProduct product = new TProduct();
        product.setCategoryId(3L);
        product.setProductName("云南小粒咖啡");
        product.setOrigin("云南");
        product.setProcessingMethod("日晒");
        product.setRoastLevel("中深烘");
        product.setFlavorNotes("红糖 坚果");
        product.setDescription("适合意式咖啡");
        product.setPrice(new BigDecimal("42.00"));
        product.setStock(20L);
        product.setImageUrl("yunnan.png");
        product.setStatus(1);
        product.setCreateBy("tester");
        product.setRemark("半磅装");

        assertEquals(1, productMapper.insertTProduct(product));
        assertNotNull(product.getProductId());

        product.setProductName("云南精品咖啡");
        product.setStock(18L);
        product.setUpdateBy("admin");
        assertEquals(1, productMapper.updateTProduct(product));

        TProduct saved = productMapper.selectTProductByProductId(product.getProductId());
        assertEquals("云南精品咖啡", saved.getProductName());
        assertEquals("云南", saved.getOrigin());
        assertEquals("日晒", saved.getProcessingMethod());
        assertEquals("中深烘", saved.getRoastLevel());
        assertEquals("红糖 坚果", saved.getFlavorNotes());
        assertEquals("适合意式咖啡", saved.getDescription());
        assertEquals(new BigDecimal("42.00"), saved.getPrice());
        assertEquals(Long.valueOf(18L), saved.getStock());
        assertEquals("半磅装", saved.getRemark());

        assertEquals(1, productMapper.deleteTProductByProductId(product.getProductId()));
        assertNull(productMapper.selectTProductByProductId(product.getProductId()));
    }

    @Test
    void batchQueryCountAndDeleteShouldUseProductIdsAndCategoryIds()
    {
        insertProduct(1001L, 1L, "咖啡豆 A", "云南", "水洗", "中烘",
            "坚果", "a.png", new BigDecimal("39.00"), 1, 10L,
            "2026-06-17 10:00:00");
        insertProduct(1002L, 1L, "咖啡豆 B", "云南", "日晒", "浅烘",
            "莓果", "b.png", new BigDecimal("49.00"), 1, 8L,
            "2026-06-17 11:00:00");
        insertProduct(1003L, 2L, "滤杯", "中国", "陶瓷", "无",
            "器具", "c.png", new BigDecimal("29.00"), 1, 6L,
            "2026-06-17 12:00:00");

        assertEquals(2, productMapper.countProductByCategoryIds(new String[] {"1"}));
        assertEquals(2, productMapper.selectTProductByProductIds(Arrays.asList(1001L, 1003L)).size());
        assertEquals(2, productMapper.deleteTProductByProductIds(new String[] {"1001", "1002"}));

        assertEquals(1, productMapper.selectTProductList(new TProduct()).size());
        assertEquals(Long.valueOf(1003L), productMapper.selectTProductList(new TProduct()).get(0).getProductId());
    }

    private void insertProduct(Long productId, Long categoryId, String productName, String origin,
        String processingMethod, String roastLevel, String flavorNotes, String imageUrl,
        BigDecimal price, Integer status, Long stock, String createTime)
    {
        jdbcTemplate.update(
            "insert into t_product(product_id, category_id, product_name, origin, processing_method, roast_level, "
                + "flavor_notes, description, price, stock, image_url, status, create_by, create_time, remark) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            productId, categoryId, productName, origin, processingMethod, roastLevel, flavorNotes,
            productName + "描述", price, stock, imageUrl, status, "tester",
            Timestamp.valueOf(createTime), "默认规格"
        );
    }
}
