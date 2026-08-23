package com.ruoyi.project.coffee.decorator.asset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;
import com.ruoyi.project.coffee.decorator.mapper.BackgroundSlotMapper;

@Service
public class BackgroundSlotService
{
    @Autowired
    private BackgroundSlotMapper slotMapper;

    public BackgroundSlotSpec require(String componentKey)
    {
        if (componentKey == null || !componentKey.matches("^[A-Za-z][A-Za-z0-9]{1,63}$"))
        {
            throw new ServiceException("背景插槽不存在");
        }
        BackgroundSlotSpec slot = slotMapper.selectActiveByComponentKey(componentKey);
        if (slot == null) throw new ServiceException("背景插槽不存在或已停用: " + componentKey);
        // The built-in about-us.jpg is the source of truth for this widthFix image.
        if ("aboutImage".equals(componentKey))
        {
            slot.setOutputWidth(800);
            slot.setOutputHeight(1421);
            slot.setRenderMode("STRETCH");
        }
        return slot;
    }

    public BackgroundSlotSpec validateUpload(String componentKey, int width, int height, long byteSize)
    {
        BackgroundSlotSpec slot = require(componentKey);
        if (slot.getMaxFileSize() != null && byteSize > slot.getMaxFileSize())
        {
            throw new ServiceException("图片超过该插槽的文件大小限制");
        }
        if (width < slot.getUploadMinWidth() || height < slot.getUploadMinHeight())
        {
            throw new ServiceException("图片尺寸过小，至少需要 " + slot.getUploadMinWidth() + "×" + slot.getUploadMinHeight());
        }
        double actual = (double) width / (double) height;
        double expected = (double) slot.getOutputWidth() / (double) slot.getOutputHeight();
        if (Math.abs(actual - expected) / expected > 0.02d)
        {
            throw new ServiceException("图片比例不符合插槽要求，请使用 " + slot.getOutputWidth() + ":" + slot.getOutputHeight());
        }
        return slot;
    }
}
