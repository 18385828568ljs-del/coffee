package com.ruoyi.project.coffee.profile.task;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.profile.service.UserProfileService;

class UserProfileTaskTest
{
    @Mock
    private UserProfileService userProfileService;

    private UserProfileTask task;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        task = new UserProfileTask();
        ReflectionTestUtils.setField(task, "userProfileService", userProfileService);
    }

    @Test
    void incrementalRefreshContinuesAfterOneUserFails()
    {
        when(userProfileService.selectChangedUserIds()).thenReturn(Arrays.asList(1L, 2L));
        doThrow(new IllegalStateException("calculation failed"))
            .when(userProfileService).recalculateUser(1L);

        task.refreshIncrementalProfiles();

        verify(userProfileService).recalculateUser(1L);
        verify(userProfileService).recalculateUser(2L);
    }

    @Test
    void fullRefreshCleansExpiredBehaviorBeforeCalibration()
    {
        when(userProfileService.selectAllUserIds()).thenReturn(Collections.singletonList(3L));

        task.refreshAllProfiles();

        verify(userProfileService).deleteExpiredBehavior();
        verify(userProfileService).recalculateUser(3L);
    }
}
