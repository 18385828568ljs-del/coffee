package com.ruoyi.project.coffee.decorator.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.decorator.ai.profile.ComponentAiCapabilities;
import com.ruoyi.project.coffee.decorator.ai.profile.ComponentAiProfileService;
import com.ruoyi.project.coffee.decorator.context.DecoratorPermission;
import com.ruoyi.project.coffee.decorator.context.TenantContext;
import com.ruoyi.project.coffee.decorator.context.TenantContextHolder;
import com.ruoyi.project.coffee.decorator.context.TenantContextService;

class DecoratorAiCapabilitiesControllerTest
{
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test
    void requiresAssetViewBeforeReturningCapabilities()
    {
        ComponentAiProfileService profiles = mock(ComponentAiProfileService.class);
        TenantContextService contexts = mock(TenantContextService.class);
        when(profiles.capabilities("homeBanner")).thenReturn(new ComponentAiCapabilities());
        DecoratorAiCapabilitiesController controller = new DecoratorAiCapabilitiesController();
        ReflectionTestUtils.setField(controller, "profileService", profiles);
        ReflectionTestUtils.setField(controller, "contextService", contexts);
        TenantContext context = new TenantContext(1L, 2L, 3L, "OWNER", "ALL", Collections.<Long>emptySet());
        TenantContextHolder.set(context);
        controller.capabilities("homeBanner");
        verify(contexts).requirePermission(context, DecoratorPermission.ASSET_VIEW);
        verify(profiles).capabilities("homeBanner");
    }
}
