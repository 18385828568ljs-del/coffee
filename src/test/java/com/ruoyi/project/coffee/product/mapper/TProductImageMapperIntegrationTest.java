package com.ruoyi.project.coffee.product.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ruoyi.project.coffee.product.domain.TProductImage;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ruoyi_product_image_mapper_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mybatis.mapper-locations=classpath:mybatis/coffee/TProductImageMapper.xml",
    "mybatis.type-aliases-package=com.ruoyi.project.coffee.product.domain"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.ruoyi.project.coffee.product.mapper")
@Sql(scripts = "/mapper-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TProductImageMapperIntegrationTest
{
    @Autowired
    private TProductImageMapper productImageMapper;

    @Test
    void batchInsertAndSelectShouldKeepProductImagesOrdered()
    {
        List<TProductImage> images = Arrays.asList(
            productImage(1001L, "detail.png", 1, 0),
            productImage(1001L, "cover.png", 0, 1),
            productImage(1002L, "other.png", 0, 1)
        );

        assertEquals(3, productImageMapper.batchInsertTProductImage(images));

        List<TProductImage> productImages = productImageMapper.selectTProductImageByProductId(1001L);
        assertEquals(2, productImages.size());
        assertEquals("cover.png", productImages.get(0).getImageUrl());
        assertEquals("detail.png", productImages.get(1).getImageUrl());

        List<TProductImage> batchImages = productImageMapper.selectTProductImageByProductIds(Arrays.asList(1002L, 1001L));
        assertEquals(3, batchImages.size());
        assertEquals("cover.png", batchImages.get(0).getImageUrl());
        assertEquals("detail.png", batchImages.get(1).getImageUrl());
        assertEquals("other.png", batchImages.get(2).getImageUrl());
    }

    @Test
    void deleteShouldRemoveImagesByProductIds()
    {
        productImageMapper.batchInsertTProductImage(Arrays.asList(
            productImage(1001L, "cover.png", 0, 1),
            productImage(1002L, "other.png", 0, 1)
        ));

        assertEquals(1, productImageMapper.deleteTProductImageByProductId(1001L));
        assertEquals(0, productImageMapper.selectTProductImageByProductId(1001L).size());
        assertEquals(1, productImageMapper.deleteTProductImageByProductIds(new Long[] {1002L}));
        assertEquals(0, productImageMapper.selectTProductImageByProductId(1002L).size());
    }

    private TProductImage productImage(Long productId, String imageUrl, Integer sortOrder, Integer isMain)
    {
        TProductImage image = new TProductImage();
        image.setProductId(productId);
        image.setImageUrl(imageUrl);
        image.setSortOrder(sortOrder);
        image.setIsMain(isMain);
        return image;
    }
}
