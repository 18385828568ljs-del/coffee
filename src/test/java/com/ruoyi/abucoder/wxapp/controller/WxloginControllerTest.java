package com.ruoyi.abucoder.wxapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.abucoder.wxuser.service.IAbucoderWxuserService;
import com.ruoyi.project.coffee.auth.WxUserTokenService;
import com.ruoyi.project.common.storage.FileStorageService;
import com.ruoyi.project.common.storage.StoredFileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

class WxloginControllerTest
{
    private WxloginController controller;

    @Mock
    private IAbucoderWxuserService wxuserService;

    @Mock
    private WxUserTokenService tokenService;

    @Mock
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        controller = new WxloginController();
        ReflectionTestUtils.setField(controller, "iAbucoderWxuserService", wxuserService);
        ReflectionTestUtils.setField(controller, "wxUserTokenService", tokenService);
        ReflectionTestUtils.setField(controller, "fileStorageService", fileStorageService);
        ReflectionTestUtils.setField(controller, "appId", "test-app-id");
        ReflectionTestUtils.setField(controller, "appSecret", "test-app-secret");
    }

    @Test
    void meReturnsExpiredWhenTokenInvalid()
    {
        when(tokenService.getUserByToken("invalid-token")).thenReturn(null);

        AjaxResult result = controller.me("Bearer invalid-token", null);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("登录已失效", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void meReturnsUserAndRefreshTokenWhenTokenValid()
    {
        AbucoderWxuser user = buildUser();
        when(tokenService.getUserByToken("valid-token")).thenReturn(user);
        when(tokenService.createToken(user)).thenReturn("new-token");

        AjaxResult result = controller.me("Bearer valid-token", null);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals(user, result.get("data"));
        assertEquals("new-token", result.get("token"));
    }

    @Test
    void saveUserInfoRequiresLoginState()
    {
        JSONObject body = new JSONObject();

        AjaxResult result = controller.saveUserInfo(body, null);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("缺少登录态", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void uploadAvatarRejectsEmptyFileWhenLoggedIn()
    {
        AbucoderWxuser user = buildUser();
        when(tokenService.getUserByToken("valid-token")).thenReturn(user);
        MockMultipartFile file = new MockMultipartFile("file", new byte[0]);

        AjaxResult result = controller.uploadAvatar(file, "Bearer valid-token", null);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("缺少头像文件", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void uploadAvatarReturnsStoredFileInfoWhenLoggedIn() throws Exception
    {
        AbucoderWxuser user = buildUser();
        StoredFileInfo storedFile = new StoredFileInfo(
            "https://example.com/avatar.jpg",
            "avatar.jpg",
            "coffee/avatar.jpg",
            "avatar-origin.jpg");
        when(tokenService.getUserByToken("valid-token")).thenReturn(user);
        when(fileStorageService.upload(any(MultipartFile.class))).thenReturn(storedFile);
        MockMultipartFile file = new MockMultipartFile("file", "avatar-origin.jpg", "image/jpeg", "avatar".getBytes("UTF-8"));

        AjaxResult result = controller.uploadAvatar(file, "Bearer valid-token", null);

        assertEquals(0, result.get(AjaxResult.CODE_TAG));
        assertEquals("https://example.com/avatar.jpg", result.get("url"));
        assertEquals("avatar.jpg", result.get("fileName"));
        assertNotNull(result.get("newFileName"));
    }

    private AbucoderWxuser buildUser()
    {
        AbucoderWxuser user = new AbucoderWxuser();
        user.setId(1L);
        user.setOpenid("openid-1");
        user.setNickname("测试用户");
        return user;
    }
}
