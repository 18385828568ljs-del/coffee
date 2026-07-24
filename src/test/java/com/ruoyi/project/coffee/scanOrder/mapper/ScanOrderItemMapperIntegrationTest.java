package com.ruoyi.project.coffee.scanOrder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderItem;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/ScanOrderItemMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.scanOrder.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.scanOrder.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ScanOrderItemMapperIntegrationTest
{
    @Autowired
    private ScanOrderItemMapper scanOrderItemMapper;

    @Test
    void insertScanOrderItemShouldPersistSnapshotFields()
    {
        ScanOrderItem item = buildItem(2001L, 101L, "招牌奶茶", "热/正常糖/大杯");

        assertEquals(1, scanOrderItemMapper.insertScanOrderItem(item));
        assertNotNull(item.getItemId());

        List<ScanOrderItem> items = scanOrderItemMapper.selectItemsByOrderId(2001L);
        assertEquals(1, items.size());
        assertEquals("招牌奶茶", items.get(0).getProductName());
        assertEquals("热/正常糖/大杯", items.get(0).getSpec());
        assertEquals(new BigDecimal("18.00"), items.get(0).getTotalPrice());
        assertNotNull(items.get(0).getCreateTime());
    }

    @Test
    void batchInsertShouldKeepOrderIsolationAndPrimaryKeyOrdering()
    {
        ScanOrderItem first = buildItem(2002L, 201L, "美式咖啡", "冰/少糖");
        ScanOrderItem second = buildItem(2002L, 202L, "焦糖玛奇朵", "热/正常糖");
        ScanOrderItem otherOrder = buildItem(2003L, 203L, "阿芙佳朵", "冰/加奶");

        assertEquals(2, scanOrderItemMapper.batchInsertScanOrderItem(Arrays.asList(first, second)));
        assertEquals(1, scanOrderItemMapper.insertScanOrderItem(otherOrder));

        List<ScanOrderItem> items = scanOrderItemMapper.selectItemsByOrderId(2002L);

        assertEquals(2, items.size());
        assertEquals("美式咖啡", items.get(0).getProductName());
        assertEquals("焦糖玛奇朵", items.get(1).getProductName());
        assertEquals(Long.valueOf(2002L), items.get(0).getOrderId());
        assertEquals(Long.valueOf(2002L), items.get(1).getOrderId());
    }

    @Test
    void selectItemsByOrderIdsShouldReturnSnapshotsForListCards()
    {
        ScanOrderItem first = buildItem(2004L, 301L, "生椰拿铁", "热");
        ScanOrderItem second = buildItem(2005L, 302L, "冷萃咖啡", "冰");
        scanOrderItemMapper.batchInsertScanOrderItem(Arrays.asList(first, second));

        List<ScanOrderItem> items = scanOrderItemMapper.selectItemsByOrderIds(Arrays.asList(2004L, 2005L));

        assertEquals(2, items.size());
        assertEquals("https://example.com/302.png", items.get(0).getProductImage());
        assertEquals("https://example.com/301.png", items.get(1).getProductImage());
    }

    private ScanOrderItem buildItem(Long orderId, Long productId, String productName, String spec)
    {
        ScanOrderItem item = new ScanOrderItem();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setProductName(productName);
        item.setProductImage("https://example.com/" + productId + ".png");
        item.setSpec(spec);
        item.setPrice(new BigDecimal("9.00"));
        item.setQuantity(2);
        item.setTotalPrice(new BigDecimal("18.00"));
        item.setCreateTime(new Date());
        return item;
    }
}
