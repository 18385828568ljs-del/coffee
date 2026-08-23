package com.ruoyi.project.coffee.decorator.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;

public interface BackgroundSlotMapper
{
    BackgroundSlotSpec selectActiveByComponentKey(@Param("componentKey") String componentKey);
}
