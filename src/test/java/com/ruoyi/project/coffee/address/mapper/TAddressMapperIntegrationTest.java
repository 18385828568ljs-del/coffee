package com.ruoyi.project.coffee.address.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.address.domain.TAddress;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/TAddressMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.address.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.address.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TAddressMapperIntegrationTest
{
    @Autowired
    private TAddressMapper addressMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectTAddressListShouldFilterByUserAndOrderNewestFirst()
    {
        insertAddress(1L, 88L, "旧地址", 1, "2026-06-17 09:00:00");
        insertAddress(2L, 88L, "新地址", 0, "2026-06-17 10:00:00");
        insertAddress(3L, 99L, "其他用户地址", 1, "2026-06-17 11:00:00");

        TAddress query = new TAddress();
        query.setUserId(88L);
        List<TAddress> addresses = addressMapper.selectTAddressList(query);

        assertEquals(2, addresses.size());
        assertEquals("新地址", addresses.get(0).getDetailAddress());
        assertEquals("旧地址", addresses.get(1).getDetailAddress());
        assertEquals(Long.valueOf(88L), addresses.get(0).getUserId());
    }

    @Test
    void selectTAddressListShouldFilterDefaultAddressForCurrentUser()
    {
        insertAddress(1L, 88L, "默认地址", 1, "2026-06-17 09:00:00");
        insertAddress(2L, 88L, "普通地址", 0, "2026-06-17 10:00:00");
        insertAddress(3L, 99L, "其他用户默认地址", 1, "2026-06-17 11:00:00");

        TAddress query = new TAddress();
        query.setUserId(88L);
        query.setIsDefault(1);
        List<TAddress> addresses = addressMapper.selectTAddressList(query);

        assertEquals(1, addresses.size());
        assertEquals("默认地址", addresses.get(0).getDetailAddress());
    }

    @Test
    void insertUpdateDeleteShouldRoundTripAddressFields()
    {
        TAddress address = buildAddress(66L, "新增地址", 1);

        assertEquals(1, addressMapper.insertTAddress(address));
        assertNotNull(address.getAddressId());

        address.setReceiverName("修改用户");
        address.setDetailAddress("修改地址");
        address.setIsDefault(0);
        assertEquals(1, addressMapper.updateTAddress(address));

        TAddress saved = addressMapper.selectTAddressByAddressId(address.getAddressId());
        assertEquals("修改用户", saved.getReceiverName());
        assertEquals("修改地址", saved.getDetailAddress());
        assertEquals(Integer.valueOf(0), saved.getIsDefault());

        assertEquals(1, addressMapper.deleteTAddressByAddressId(address.getAddressId()));
        assertNull(addressMapper.selectTAddressByAddressId(address.getAddressId()));
    }

    @Test
    void deleteTAddressByAddressIdsShouldDeleteOnlyTargetIds()
    {
        insertAddress(1L, 88L, "地址一", 0, "2026-06-17 09:00:00");
        insertAddress(2L, 88L, "地址二", 0, "2026-06-17 10:00:00");
        insertAddress(3L, 88L, "地址三", 0, "2026-06-17 11:00:00");

        assertEquals(2, addressMapper.deleteTAddressByAddressIds(new String[] {"1", "3"}));

        assertNull(addressMapper.selectTAddressByAddressId(1L));
        assertNotNull(addressMapper.selectTAddressByAddressId(2L));
        assertNull(addressMapper.selectTAddressByAddressId(3L));
    }

    private TAddress buildAddress(Long userId, String detailAddress, Integer isDefault)
    {
        TAddress address = new TAddress();
        address.setUserId(userId);
        address.setReceiverName("测试用户");
        address.setReceiverPhone("13800000000");
        address.setProvince("广东省");
        address.setCity("广州市");
        address.setDistrict("天河区");
        address.setDetailAddress(detailAddress);
        address.setIsDefault(isDefault);
        return address;
    }

    private void insertAddress(Long addressId, Long userId, String detailAddress, Integer isDefault, String createTime)
    {
        jdbcTemplate.update(
            "insert into t_address(address_id, user_id, receiver_name, receiver_phone, province, city, district, detail_address, is_default, create_time) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            addressId,
            userId,
            "测试用户",
            "13800000000",
            "广东省",
            "广州市",
            "天河区",
            detailAddress,
            isDefault,
            Timestamp.valueOf(createTime)
        );
    }
}
