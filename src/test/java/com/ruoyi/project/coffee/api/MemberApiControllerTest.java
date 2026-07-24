package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.member.domain.TMember;
import com.ruoyi.project.coffee.member.service.MemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class MemberApiControllerTest
{
    private MemberApiController controller;

    @Mock
    private MemberService memberService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new MemberApiController();
        ReflectionTestUtils.setField(controller, "memberService", memberService);
    }

    @AfterEach
    void tearDown()
    {
        WxUserAuthContext.clear();
    }

    @Test
    void infoRequiresLogin()
    {
        AjaxResult result = controller.info();

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("未登录", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void infoReturnsCurrentUsersMember()
    {
        bindUser(3L);
        TMember member = new TMember();
        member.setUserId(3L);
        when(memberService.getOrCreateMember(3L)).thenReturn(member);

        AjaxResult result = controller.info();

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(member, result.get(AjaxResult.DATA_TAG));
    }

    @Test
    void levelConfigBuildsFourLevels()
    {
        when(memberService.getLevelNameByLevel(1)).thenReturn("初遇会员");
        when(memberService.getLevelNameByLevel(2)).thenReturn("常客会员");
        when(memberService.getLevelNameByLevel(3)).thenReturn("知味会员");
        when(memberService.getLevelNameByLevel(4)).thenReturn("臻享会员");
        when(memberService.getThresholdByLevel(org.mockito.ArgumentMatchers.anyInt())).thenReturn(BigDecimal.ZERO);
        when(memberService.getDiscountRateByLevel(org.mockito.ArgumentMatchers.anyInt())).thenReturn(BigDecimal.ONE);

        AjaxResult result = controller.levelConfig();

        List<?> data = (List<?>) result.get(AjaxResult.DATA_TAG);
        assertEquals(4, data.size());
        Map<?, ?> first = (Map<?, ?>) data.get(0);
        assertEquals(1, first.get("level"));
        assertEquals("初遇会员", first.get("levelName"));
        assertEquals("初", first.get("icon"));
    }

    private void bindUser(Long userId)
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(userId);
        user.setOpenid("openid-" + userId);
        WxUserAuthContext.setCurrentUser(user);
    }
}

