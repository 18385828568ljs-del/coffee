package com.ruoyi.project.coffee.scanOrder.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/ScanTableQrcodeMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.scanOrder.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.scanOrder.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ScanTableQrcodeMapperIntegrationTest
{
    @Autowired
    private ScanTableQrcodeMapper scanTableQrcodeMapper;

    @Test
    void insertAndSelectByIdShouldPersistCoreFields()
    {
        ScanTableQrcode qrcode = qrcode("A01", "dine_in", 1);
        qrcode.setCreateTime(new Date());
        qrcode.setRemark("靠窗桌");

        assertEquals(1, scanTableQrcodeMapper.insert(qrcode));
        assertNotNull(qrcode.getTableId());

        ScanTableQrcode saved = scanTableQrcodeMapper.selectById(qrcode.getTableId());
        assertEquals("A01", saved.getTableNo());
        assertEquals("dine_in", saved.getScene());
        assertEquals("https://qrcode.example.com/A01.jpg", saved.getQrUrl());
        assertEquals(Integer.valueOf(1), saved.getStatus());
        assertEquals("靠窗桌", saved.getRemark());
    }

    @Test
    void selectByTableNoShouldMatchExactTable()
    {
        scanTableQrcodeMapper.insert(qrcode("A01", "dine_in", 1));
        scanTableQrcodeMapper.insert(qrcode("B02", "take_out", 1));

        ScanTableQrcode result = scanTableQrcodeMapper.selectByTableNo("A01");

        assertNotNull(result);
        assertEquals("A01", result.getTableNo());
    }

    @Test
    void selectListShouldFilterByTableStatusAndSceneThenOrderByTableId()
    {
        scanTableQrcodeMapper.insert(qrcode("B02", "take_out", 1));
        scanTableQrcodeMapper.insert(qrcode("A02", "dine_in", 0));
        scanTableQrcodeMapper.insert(qrcode("A01", "dine_in", 1));
        scanTableQrcodeMapper.insert(qrcode("A03", "dine_in", 1));

        ScanTableQrcode query = new ScanTableQrcode();
        query.setTableNo("A");
        query.setStatus(1);
        query.setScene("dine_in");

        List<ScanTableQrcode> results = scanTableQrcodeMapper.selectList(query);

        assertEquals(2, results.size());
        assertEquals("A01", results.get(0).getTableNo());
        assertEquals("A03", results.get(1).getTableNo());
    }

    @Test
    void updateShouldOnlyChangeProvidedFields()
    {
        ScanTableQrcode qrcode = qrcode("A01", "dine_in", 1);
        scanTableQrcodeMapper.insert(qrcode);

        ScanTableQrcode update = new ScanTableQrcode();
        update.setTableId(qrcode.getTableId());
        update.setQrUrl("https://qrcode.example.com/new.jpg");
        update.setStatus(0);
        update.setUpdateTime(new Date());

        assertEquals(1, scanTableQrcodeMapper.update(update));

        ScanTableQrcode saved = scanTableQrcodeMapper.selectById(qrcode.getTableId());
        assertEquals("A01", saved.getTableNo());
        assertEquals("dine_in", saved.getScene());
        assertEquals("https://qrcode.example.com/new.jpg", saved.getQrUrl());
        assertEquals(Integer.valueOf(0), saved.getStatus());
        assertNotNull(saved.getUpdateTime());
    }

    @Test
    void deleteByIdShouldRemoveOnlyTargetRecord()
    {
        ScanTableQrcode first = qrcode("A01", "dine_in", 1);
        ScanTableQrcode second = qrcode("A02", "dine_in", 1);
        scanTableQrcodeMapper.insert(first);
        scanTableQrcodeMapper.insert(second);

        assertEquals(1, scanTableQrcodeMapper.deleteById(first.getTableId()));

        assertNull(scanTableQrcodeMapper.selectById(first.getTableId()));
        assertNotNull(scanTableQrcodeMapper.selectById(second.getTableId()));
    }

    private static ScanTableQrcode qrcode(String tableNo, String scene, Integer status)
    {
        ScanTableQrcode qrcode = new ScanTableQrcode();
        qrcode.setTableNo(tableNo);
        qrcode.setScene(scene);
        qrcode.setQrUrl("https://qrcode.example.com/" + tableNo + ".jpg");
        qrcode.setStatus(status);
        return qrcode;
    }
}
