package com.ruoyi.project.coffee.behavior.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户非交易行为证据。
 */
public class UserBehaviorEvent implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long eventId;
    private Long userId;
    private String eventType;
    private String scene;
    private Long productId;
    private Long categoryId;
    private Long sourceId;
    private String specJson;
    private String source;
    private String dedupKey;
    private Date eventTime;

    public Long getEventId()
    {
        return eventId;
    }

    public void setEventId(Long eventId)
    {
        this.eventId = eventId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getEventType()
    {
        return eventType;
    }

    public void setEventType(String eventType)
    {
        this.eventType = eventType;
    }

    public String getScene()
    {
        return scene;
    }

    public void setScene(String scene)
    {
        this.scene = scene;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public Long getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(Long sourceId)
    {
        this.sourceId = sourceId;
    }

    public String getSpecJson()
    {
        return specJson;
    }

    public void setSpecJson(String specJson)
    {
        this.specJson = specJson;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getDedupKey()
    {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey)
    {
        this.dedupKey = dedupKey;
    }

    public Date getEventTime()
    {
        return eventTime;
    }

    public void setEventTime(Date eventTime)
    {
        this.eventTime = eventTime;
    }
}
