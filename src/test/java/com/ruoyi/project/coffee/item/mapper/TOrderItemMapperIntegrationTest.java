package com.ruoyi.project.coffee.item.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import com.ruoyi.project.coffee.item.domain.TOrderItem;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/TOrderItemMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.item.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.item.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TOrderItemMapperIntegrationTest
{
    @Autowired
    private TOrderItemMapper orderItemMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectTOrderItemListShouldUseCurrentProductNameAndImageWhenAvailable()
    {
        jdbcTemplate.update(
            "insert into t_product(product_id, product_name, image_url, remark, price, status) values (?, ?, ?, ?, ?, ?)",
            9201L, "当前商品名", "current.png", "默认规格", new BigDecimal("18.00"), 1
        );
        TOrderItem item = buildItem(9301L, 9201L, "历史快照名", "snapshot.png", "");
        orderItemMapper.insertTOrderItem(item);

        TOrderItem query = new TOrderItem();
        query.setOrderId(9301L);
        List<TOrderItem> items = orderItemMapper.selectTOrderItemList(query);

        assertEquals(1, items.size());
        assertEquals("当前商品名", items.get(0).getProductName());
        assertEquals("current.png", items.get(0).getProductImage());
        assertEquals("默认规格", items.get(0).getSpec());
    }

    @Test
    void selectTOrderItemListByOrderIdsShouldReturnItemsGroupedByOrderDesc()
    {
        orderItemMapper.insertTOrderItem(buildItem(9301L, 9202L, "拿铁", "latte.png", "热"));
        orderItemMapper.insertTOrderItem(buildItem(9302L, 9203L, "美式", "americano.png", "冰"));

        List<TOrderItem> items = orderItemMapper.selectTOrderItemListByOrderIds(Arrays.asList(9301L, 9302L));

        assertEquals(2, items.size());
        assertEquals(Long.valueOf(9302L), items.get(0).getOrderId());
        assertEquals(Long.valueOf(9301L), items.get(1).getOrderId());
    }

    @Test
    void insertUpdateDeleteShouldRoundTrip()
    {
        TOrderItem item = buildItem(9303L, 9204L, "卡布奇诺", "cappuccino.png", "少糖");

        assertEquals(1, orderItemMapper.insertTOrderItem(item));
        assertNotNull(item.getItemId());

        item.setQuantity(3L);
        item.setTotalPrice(new BigDecimal("54.00"));
        assertEquals(1, orderItemMapper.updateTOrderItem(item));

        TOrderItem saved = orderItemMapper.selectTOrderItemByItemId(item.getItemId());
        assertEquals(Long.valueOf(3L), saved.getQuantity());
        assertEquals(new BigDecimal("54.00"), saved.getTotalPrice());

        assertEquals(1, orderItemMapper.deleteTOrderItemByOrderId(9303L));
        TOrderItem query = new TOrderItem();
        query.setOrderId(9303L);
        assertEquals(0, orderItemMapper.selectTOrderItemList(query).size());
    }

    private TOrderItem buildItem(Long orderId, Long productId, String name, String image, String spec)
    {
        TOrderItem item = new TOrderItem();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setProductName(name);
        item.setProductImage(image);
        item.setPrice(new BigDecimal("18.00"));
        item.setSpec(spec);
        item.setQuantity(2L);
        item.setTotalPrice(new BigDecimal("36.00"));
        return item;
    }
}
