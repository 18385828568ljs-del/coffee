package com.ruoyi.project.coffee.scanOrder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/ScanCartMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.scanOrder.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.scanOrder.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ScanCartMapperIntegrationTest
{
    @Autowired
    private ScanCartMapper scanCartMapper;

    @Test
    void selectMatchingCartShouldReturnSameOwnerSameSpecRecord()
    {
        ScanCart cart = createCart(7L, "openid-a", "A01", 3001L, "{\"temp\":\"hot\"}");
        scanCartMapper.insertScanCart(cart);

        ScanCart query = new ScanCart();
        query.setUserId(7L);
        query.setShopId(1L);
        query.setTableNo("A01");
        query.setProductId(3001L);
        query.setSpecJson("{\"temp\":\"hot\"}");

        List<ScanCart> matches = scanCartMapper.selectMatchingCart(query);
        assertEquals(1, matches.size());
        assertEquals("燕麦拿铁", matches.get(0).getProductName());
        assertEquals(Integer.valueOf(2), matches.get(0).getQuantity());
    }

    @Test
    void logicDeleteByOwnerAndTableShouldOnlyAffectCurrentTable()
    {
        scanCartMapper.insertScanCart(createCart(9L, "openid-b", "A01", 3002L, "{\"sugar\":\"half\"}"));
        scanCartMapper.insertScanCart(createCart(9L, "openid-b", "B02", 3003L, "{\"sugar\":\"full\"}"));

        ScanCart query = new ScanCart();
        query.setUserId(9L);
        query.setShopId(1L);
        query.setTableNo("A01");
        assertEquals(1, scanCartMapper.logicDeleteByOwnerAndTable(query));

        ScanCart remainQuery = new ScanCart();
        remainQuery.setUserId(9L);
        remainQuery.setShopId(1L);

        List<ScanCart> remains = scanCartMapper.selectScanCartList(remainQuery);
        assertEquals(1, remains.size());
        assertEquals("B02", remains.get(0).getTableNo());
    }

    private ScanCart createCart(Long userId, String openid, String tableNo, Long productId, String specJson)
    {
        ScanCart cart = new ScanCart();
        cart.setUserId(userId);
        cart.setOpenid(openid);
        cart.setShopId(1L);
        cart.setTableNo(tableNo);
        cart.setProductId(productId);
        cart.setProductName("燕麦拿铁");
        cart.setProductImage("oat-latte.png");
        cart.setPrice(new BigDecimal("22.00"));
        cart.setQuantity(2);
        cart.setSpecText("热 / 半糖");
        cart.setSpecJson(specJson);
        cart.setSelected(1);
        cart.setStatus(1);
        cart.setDelFlag(0);
        return cart;
    }
}
