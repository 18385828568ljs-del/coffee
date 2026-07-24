package com.ruoyi.project.coffee.offlineActivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivity;
import com.ruoyi.project.coffee.offlineActivity.domain.OfflineActivitySignup;
import com.ruoyi.project.coffee.offlineActivity.mapper.OfflineActivityMapper;
import com.ruoyi.project.coffee.offlineActivity.mapper.OfflineActivitySignupMapper;
import com.ruoyi.project.coffee.offlineActivity.service.impl.OfflineActivityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class OfflineActivityServiceImplTest
{
    private OfflineActivityServiceImpl offlineActivityService;

    @Mock
    private OfflineActivityMapper offlineActivityMapper;

    @Mock
    private OfflineActivitySignupMapper signupMapper;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        offlineActivityService = new OfflineActivityServiceImpl();
        ReflectionTestUtils.setField(offlineActivityService, "offlineActivityMapper", offlineActivityMapper);
        ReflectionTestUtils.setField(offlineActivityService, "signupMapper", signupMapper);
    }

    @Test
    void insertActivityShouldNormalizeDefaultStatusSortAndQuota()
    {
        OfflineActivity activity = new OfflineActivity();
        activity.setQuota(0);
        when(offlineActivityMapper.insertOfflineActivity(activity)).thenReturn(1);

        int rows = offlineActivityService.insertOfflineActivity(activity);

        assertEquals(1, rows);
        assertEquals(OfflineActivityServiceImpl.ACTIVITY_STATUS_OFF, activity.getStatus());
        assertEquals(0, activity.getSortOrder());
        assertEquals(1, activity.getQuota());
        assertNotNull(activity.getCreateTime());
    }

    @Test
    void signupShouldCreateReservedSignupWhenActivityIsOpenAndQuotaAvailable()
    {
        OfflineActivity activity = availableActivity();
        OfflineActivitySignup saved = signup(88L, OfflineActivityServiceImpl.SIGNUP_STATUS_RESERVED);
        when(offlineActivityMapper.selectOfflineActivityByActivityId(3L)).thenReturn(activity);
        when(signupMapper.selectUserSignup(3L, 9L)).thenReturn(null);
        when(signupMapper.countReservedByActivityId(3L)).thenReturn(1);
        when(signupMapper.selectOfflineActivitySignupBySignupId(null)).thenReturn(saved);

        OfflineActivitySignup result = offlineActivityService.signup(3L, 9L);

        ArgumentCaptor<OfflineActivitySignup> captor = ArgumentCaptor.forClass(OfflineActivitySignup.class);
        verify(signupMapper).insertOfflineActivitySignup(captor.capture());
        OfflineActivitySignup inserted = captor.getValue();
        assertEquals(3L, inserted.getActivityId());
        assertEquals(9L, inserted.getUserId());
        assertEquals(OfflineActivityServiceImpl.SIGNUP_STATUS_RESERVED, inserted.getStatus());
        assertNotNull(inserted.getSignupTime());
        assertNotNull(inserted.getCreateTime());
        assertSame(saved, result);
    }

    @Test
    void signupShouldRejectClosedActivity()
    {
        OfflineActivity activity = availableActivity();
        activity.setStatus(OfflineActivityServiceImpl.ACTIVITY_STATUS_OFF);
        when(offlineActivityMapper.selectOfflineActivityByActivityId(3L)).thenReturn(activity);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> offlineActivityService.signup(3L, 9L)
        );

        assertEquals("活动暂未开放预约", exception.getMessage());
        verify(signupMapper, never()).insertOfflineActivitySignup(any());
    }

    @Test
    void signupShouldRejectWhenQuotaIsFull()
    {
        OfflineActivity activity = availableActivity();
        activity.setQuota(2);
        when(offlineActivityMapper.selectOfflineActivityByActivityId(3L)).thenReturn(activity);
        when(signupMapper.selectUserSignup(3L, 9L)).thenReturn(null);
        when(signupMapper.countReservedByActivityId(3L)).thenReturn(2);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> offlineActivityService.signup(3L, 9L)
        );

        assertEquals("活动名额已满", exception.getMessage());
        verify(signupMapper, never()).insertOfflineActivitySignup(any());
    }

    @Test
    void signupShouldRejectDuplicateReservedSignup()
    {
        OfflineActivity activity = availableActivity();
        when(offlineActivityMapper.selectOfflineActivityByActivityId(3L)).thenReturn(activity);
        when(signupMapper.selectUserSignup(3L, 9L)).thenReturn(
            signup(7L, OfflineActivityServiceImpl.SIGNUP_STATUS_RESERVED)
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> offlineActivityService.signup(3L, 9L)
        );

        assertEquals("你已经预约过该活动", exception.getMessage());
        verify(signupMapper, never()).insertOfflineActivitySignup(any());
    }

    @Test
    void signupShouldReactivateCancelledSignup()
    {
        OfflineActivity activity = availableActivity();
        OfflineActivitySignup cancelled = signup(7L, OfflineActivityServiceImpl.SIGNUP_STATUS_CANCELLED);
        cancelled.setCancelTime(new Date());
        cancelled.setCheckinTime(new Date());
        when(offlineActivityMapper.selectOfflineActivityByActivityId(3L)).thenReturn(activity);
        when(signupMapper.selectUserSignup(3L, 9L)).thenReturn(cancelled);
        when(signupMapper.countReservedByActivityId(3L)).thenReturn(0);
        when(signupMapper.selectOfflineActivitySignupBySignupId(7L)).thenReturn(cancelled);

        OfflineActivitySignup result = offlineActivityService.signup(3L, 9L);

        assertEquals(OfflineActivityServiceImpl.SIGNUP_STATUS_RESERVED, cancelled.getStatus());
        assertNotNull(cancelled.getSignupTime());
        assertNull(cancelled.getCancelTime());
        assertNull(cancelled.getCheckinTime());
        assertNotNull(cancelled.getUpdateTime());
        verify(signupMapper).updateOfflineActivitySignup(cancelled);
        assertSame(cancelled, result);
    }

    @Test
    void cancelSignupShouldRejectAfterActivityStarted()
    {
        OfflineActivity activity = availableActivity();
        activity.setStartTime(new Date(System.currentTimeMillis() - 1000L));
        when(offlineActivityMapper.selectOfflineActivityByActivityId(3L)).thenReturn(activity);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> offlineActivityService.cancelSignup(3L, 9L)
        );

        assertEquals("活动已开始，不能取消预约", exception.getMessage());
        verify(signupMapper, never()).updateOfflineActivitySignup(any());
    }

    @Test
    void cancelSignupShouldMarkReservedSignupCancelled()
    {
        OfflineActivity activity = availableActivity();
        OfflineActivitySignup signup = signup(7L, OfflineActivityServiceImpl.SIGNUP_STATUS_RESERVED);
        when(offlineActivityMapper.selectOfflineActivityByActivityId(3L)).thenReturn(activity);
        when(signupMapper.selectUserSignup(3L, 9L)).thenReturn(signup);
        when(signupMapper.updateOfflineActivitySignup(signup)).thenReturn(1);

        int rows = offlineActivityService.cancelSignup(3L, 9L);

        assertEquals(1, rows);
        assertEquals(OfflineActivityServiceImpl.SIGNUP_STATUS_CANCELLED, signup.getStatus());
        assertNotNull(signup.getCancelTime());
        assertNotNull(signup.getUpdateTime());
    }

    @Test
    void markSignupStatusShouldRejectInvalidStatus()
    {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> offlineActivityService.markSignupStatus(7L, 1)
        );

        assertEquals("状态参数错误", exception.getMessage());
        verify(signupMapper, never()).markSignupStatus(any(), any());
    }

    @Test
    void markSignupStatusShouldOnlyAllowAttendedOrAbsent()
    {
        when(signupMapper.markSignupStatus(7L, OfflineActivityServiceImpl.SIGNUP_STATUS_ATTENDED)).thenReturn(1);

        int rows = offlineActivityService.markSignupStatus(7L, OfflineActivityServiceImpl.SIGNUP_STATUS_ATTENDED);

        assertEquals(1, rows);
        verify(signupMapper).markSignupStatus(7L, OfflineActivityServiceImpl.SIGNUP_STATUS_ATTENDED);
    }

    private OfflineActivity availableActivity()
    {
        OfflineActivity activity = new OfflineActivity();
        activity.setActivityId(3L);
        activity.setStatus(OfflineActivityServiceImpl.ACTIVITY_STATUS_ON);
        activity.setQuota(3);
        activity.setStartTime(new Date(System.currentTimeMillis() + 86400000L));
        activity.setSignupDeadline(new Date(System.currentTimeMillis() + 3600000L));
        return activity;
    }

    private OfflineActivitySignup signup(Long signupId, Integer status)
    {
        OfflineActivitySignup signup = new OfflineActivitySignup();
        signup.setSignupId(signupId);
        signup.setActivityId(3L);
        signup.setUserId(9L);
        signup.setStatus(status);
        return signup;
    }
}
