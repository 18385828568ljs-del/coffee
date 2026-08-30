package com.ruoyi.project.coffee.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:recommendation_acceptance;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/UserProfileMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.profile.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.profile.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProductRecommendationConcurrencyAcceptanceTest
{
    private static final Instant NOW = Instant.parse("2026-08-07T03:00:00Z");
    private static final int PRODUCT_COUNT = 1000;
    private static final int ORDER_COUNT = 10000;
    private static final int CONCURRENCY = 16;
    private static final int REQUEST_COUNT = 160;
    private static final double MAX_P95_MILLIS = 1500D;
    private static final double MIN_THROUGHPUT = 15D;

    @Autowired
    private UserProfileMapper delegateMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ProductRecommendationService service;
    private AtomicInteger profileQueries;
    private List<TProduct> candidates;

    @BeforeEach
    void setUp()
    {
        insertRepresentativeData();
        profileQueries = new AtomicInteger();
        UserProfileMapper countingMapper = countingMapper(delegateMapper);

        service = new ProductRecommendationService();
        ReflectionTestUtils.setField(service, "userProfileMapper", countingMapper);
        candidates = representativeProducts();
    }

    @Test
    void recommendationRemainsStableUnderRepresentativeConcurrentLoad() throws Exception
    {
        List<Long> expectedProductIds = productIds(service.recommendMall(7L, copyProducts(candidates)));
        for (int i = 0; i < 7; i++)
        {
            service.recommendMall(7L, copyProducts(candidates));
        }
        profileQueries.set(0);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();
        try
        {
            for (int i = 0; i < REQUEST_COUNT; i++)
            {
                futures.add(executor.submit(() -> {
                    List<TProduct> requestProducts = copyProducts(candidates);
                    start.await();
                    long started = System.nanoTime();
                    List<TProduct> result = service.recommendMall(7L, requestProducts);
                    long duration = System.nanoTime() - started;
                    if (result.size() != PRODUCT_COUNT
                        || !expectedProductIds.equals(productIds(result)))
                    {
                        throw new IllegalStateException("Concurrent recommendation result was unstable");
                    }
                    return duration;
                }));
            }

            long batchStarted = System.nanoTime();
            start.countDown();
            List<Long> durations = new ArrayList<>();
            for (Future<Long> future : futures)
            {
                durations.add(future.get(30, TimeUnit.SECONDS));
            }
            double elapsedSeconds = (System.nanoTime() - batchStarted) / 1_000_000_000D;
            Collections.sort(durations);
            double p95Millis = durations.get((int) Math.ceil(durations.size() * 0.95D) - 1) / 1_000_000D;
            double throughput = REQUEST_COUNT / elapsedSeconds;

            System.out.printf("RECOMMENDATION_PERF products=%d orders=%d concurrency=%d requests=%d "
                    + "elapsedMs=%.2f p95Ms=%.2f throughput=%.2f%n",
                PRODUCT_COUNT, ORDER_COUNT, CONCURRENCY, REQUEST_COUNT,
                elapsedSeconds * 1000D, p95Millis, throughput);

            assertEquals(REQUEST_COUNT, profileQueries.get(), "profile query count");
            assertTrue(p95Millis <= MAX_P95_MILLIS,
                "P95 recommendation latency exceeded " + MAX_P95_MILLIS + " ms: " + p95Millis);
            assertTrue(throughput >= MIN_THROUGHPUT,
                "Recommendation throughput was below " + MIN_THROUGHPUT + " req/s: " + throughput);
        }
        finally
        {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private UserProfileMapper countingMapper(UserProfileMapper delegate)
    {
        return (UserProfileMapper) Proxy.newProxyInstance(
            UserProfileMapper.class.getClassLoader(), new Class<?>[] { UserProfileMapper.class },
            (proxy, method, args) -> {
                if ("selectUserProfileByUserId".equals(method.getName()))
                {
                    profileQueries.incrementAndGet();
                }
                try
                {
                    return method.invoke(delegate, args);
                }
                catch (InvocationTargetException e)
                {
                    throw e.getCause();
                }
            });
    }

    private void insertRepresentativeData()
    {
        Date orderTime = Date.from(NOW.minusSeconds(10L * 24L * 60L * 60L));
        jdbcTemplate.update("insert into t_wxuser(id, openid, nickname) values (7, 'perf-user', 'Perf user')");
        jdbcTemplate.update("insert into t_user_profile(user_id, profile_status, profile_data, "
                + "calculate_time, create_time, update_time) values (7, 'READY', ?, ?, ?, ?)",
            profileData(), Date.from(NOW), Date.from(NOW), Date.from(NOW));

        jdbcTemplate.batchUpdate("insert into t_order(order_id, order_no, user_id, total_amount, pay_amount, "
            + "status, refund_status, pay_time, create_time) values (?, ?, 7, 50.00, 50.00, 1, 0, ?, ?)",
            new BatchPreparedStatementSetter()
            {
                @Override
                public void setValues(PreparedStatement statement, int index) throws SQLException
                {
                    long orderId = index + 1L;
                    statement.setLong(1, orderId);
                    statement.setString(2, "PERF-" + orderId);
                    statement.setTimestamp(3, new java.sql.Timestamp(orderTime.getTime()));
                    statement.setTimestamp(4, new java.sql.Timestamp(orderTime.getTime()));
                }

                @Override
                public int getBatchSize()
                {
                    return ORDER_COUNT;
                }
            });

        jdbcTemplate.batchUpdate("insert into t_order_item(order_id, product_id, product_name, price, "
            + "quantity, total_price) values (?, ?, ?, 50.00, 1, 50.00)",
            new BatchPreparedStatementSetter()
            {
                @Override
                public void setValues(PreparedStatement statement, int index) throws SQLException
                {
                    long productId = index % 500L + 1L;
                    statement.setLong(1, index + 1L);
                    statement.setLong(2, productId);
                    statement.setString(3, "Product " + productId);
                }

                @Override
                public int getBatchSize()
                {
                    return ORDER_COUNT;
                }
            });
    }

    private String profileData()
    {
        JSONObject root = new JSONObject(true);
        JSONObject mall = new JSONObject(true);
        JSONArray tags = new JSONArray();
        for (long id = 1; id <= 20; id++)
        {
            JSONObject item = new JSONObject(true);
            item.put("key", "product:" + id);
            item.put("dimension", "product");
            item.put("score", 21L - id);
            tags.add(item);
        }
        for (long id = 1; id <= 10; id++)
        {
            JSONObject item = new JSONObject(true);
            item.put("key", "category:" + id);
            item.put("dimension", "category");
            item.put("score", 11L - id);
            tags.add(item);
        }
        mall.put("tags", tags);
        root.put("MALL", mall);
        root.put("SCAN", new JSONObject(true));
        return root.toJSONString();
    }

    private List<TProduct> representativeProducts()
    {
        List<TProduct> products = new ArrayList<>(PRODUCT_COUNT);
        Date createTime = Date.from(NOW.minusSeconds(60L * 24L * 60L * 60L));
        for (long id = 1; id <= PRODUCT_COUNT; id++)
        {
            TProduct product = new TProduct();
            product.setProductId(id);
            product.setProductName("Product " + id);
            product.setCategoryId(id % 20L + 1L);
            product.setPrice(BigDecimal.valueOf(10L + id % 100L));
            product.setStock(100L);
            product.setCreateTime(createTime);
            products.add(product);
        }
        return Collections.unmodifiableList(products);
    }

    private List<TProduct> copyProducts(List<TProduct> source)
    {
        List<TProduct> copy = new ArrayList<>(source.size());
        for (TProduct original : source)
        {
            TProduct product = new TProduct();
            product.setProductId(original.getProductId());
            product.setProductName(original.getProductName());
            product.setCategoryId(original.getCategoryId());
            product.setPrice(original.getPrice());
            product.setStock(original.getStock());
            product.setCreateTime(original.getCreateTime());
            copy.add(product);
        }
        return copy;
    }

    private List<Long> productIds(List<TProduct> products)
    {
        List<Long> ids = new ArrayList<>(products.size());
        for (TProduct product : products)
        {
            ids.add(product.getProductId());
        }
        return ids;
    }

}
