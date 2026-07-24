package com.ruoyi.project.coffee.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.abucoder.wxuser.service.IAbucoderWxuserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class WxUserTokenServiceTest
{
    private WxUserTokenService tokenService;

    @Mock
    private IAbucoderWxuserService wxuserService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        tokenService = new WxUserTokenService();
        ReflectionTestUtils.setField(tokenService, "wxuserService", wxuserService);
        ReflectionTestUtils.setField(tokenService, "configuredTokenSecret", "unit-test-secret");
        ReflectionTestUtils.setField(tokenService, "appSecret", "fallback-secret");
    }

    @Test
    void createTokenRejectsIncompleteUser()
    {
        AbucoderWxuser missingOpenid = new AbucoderWxuser();
        missingOpenid.setId(1L);

        assertThrows(IllegalArgumentException.class, () -> tokenService.createToken(null));
        assertThrows(IllegalArgumentException.class, () -> tokenService.createToken(missingOpenid));
    }

    @Test
    void createTokenCanResolveSameUser()
    {
        AbucoderWxuser user = buildUser(8L, "openid-8");
        when(wxuserService.selectAbucoderWxuserById(8L)).thenReturn(user);

        String token = tokenService.createToken(user);
        AbucoderWxuser resolved = tokenService.getUserByToken(token);

        assertNotNull(token);
        assertEquals(user, resolved);
    }

    @Test
    void getUserByTokenRejectsTamperedToken()
    {
        AbucoderWxuser user = buildUser(8L, "openid-8");
        String token = tokenService.createToken(user);

        assertNull(tokenService.getUserByToken(token + "x"));
    }

    @Test
    void getUserByTokenRejectsOpenidMismatch()
    {
        AbucoderWxuser tokenUser = buildUser(8L, "openid-8");
        AbucoderWxuser databaseUser = buildUser(8L, "openid-changed");
        when(wxuserService.selectAbucoderWxuserById(8L)).thenReturn(databaseUser);

        String token = tokenService.createToken(tokenUser);

        assertNull(tokenService.getUserByToken(token));
    }

    @Test
    void extractTokenPrefersBearerAuthorization()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc.def");
        request.addHeader("X-Wx-Token", "fallback-token");

        assertEquals("abc.def", tokenService.extractToken(request));
    }

    @Test
    void extractTokenFallsBackToWxTokenHeader()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Wx-Token", " wx-token ");

        assertEquals("wx-token", tokenService.extractToken(request));
    }

    private AbucoderWxuser buildUser(Long id, String openid)
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(id);
        user.setOpenid(openid);
        user.setNickname("测试用户");
        return user;
    }
}

