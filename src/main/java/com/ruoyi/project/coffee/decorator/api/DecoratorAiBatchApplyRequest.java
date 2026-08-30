package com.ruoyi.project.coffee.decorator.api;

import java.util.List;

public class DecoratorAiBatchApplyRequest extends DecoratorAiApplyRequest
{
    private List<Long> resultIds;

    public List<Long> getResultIds() { return resultIds; }
    public void setResultIds(List<Long> resultIds) { this.resultIds = resultIds; }
}
