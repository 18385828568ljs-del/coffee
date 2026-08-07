package com.ruoyi.project.abucoder.wxuser.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.abucoder.wxuser.mapper.AbucoderWxuserMapper;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;

class AbucoderWxuserServiceImplTest
{
    @Mock
    private AbucoderWxuserMapper wxuserMapper;

    @Mock
    private UserProfileMapper userProfileMapper;

    private AbucoderWxuserServiceImpl service;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new AbucoderWxuserServiceImpl();
        ReflectionTestUtils.setField(service, "abucoderWxuserMapper", wxuserMapper);
        ReflectionTestUtils.setField(service, "userProfileMapper", userProfileMapper);
    }

    @Test
    void deleteUserFirstPreventsLateEvidenceThenCleansExistingData()
    {
        String[] userIds = { "7", "8" };
        when(wxuserMapper.deleteAbucoderWxuserByIds(userIds)).thenReturn(2);

        assertEquals(2, service.deleteAbucoderWxuserByIds("7,8"));

        InOrder order = inOrder(userProfileMapper, wxuserMapper);
        order.verify(wxuserMapper).deleteAbucoderWxuserByIds(userIds);
        order.verify(userProfileMapper).deleteBehaviorByUserIds(userIds);
        order.verify(userProfileMapper).deleteProfilesByUserIds(userIds);
    }
}
