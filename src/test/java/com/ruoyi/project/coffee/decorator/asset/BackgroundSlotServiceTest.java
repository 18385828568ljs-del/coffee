package com.ruoyi.project.coffee.decorator.asset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;
import com.ruoyi.project.coffee.decorator.mapper.BackgroundSlotMapper;

class BackgroundSlotServiceTest
{
    @Mock private BackgroundSlotMapper slotMapper;
    private BackgroundSlotService service;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new BackgroundSlotService();
        ReflectionTestUtils.setField(service, "slotMapper", slotMapper);
        BackgroundSlotSpec slot = new BackgroundSlotSpec();
        slot.setComponentKey("homeBanner"); slot.setOutputWidth(1500); slot.setOutputHeight(720);
        slot.setUploadMinWidth(750); slot.setUploadMinHeight(360); slot.setMaxFileSize(10485760L);
        when(slotMapper.selectActiveByComponentKey("homeBanner")).thenReturn(slot);
        BackgroundSlotSpec about = new BackgroundSlotSpec();
        about.setComponentKey("aboutImage"); about.setOutputWidth(1372); about.setOutputHeight(2000);
        about.setUploadMinWidth(686); about.setUploadMinHeight(1000); about.setMaxFileSize(10485760L);
        when(slotMapper.selectActiveByComponentKey("aboutImage")).thenReturn(about);
    }

    @Test
    void validatesSizeRatioAndFileLimit()
    {
        assertDoesNotThrow(() -> service.validateUpload("homeBanner", 1500, 720, 1024));
        assertThrows(ServiceException.class, () -> service.validateUpload("homeBanner", 800, 800, 1024));
        assertThrows(ServiceException.class, () -> service.validateUpload("homeBanner", 1500, 720, 11000000));
    }

    @Test
    void identifiesMissingSlotInErrorMessage()
    {
        ServiceException error = assertThrows(ServiceException.class, () -> service.require("actionCard"));
        assertEquals("背景插槽不存在或已停用: actionCard", error.getMessage());
    }

    @Test
    void normalizesBuiltInAboutImageCanvas()
    {
        BackgroundSlotSpec slot = service.require("aboutImage");

        assertEquals(800, slot.getOutputWidth());
        assertEquals(1421, slot.getOutputHeight());
        assertEquals("STRETCH", slot.getRenderMode());
    }
}
