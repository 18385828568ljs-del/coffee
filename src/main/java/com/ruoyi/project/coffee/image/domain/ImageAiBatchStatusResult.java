package com.ruoyi.project.coffee.image.domain;

import java.util.List;

public class ImageAiBatchStatusResult
{
    private String batchId;

    private String productType;

    private boolean running;

    private int total;

    private int completed;

    private int success;

    private int failed;

    private List<ImageAiBatchItemResult> items;

    public String getBatchId()
    {
        return batchId;
    }

    public void setBatchId(String batchId)
    {
        this.batchId = batchId;
    }

    public String getProductType()
    {
        return productType;
    }

    public void setProductType(String productType)
    {
        this.productType = productType;
    }

    public boolean isRunning()
    {
        return running;
    }

    public void setRunning(boolean running)
    {
        this.running = running;
    }

    public int getTotal()
    {
        return total;
    }

    public void setTotal(int total)
    {
        this.total = total;
    }

    public int getCompleted()
    {
        return completed;
    }

    public void setCompleted(int completed)
    {
        this.completed = completed;
    }

    public int getSuccess()
    {
        return success;
    }

    public void setSuccess(int success)
    {
        this.success = success;
    }

    public int getFailed()
    {
        return failed;
    }

    public void setFailed(int failed)
    {
        this.failed = failed;
    }

    public List<ImageAiBatchItemResult> getItems()
    {
        return items;
    }

    public void setItems(List<ImageAiBatchItemResult> items)
    {
        this.items = items;
    }
}
