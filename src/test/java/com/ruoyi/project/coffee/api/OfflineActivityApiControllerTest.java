package com.ruoyi.project.coffee.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.auth.WxUserTokenService;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivity;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivitySignup;
import com.ruoyi.project.coffee.offlineActivity.service.IOfflineActivityService;

class OfflineActivityApiControllerTest
{
    @Mock
    private IOfflineActivityService offlineActivityService;

    @Mock
    private WxUserTokenService wxUserTokenService;

    @Mock
    private HttpServletRequest request;

    private OfflineActivityApiController controller;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new OfflineActivityApiController();
        ReflectionTestUtils.setField(controller, "offlineActivityService", offlineActivityService);
        ReflectionTestUtils.setField(controller, "wxUserTokenService", wxUserTokenService);
    }

    @AfterEach
    void tearDown()
    {
        WxUserAuthContext.clear();
    }

    @Test
    void listShouldResolveUserIdFromRequestAndReturnAvailableActivities()
    {
        List<OfflineActivity> activities = Collections.singletonList(new OfflineActivity());
        when(wxUserTokenService.resolveUserId(request)).thenReturn(11L);
        when(offlineActivityService.selectAvailableOfflineActivityList(11L)).thenReturn(activities);

        AjaxResult result = controller.list(request);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(activities, result.get(AjaxResult.DATA_TAG));
        verify(wxUserTokenService).resolveUserId(request);
        verify(offlineActivityService).selectAvailableOfflineActivityList(11L);
    }

    @Test
    void detailShouldReturnActivityWhenAvailable()
    {
        OfflineActivity activity = new OfflineActivity();
        when(wxUserTokenService.resolveUserId(request)).thenReturn(12L);
        when(offlineActivityService.selectAvailableOfflineActivityById(3L, 12L)).thenReturn(activity);

        AjaxResult result = controller.detail(3L, request);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(activity, result.get(AjaxResult.DATA_TAG));
    }

    @Test
    void detailShouldReturnErrorWhenActivityMissing()
    {
        when(wxUserTokenService.resolveUserId(request)).thenReturn(12L);
        when(offlineActivityService.selectAvailableOfflineActivityById(3L, 12L)).thenReturn(null);

        AjaxResult result = controller.detail(3L, request);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("活动不存在或已下架", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void signupShouldUseCurrentUserAndReturnSignup()
    {
        bindUser(15L);
        OfflineActivitySignup signup = new OfflineActivitySignup();
        when(offlineActivityService.signup(3L, 15L)).thenReturn(signup);

        AjaxResult result = controller.signup(bodyWithActivityId(3L));

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(signup, result.get(AjaxResult.DATA_TAG));
        verify(offlineActivityService).signup(3L, 15L);
    }

    @Test
    void signupShouldReturnServiceErrorMessage()
    {
        bindUser(15L);
        when(offlineActivityService.signup(3L, 15L)).thenThrow(new IllegalArgumentException("活动名额已满"));

        AjaxResult result = controller.signup(bodyWithActivityId(3L));

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("活动名额已满", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void signupShouldReturnInvalidNumberErrorWhenActivityIdInvalid()
    {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("activityId", "abc");

        AjaxResult result = controller.signup(body);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("For input string: \"abc\"", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void cancelShouldUseCurrentUserAndReturnRows()
    {
        bindUser(16L);
        when(offlineActivityService.cancelSignup(5L, 16L)).thenReturn(1);

        AjaxResult result = controller.cancel(bodyWithActivityId("5"));

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals(1, result.get(AjaxResult.DATA_TAG));
        verify(offlineActivityService).cancelSignup(5L, 16L);
    }

    @Test
    void myShouldQueryCurrentUserSignups()
    {
        bindUser(17L);
        List<OfflineActivitySignup> signups = Collections.singletonList(new OfflineActivitySignup());
        when(offlineActivityService.selectUserSignupList(17L)).thenReturn(signups);

        AjaxResult result = controller.my();

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertSame(signups, result.get(AjaxResult.DATA_TAG));
        verify(offlineActivityService).selectUserSignupList(17L);
    }

    private static Map<String, Object> bodyWithActivityId(Object activityId)
    {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("activityId", activityId);
        return body;
    }

    private static void bindUser(Long userId)
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(userId);
        user.setOpenid("openid-" + userId);
        WxUserAuthContext.setCurrentUser(user);
    }
}
