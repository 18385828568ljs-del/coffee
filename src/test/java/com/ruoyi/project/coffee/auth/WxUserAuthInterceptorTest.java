package com.ruoyi.project.coffee.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;

class WxUserAuthInterceptorTest
{
    @Mock
    private WxUserTokenService wxUserTokenService;

    @Mock
    private HttpServletRequest request;

    private AutoCloseable mocks;

    private WxUserAuthInterceptor interceptor;

    @BeforeEach
    void setUp()
    {
        mocks = MockitoAnnotations.openMocks(this);
        interceptor = new WxUserAuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "wxUserTokenService", wxUserTokenService);
    }

    @AfterEach
    void tearDown() throws Exception
    {
        WxUserAuthContext.clear();
        mocks.close();
    }

    @Test
    void preHandleShouldRejectMissingTokenAndWriteUnauthorizedJson() throws Exception
    {
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(wxUserTokenService.resolveUser(request)).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("application/json;charset=UTF-8", response.getContentType());

        JSONObject body = JSONObject.parseObject(response.getContentAsString());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, body.getInteger(AjaxResult.CODE_TAG));
        assertEquals("登录已失效，请重新登录", body.getString(AjaxResult.MSG_TAG));
        assertNull(WxUserAuthContext.getCurrentUser());
        verify(wxUserTokenService).resolveUser(request);
    }

    @Test
    void preHandleShouldBindResolvedUserToCurrentContext() throws Exception
    {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AbucoderWxuser wxuser = new AbucoderWxuser();
        wxuser.setId(12L);
        wxuser.setOpenid("openid-12");
        when(wxUserTokenService.resolveUser(request)).thenReturn(wxuser);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertSame(wxuser, WxUserAuthContext.getCurrentUser());
        assertEquals(12L, WxUserAuthContext.getCurrentUserId());
        verify(wxUserTokenService).resolveUser(request);
    }

    @Test
    void afterCompletionShouldClearBoundUserContext()
    {
        AbucoderWxuser wxuser = new AbucoderWxuser();
        wxuser.setId(18L);
        wxuser.setOpenid("openid-18");
        WxUserAuthContext.setCurrentUser(wxuser);

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);

        assertNull(WxUserAuthContext.getCurrentUser());
        assertNull(WxUserAuthContext.getCurrentUserId());
    }
}
