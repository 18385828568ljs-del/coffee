package com.ruoyi.project.coffee.decorator.api;

public class DecoratorDraftRequest
{
    private Integer revision;
    private Object config;

    public Integer getRevision() { return revision; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public Object getConfig() { return config; }
    public void setConfig(Object config) { this.config = config; }
}
