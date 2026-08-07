package com.ruoyi.project.coffee.cart.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.cart.domain.TCart;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/TCartMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.cart.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.cart.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TCartMapperIntegrationTest
{
    @Autowired
    private TCartMapper cartMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectTCartListShouldJoinProductSnapshot()
    {
        jdbcTemplate.update(
            "insert into t_product(product_id, category_id, product_name, image_url, price, status) "
                + "values (?, ?, ?, ?, ?, ?)",
            1001L, 3L, "招牌拿铁", "latte.png", new BigDecimal("18.50"), 1
        );
        jdbcTemplate.update(
            "insert into t_cart(cart_id, user_id, product_id, quantity, spec, create_time) values (?, ?, ?, ?, ?, ?)",
            2001L, 88L, 1001L, 2L, "热", Timestamp.valueOf("2026-06-17 10:00:00")
        );

        TCart query = new TCart();
        query.setUserId(88L);
        List<TCart> carts = cartMapper.selectTCartList(query);

        assertEquals(1, carts.size());
        assertEquals("招牌拿铁", carts.get(0).getProductName());
        assertEquals(Long.valueOf(3L), carts.get(0).getCategoryId());
        assertEquals("latte.png", carts.get(0).getProductImage());
        assertEquals(new BigDecimal("18.50"), carts.get(0).getPrice());
        assertEquals(Long.valueOf(2L), carts.get(0).getQuantity());
    }

    @Test
    void insertUpdateDeleteShouldRoundTrip()
    {
        jdbcTemplate.update(
            "insert into t_product(product_id, product_name, image_url, price, status) values (?, ?, ?, ?, ?)",
            1002L, "冰美式", "americano.png", new BigDecimal("12.00"), 1
        );

        TCart cart = new TCart();
        cart.setUserId(99L);
        cart.setProductId(1002L);
        cart.setQuantity(1L);
        cart.setSpec("少冰");

        assertEquals(1, cartMapper.insertTCart(cart));
        assertNotNull(cart.getCartId());

        cart.setQuantity(3L);
        assertEquals(1, cartMapper.updateTCart(cart));

        TCart saved = cartMapper.selectTCartByCartId(cart.getCartId());
        assertEquals(Long.valueOf(3L), saved.getQuantity());
        assertEquals("冰美式", saved.getProductName());

        assertEquals(1, cartMapper.deleteTCartByCartId(cart.getCartId()));
        assertNull(cartMapper.selectTCartByCartId(cart.getCartId()));
    }
}
