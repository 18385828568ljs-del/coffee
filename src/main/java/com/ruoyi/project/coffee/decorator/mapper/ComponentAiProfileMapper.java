package com.ruoyi.project.coffee.decorator.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.decorator.ai.profile.ComponentAiProfile;

public interface ComponentAiProfileMapper
{
    ComponentAiProfile selectActive(@Param("slotKey") String slotKey);
    List<ComponentAiProfile> selectActiveList();
}
