package com.ruoyi.project.coffee.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import com.ruoyi.project.coffee.member.domain.TMember;
import com.ruoyi.project.coffee.member.mapper.TMemberMapper;
import com.ruoyi.project.system.config.service.IConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class MemberServiceTest
{
    private MemberService memberService;

    @Mock
    private TMemberMapper memberMapper;

    @Mock
    private IConfigService configService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        memberService = new MemberService();
        ReflectionTestUtils.setField(memberService, "memberMapper", memberMapper);
        ReflectionTestUtils.setField(memberService, "configService", configService);
    }

    @Test
    void getLevelNameFallsBackWhenConfigMissing()
    {
        when(configService.selectConfigByKey("coffee.member.name.lv2")).thenReturn("");

        assertEquals("常客会员", memberService.getLevelNameByLevel(2));
    }

    @Test
    void getThresholdUsesConfiguredPositiveValue()
    {
        when(configService.selectConfigByKey("coffee.member.threshold.lv3")).thenReturn(" 999.50 ");

        assertEquals(new BigDecimal("999.50"), memberService.getThresholdByLevel(3));
    }

    @Test
    void getDiscountFallsBackForInvalidConfig()
    {
        when(configService.selectConfigByKey("coffee.member.discount.lv3")).thenReturn("bad-number");

        assertEquals(new BigDecimal("0.95"), memberService.getDiscountRateByLevel(3));
    }

    @Test
    void getOrCreateMemberInitializesNewMember()
    {
        when(memberMapper.selectMemberByUserId(8L)).thenReturn(null);

        TMember member = memberService.getOrCreateMember(8L);

        assertEquals(8L, member.getUserId());
        assertEquals(1, member.getLevel());
        assertEquals("初遇会员", member.getLevelName());
        assertEquals(BigDecimal.ZERO, member.getTotalSpending());
    }
}

