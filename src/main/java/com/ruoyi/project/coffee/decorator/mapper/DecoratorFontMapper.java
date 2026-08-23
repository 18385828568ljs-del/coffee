package com.ruoyi.project.coffee.decorator.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.decorator.font.domain.FontResource;

public interface DecoratorFontMapper
{
    List<FontResource> selectAvailableFonts(@Param("merchantId") Long merchantId);

    List<FontResource> selectUsableFonts(@Param("merchantId") Long merchantId,
            @Param("fontIds") List<Long> fontIds);
}
