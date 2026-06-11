package com.ruoyi.project.coffee.image.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.image.domain.ImageAiBatchItemRequest;
import com.ruoyi.project.coffee.image.domain.ImageAiBatchItemResult;
import com.ruoyi.project.coffee.image.domain.ImageAiBatchStatusResult;
import com.ruoyi.project.coffee.image.domain.ImageAiGenerateResult;

@Service
public class ImageAiBatchService
{
    private static final int MAX_BATCH_SIZE = 50;

    private static final int BATCH_CONCURRENCY = 3;

    private static final String STATUS_WAITING = "等待中";

    private static final String STATUS_RUNNING = "生成中";

    private static final String STATUS_SUCCESS = "成功";

    private static final String STATUS_FAILED = "失败";

    private final ImageAiService imageAiService;

    private final ThreadPoolTaskExecutor taskExecutor;

    private final ConcurrentMap<String, BatchTask> tasks = new ConcurrentHashMap<String, BatchTask>();

    public ImageAiBatchService(ImageAiService imageAiService,
        @Qualifier("threadPoolTaskExecutor") ThreadPoolTaskExecutor taskExecutor)
    {
        this.imageAiService = imageAiService;
        this.taskExecutor = taskExecutor;
    }

    public ImageAiBatchStatusResult start(String productType, String prompt, List<ImageAiBatchItemRequest> requests)
    {
        imageAiService.validateProductType(productType);
        if (requests == null || requests.isEmpty())
        {
            throw new ServiceException("请选择已有主图的商品");
        }
        if (requests.size() > MAX_BATCH_SIZE)
        {
            throw new ServiceException("一次最多批量生成 " + MAX_BATCH_SIZE + " 张图片");
        }

        String batchId = UUID.randomUUID().toString().replace("-", "");
        BatchTask task = new BatchTask(batchId, productType, prompt, buildItems(requests));
        tasks.put(batchId, task);
        runBatch(task);
        return task.toStatusResult();
    }

    public ImageAiBatchStatusResult status(String batchId)
    {
        if (StringUtils.isEmpty(batchId))
        {
            throw new ServiceException("批量任务ID不能为空");
        }
        BatchTask task = tasks.get(batchId);
        if (task == null)
        {
            throw new ServiceException("批量任务不存在或已过期");
        }
        return task.toStatusResult();
    }

    private List<ImageAiBatchItemResult> buildItems(List<ImageAiBatchItemRequest> requests)
    {
        List<ImageAiBatchItemResult> items = new ArrayList<ImageAiBatchItemResult>();
        for (ImageAiBatchItemRequest request : requests)
        {
            ImageAiBatchItemResult item = new ImageAiBatchItemResult();
            item.setProductId(request.getProductId());
            item.setProductName(StringUtils.defaultString(request.getProductName(), "-"));
            item.setOriginalUrl(request.getImageUrl());
            item.setGeneratedUrl("");
            item.setStatus(STATUS_WAITING);
            item.setMessage("");
            items.add(item);
        }
        return items;
    }

    private void runBatch(BatchTask task)
    {
        int workerCount = Math.min(BATCH_CONCURRENCY, task.items.size());
        final AtomicInteger nextIndex = new AtomicInteger(0);
        final AtomicInteger remainingWorkers = new AtomicInteger(workerCount);
        for (int i = 0; i < workerCount; i++)
        {
            taskExecutor.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        while (true)
                        {
                            int index = nextIndex.getAndIncrement();
                            if (index >= task.items.size())
                            {
                                break;
                            }
                            generateItem(task, task.items.get(index));
                        }
                    }
                    finally
                    {
                        if (remainingWorkers.decrementAndGet() == 0)
                        {
                            task.running = false;
                        }
                    }
                }
            });
        }
    }

    private void generateItem(BatchTask task, ImageAiBatchItemResult item)
    {
        synchronized (item)
        {
            item.setStatus(STATUS_RUNNING);
            item.setMessage("");
            item.setGeneratedUrl("");
        }
        try
        {
            ImageAiGenerateResult result = imageAiService.generate(task.productType, item.getProductId(),
                item.getOriginalUrl(), task.prompt);
            synchronized (item)
            {
                item.setGeneratedUrl(result.getGeneratedUrl());
                item.setStatus(STATUS_SUCCESS);
                item.setMessage("");
            }
        }
        catch (RuntimeException e)
        {
            markFailed(item, StringUtils.defaultString(e.getMessage(), "生成失败"));
        }
    }

    private void markFailed(ImageAiBatchItemResult item, String message)
    {
        synchronized (item)
        {
            item.setStatus(STATUS_FAILED);
            item.setMessage(message);
        }
    }

    private static class BatchTask
    {
        private final String batchId;

        private final String productType;

        private final String prompt;

        private final List<ImageAiBatchItemResult> items;

        private volatile boolean running = true;

        private BatchTask(String batchId, String productType, String prompt, List<ImageAiBatchItemResult> items)
        {
            this.batchId = batchId;
            this.productType = productType;
            this.prompt = prompt;
            this.items = Collections.synchronizedList(items);
        }

        private ImageAiBatchStatusResult toStatusResult()
        {
            List<ImageAiBatchItemResult> snapshot = new ArrayList<ImageAiBatchItemResult>();
            int completed = 0;
            int success = 0;
            int failed = 0;
            synchronized (items)
            {
                for (ImageAiBatchItemResult item : items)
                {
                    ImageAiBatchItemResult copy = copyItem(item);
                    snapshot.add(copy);
                    if (STATUS_SUCCESS.equals(copy.getStatus()))
                    {
                        completed++;
                        success++;
                    }
                    else if (STATUS_FAILED.equals(copy.getStatus()))
                    {
                        completed++;
                        failed++;
                    }
                }
            }

            ImageAiBatchStatusResult result = new ImageAiBatchStatusResult();
            result.setBatchId(batchId);
            result.setProductType(productType);
            result.setRunning(running);
            result.setTotal(items.size());
            result.setCompleted(completed);
            result.setSuccess(success);
            result.setFailed(failed);
            result.setItems(snapshot);
            return result;
        }

        private ImageAiBatchItemResult copyItem(ImageAiBatchItemResult source)
        {
            synchronized (source)
            {
                ImageAiBatchItemResult copy = new ImageAiBatchItemResult();
                copy.setProductId(source.getProductId());
                copy.setProductName(source.getProductName());
                copy.setOriginalUrl(source.getOriginalUrl());
                copy.setGeneratedUrl(source.getGeneratedUrl());
                copy.setStatus(source.getStatus());
                copy.setMessage(source.getMessage());
                return copy;
            }
        }
    }
}
