package com.ruoyi.project.coffee.behavior.service;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.ruoyi.project.coffee.behavior.domain.UserBehaviorEvent;
import com.ruoyi.project.coffee.behavior.mapper.UserBehaviorEventMapper;

/**
 * 用户非交易行为证据记录服务。
 */
@Service
public class UserBehaviorEventService
{
    public static final String EVENT_PRODUCT_VIEW = "PRODUCT_VIEW";
    public static final String EVENT_CART_ADD = "CART_ADD";
    public static final String SCENE_MALL = "MALL";
    public static final String SCENE_SCAN = "SCAN";

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorEventService.class);

    @Autowired
    private UserBehaviorEventMapper userBehaviorEventMapper;

    /**
     * 记录商品详情查看。查看行为允许重复保存。
     */
    public boolean recordProductView(Long userId, String scene, Long productId, Long categoryId)
    {
        return record(userId, EVENT_PRODUCT_VIEW, scene, productId, categoryId, null, null);
    }

    /**
     * 记录购物车行首次成功加购。购物车行 ID 保证数量累加不重复记证据。
     */
    public boolean recordFirstCartAdd(Long userId, String scene, Long productId, Long categoryId, Long cartId)
    {
        if (cartId == null)
        {
            return false;
        }
        String dedupKey = EVENT_CART_ADD + ":" + scene + ":" + cartId;
        return record(userId, EVENT_CART_ADD, scene, productId, categoryId, cartId, dedupKey);
    }

    private boolean record(Long userId, String eventType, String scene, Long productId,
        Long categoryId, Long sourceId, String dedupKey)
    {
        if (userId == null || productId == null || !hasText(eventType) || !hasText(scene))
        {
            return false;
        }

        UserBehaviorEvent event = new UserBehaviorEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setScene(scene);
        event.setProductId(productId);
        event.setCategoryId(categoryId);
        event.setSourceId(sourceId);
        event.setDedupKey(dedupKey);
        event.setEventTime(new Date());

        try
        {
            return userBehaviorEventMapper.insertUserBehaviorEvent(event) > 0;
        }
        catch (DuplicateKeyException e)
        {
            return false;
        }
        catch (Exception e)
        {
            // 行为证据不能阻塞浏览、加购或其他主业务流程。
            logger.warn("记录用户行为失败, eventType={}, scene={}, userId={}, productId={}",
                eventType, scene, userId, productId, e);
            return false;
        }
    }

    private boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }
}
