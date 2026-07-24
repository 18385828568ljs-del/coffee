package com.ruoyi.project.coffee.image.domain;

import java.util.List;

public class ImageAiBatchStartRequest
{
    private String productType;

    private String prompt;

    private List<ImageAiBatchItemRequest> items;

    public String getProductType()
    {
        return productType;
    }

    public void setProductType(String productType)
    {
        this.productType = productType;
    }

    public String getPrompt()
    {
        return prompt;
    }

    public void setPrompt(String prompt)
    {
        this.prompt = prompt;
    }

    public List<ImageAiBatchItemRequest> getItems()
    {
        return items;
    }

    public void setItems(List<ImageAiBatchItemRequest> items)
    {
        this.items = items;
    }
}
