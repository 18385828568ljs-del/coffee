package com.ruoyi.project.coffee.behavior.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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
    public static final String EVENT_SEARCH = "SEARCH";
    public static final String EVENT_CART_ADD = "CART_ADD";
    public static final String EVENT_CART_REMOVE = "CART_REMOVE";
    public static final String SCENE_MALL = "MALL";
    public static final String SCENE_SCAN = "SCAN";
    public static final String SOURCE_DEFAULT_LIST = "DEFAULT_LIST";
    public static final String SOURCE_PERSONALIZED_LIST = "PERSONALIZED_LIST";
    public static final String SOURCE_CATEGORY = "CATEGORY";
    public static final String SOURCE_SEARCH = "SEARCH";

    private static final int MAX_SEARCH_KEYWORD_LENGTH = 50;

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorEventService.class);

    @Autowired
    private UserBehaviorEventMapper userBehaviorEventMapper;

    /** 记录商品详情查看，同一用户、场景、商品每天只形成一次证据。 */
    public boolean recordProductView(Long userId, String scene, Long productId, Long categoryId)
    {
        return recordProductView(userId, scene, productId, categoryId, SOURCE_DEFAULT_LIST);
    }

    public boolean recordProductView(Long userId, String scene, Long productId, Long categoryId, String source)
    {
        if (productId == null)
        {
            return false;
        }
        String dedupKey = buildDailyDedupKey(EVENT_PRODUCT_VIEW, scene, userId, String.valueOf(productId));
        return record(userId, EVENT_PRODUCT_VIEW, scene, productId, categoryId, null,
            null, normalizeSource(source), dedupKey);
    }

    /** 记录商品搜索，同一用户、场景、关键词每天只形成一次证据。 */
    public boolean recordSearch(Long userId, String scene, String keyword)
    {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null)
        {
            return false;
        }
        String dedupKey = buildDailyDedupKey(EVENT_SEARCH, scene, userId,
            normalizedKeyword.toLowerCase(Locale.ROOT));
        return record(userId, EVENT_SEARCH, scene, null, null, null,
            normalizedKeyword, SOURCE_SEARCH, dedupKey);
    }

    /**
     * 记录购物车行首次成功加购。购物车行 ID 保证数量累加不重复记证据。
     */
    public boolean recordFirstCartAdd(Long userId, String scene, Long productId, Long categoryId, Long cartId)
    {
        return recordFirstCartAdd(userId, scene, productId, categoryId, cartId, SOURCE_DEFAULT_LIST);
    }

    public boolean recordFirstCartAdd(Long userId, String scene, Long productId, Long categoryId,
        Long cartId, String source)
    {
        if (cartId == null)
        {
            return false;
        }
        String dedupKey = EVENT_CART_ADD + ":" + scene + ":" + cartId;
        return record(userId, EVENT_CART_ADD, scene, productId, categoryId, cartId,
            null, normalizeSource(source), dedupKey);
    }

    /** 记录购物车行成功移出，按购物车行 ID 去重。 */
    public boolean recordCartRemove(Long userId, String scene, Long productId, Long categoryId, Long cartId)
    {
        if (cartId == null)
        {
            return false;
        }
        String dedupKey = EVENT_CART_REMOVE + ":" + scene + ":" + cartId;
        return record(userId, EVENT_CART_REMOVE, scene, productId, categoryId, cartId,
            null, SOURCE_DEFAULT_LIST, dedupKey);
    }

    private boolean record(Long userId, String eventType, String scene, Long productId,
        Long categoryId, Long sourceId, String searchKeyword, String source, String dedupKey)
    {
        if (userId == null || !hasText(eventType) || !hasText(scene)
            || (productId == null && !EVENT_SEARCH.equals(eventType)))
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
        event.setSearchKeyword(searchKeyword);
        event.setSource(source);
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

    private String buildDailyDedupKey(String eventType, String scene, Long userId, String subject)
    {
        return eventType + ":" + scene + ":" + userId + ":" + subject + ":"
            + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String normalizeKeyword(String keyword)
    {
        if (!hasText(keyword))
        {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.length() <= MAX_SEARCH_KEYWORD_LENGTH
            ? normalized : normalized.substring(0, MAX_SEARCH_KEYWORD_LENGTH);
    }

    private String normalizeSource(String source)
    {
        if (SOURCE_DEFAULT_LIST.equals(source) || SOURCE_PERSONALIZED_LIST.equals(source)
            || SOURCE_CATEGORY.equals(source) || SOURCE_SEARCH.equals(source))
        {
            return source;
        }
        return SOURCE_DEFAULT_LIST;
    }

    private boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }
}
