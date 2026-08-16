package com.ruoyi.project.coffee.image.client;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

@Component
public class HutoolImageGenerationHttpTransport implements ImageGenerationHttpTransport
{
    private static final int MAX_ATTEMPTS = 3;

    @Override
    public String postJson(String url, Map<String, String> headers, JSONObject body, int timeoutSeconds)
    {
        RuntimeException lastException = null;
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)
        {
            int remainingTimeoutMillis = remainingTimeoutMillis(deadline);
            if (remainingTimeoutMillis <= 0)
            {
                break;
            }
            try
            {
                HttpResponse response = HttpRequest.post(url)
                    .addHeaders(headers)
                    .header("Connection", "close")
                    .body(body.toJSONString())
                    .timeout(remainingTimeoutMillis)
                    .execute();
                String responseBody = response.body();
                if (!response.isOk())
                {
                    String detail = extractErrorMessage(responseBody);
                    throw new IllegalStateException("AI服务请求失败，HTTP状态：" + response.getStatus()
                        + (StringUtils.isBlank(detail) ? "" : "，原因：" + detail));
                }
                return responseBody;
            }
            catch (IllegalStateException e)
            {
                throw e;
            }
            catch (RuntimeException e)
            {
                lastException = e;
                if (!isRetryable(e) || attempt == MAX_ATTEMPTS)
                {
                    break;
                }
                sleepBeforeRetry(attempt, deadline);
            }
        }
        if (remainingTimeoutMillis(deadline) <= 0)
        {
            throw new IllegalStateException("AI图片生成超时，单张图片最多等待 " + timeoutSeconds + " 秒");
        }
        throw new IllegalStateException("AI服务连接中断，请稍后重试或减少批量数量：" + rootMessage(lastException));
    }

    private boolean isRetryable(RuntimeException e)
    {
        String message = rootMessage(e).toLowerCase();
        return message.contains("unexpected end of file")
            || message.contains("connection reset")
            || message.contains("connection refused")
            || message.contains("timeout")
            || message.contains("timed out")
            || message.contains("read timed out");
    }

    private int remainingTimeoutMillis(long deadline)
    {
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0)
        {
            return 0;
        }
        return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    }

    private void sleepBeforeRetry(int attempt, long deadline)
    {
        try
        {
            long sleepMillis = Math.min(1000L * attempt, Math.max(0L, deadline - System.currentTimeMillis()));
            if (sleepMillis > 0)
            {
                Thread.sleep(sleepMillis);
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI服务重试被中断");
        }
    }

    private String rootMessage(Throwable throwable)
    {
        if (throwable == null)
        {
            return "未知错误";
        }
        Throwable current = throwable;
        while (current.getCause() != null)
        {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private String extractErrorMessage(String responseBody)
    {
        if (StringUtils.isBlank(responseBody))
        {
            return null;
        }
        String message = null;
        try
        {
            JSONObject body = JSON.parseObject(responseBody);
            JSONObject error = body.getJSONObject("error");
            if (error != null)
            {
                message = error.getString("message");
            }
            if (StringUtils.isBlank(message)) message = body.getString("message");
            if (StringUtils.isBlank(message)) message = body.getString("msg");
        }
        catch (RuntimeException ignored)
        {
            message = responseBody.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        }
        if (StringUtils.isBlank(message))
        {
            return null;
        }
        String sanitized = message.replaceAll("data:image/[^;]+;base64,[A-Za-z0-9+/=\\r\\n]+", "[image data]")
            .replaceAll("\\s+", " ").trim();
        return sanitized.length() <= 500 ? sanitized : sanitized.substring(0, 500);
    }
}
