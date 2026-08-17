package com.ruoyi.project.coffee.decorator.api;

public class DecoratorPublishRequest
{
    private Integer revision;
    private String idempotencyKey;
    private String publishNote;

    public Integer getRevision() { return revision; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getPublishNote() { return publishNote; }
    public void setPublishNote(String publishNote) { this.publishNote = publishNote; }
}
