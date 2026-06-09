package com.ruoyi.project.coffee.member.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.member.domain.TMember;

/**
 * 会员信息 Mapper
 */
public interface TMemberMapper
{
    TMember selectMemberByUserId(Long userId);

    List<TMember> selectAllMembers();

    int insertMember(TMember member);

    int updateMember(TMember member);

    int atomicAddSpending(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
