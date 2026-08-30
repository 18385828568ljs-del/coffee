package com.ruoyi.project.coffee.image.client;

import java.net.Proxy;
import java.util.Map;
import org.springframework.stereotype.Component;
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
        return execute(url, headers, body, null, null, null, null, timeoutSeconds);
    }

    @Override
    public String postMultipart(String url, Map<String, String> headers, Map<String, String> fields,
            String fileField, byte[] fileBytes, String fileName, int timeoutSeconds)
    {
        return execute(url, headers, null, fields, fileField, fileBytes, fileName, timeoutSeconds);
    }

    private String execute(String url, Map<String, String> headers, JSONObject body,
            Map<String, String> fields, String fileField, byte[] fileBytes, String fileName,
            int timeoutSeconds)
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
                HttpRequest request = HttpRequest.post(url)
                    .addHeaders(headers)
                    .header("Connection", "close")
                    // Bypass Windows' ProxySelector for this integration. A broken
                    // system proxy must not prevent the request from reaching the API.
                    .setProxy(Proxy.NO_PROXY)
                    .setFollowRedirects(false)
                    .timeout(remainingTimeoutMillis);
                if (body != null) request.body(body.toJSONString());
                if (fields != null) for (Map.Entry<String, String> field : fields.entrySet())
                    request.form(field.getKey(), field.getValue());
                if (fileBytes != null) request.form(fileField, fileBytes, fileName);
                HttpResponse response = request.execute();
                String responseBody = response.body();
                if (!response.isOk())
                {
                    throw new IllegalStateException("AI服务请求失败，HTTP状态：" + response.getStatus()
                            + redirectMessage(response) + providerMessage(responseBody));
                }
                String contentType = response.header("Content-Type");
                if (looksLikeHtml(responseBody) || (contentType != null && !contentType.trim().isEmpty()
                        && !contentType.toLowerCase().contains("json")))
                {
                    throw new IllegalStateException("AI服务返回的不是 JSON: url=" + url
                            + ", status=" + response.getStatus()
                            + ", contentType=" + (contentType == null ? "未知" : contentType)
                            + ", location=" + location(response)
                            + ", bodyPreview=" + bodyPreview(responseBody)
                            + "；请检查 AI_IMAGE_BASE_URL 和 endpoint 是否指向 OpenAI 兼容 API");
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

    String providerMessage(String responseBody)
    {
        if (responseBody == null || responseBody.trim().isEmpty()) return "";
        String message = null;
        try
        {
            JSONObject root = JSONObject.parseObject(responseBody);
            JSONObject error = root.getJSONObject("error");
            if (error != null) message = error.getString("message");
            if (message == null) message = root.getString("message");
        }
        catch (RuntimeException ignored) { }
        if (message == null) message = responseBody;
        message = message.replaceAll("[\\p{Cntrl}]+", " ").trim();
        if (message.length() > 500) message = message.substring(0, 500);
        return message.isEmpty() ? "" : "，服务商信息：" + message;
    }

    private boolean looksLikeHtml(String responseBody)
    {
        if (responseBody == null) return false;
        String value = responseBody.trim().toLowerCase();
        return value.startsWith("<!doctype html") || value.startsWith("<html") || value.startsWith("<head");
    }

    private String redirectMessage(HttpResponse response)
    {
        String location = response.header("Location");
        return location == null || location.trim().isEmpty() ? "" : "，重定向地址：" + location.trim();
    }

    private String location(HttpResponse response)
    {
        String value = response.header("Location");
        return value == null || value.trim().isEmpty() ? "null" : value.trim();
    }

    private String bodyPreview(String responseBody)
    {
        if (responseBody == null || responseBody.trim().isEmpty()) return "";
        String value = responseBody.replaceAll("[\\p{Cntrl}]+", " ").trim();
        if (value.length() > 240) value = value.substring(0, 240);
        return value;
    }

    private boolean isRetryable(RuntimeException e)
    {
        String message = rootMessage(e).toLowerCase();
        return message.contains("unexpected end of file")
            || message.contains("connection reset")
            || message.contains("connection refused")
            || message.contains("failed to select a proxy")
            || message.contains("select a proxy")
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
}
