package com.ruoyi.project.coffee.image.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.image.config.ImageAiProperties;
import com.ruoyi.project.coffee.image.domain.ImageGenerationRequest;
import com.ruoyi.project.coffee.image.domain.ImageGenerationResult;

@Component
@ConditionalOnProperty(prefix = "ai.image", name = "provider", havingValue = "chat-completions", matchIfMissing = true)
public class ChatCompletionsImageGenerationClient implements ImageGenerationClient
{
    private static final Pattern DATA_IMAGE_PATTERN = Pattern.compile("data:image/[^;]+;base64,([A-Za-z0-9+/=\\r\\n]+)");

    private final ImageAiProperties properties;

    private final ImageGenerationHttpTransport transport;

    public ChatCompletionsImageGenerationClient(ImageAiProperties properties, ImageGenerationHttpTransport transport)
    {
        this.properties = properties;
        this.transport = transport;
    }

    @Override
    public ImageGenerationResult generate(ImageGenerationRequest request)
    {
        validateConfiguration();

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + properties.getApiKey());
        headers.put("Content-Type", "application/json");

        String response = transport.postJson(buildUrl(), headers, buildBody(request), properties.getTimeoutSeconds());
        String imageBase64 = extractImageBase64(response);
        if (StringUtils.isEmpty(imageBase64))
        {
            throw new IllegalStateException("模型未返回图片，可能不支持图生图输入");
        }
        return new ImageGenerationResult(imageBase64.replace("\r", "").replace("\n", ""));
    }

    private void validateConfiguration()
    {
        if (StringUtils.isEmpty(properties.getApiKey()))
        {
            throw new IllegalStateException("AI图片服务未配置，请先设置 NEWAPI_API_KEY");
        }
    }

    private JSONObject buildBody(ImageGenerationRequest request)
    {
        JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", request.getSourceImageDataUrl());

        JSONObject textContent = new JSONObject();
        textContent.put("type", "text");
        textContent.put("text", StringUtils.isNotEmpty(request.getPrompt()) ? request.getPrompt() : properties.getPrompt());

        JSONObject imageContent = new JSONObject();
        imageContent.put("type", "image_url");
        imageContent.put("image_url", imageUrl);

        JSONArray content = new JSONArray();
        content.add(textContent);
        content.add(imageContent);

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", content);

        JSONArray messages = new JSONArray();
        messages.add(message);

        JSONObject body = new JSONObject();
        body.put("model", properties.getModel());
        body.put("stream", false);
        body.put("messages", messages);
        return body;
    }

    private String buildUrl()
    {
        String baseUrl = trimTrailingSlash(properties.getBaseUrl());
        String endpoint = properties.getEndpoint();
        if (StringUtils.isEmpty(endpoint))
        {
            return baseUrl;
        }
        if (!endpoint.startsWith("/"))
        {
            endpoint = "/" + endpoint;
        }
        return baseUrl + endpoint;
    }

    private String trimTrailingSlash(String value)
    {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/"))
        {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String extractImageBase64(String response)
    {
        if (StringUtils.isEmpty(response))
        {
            return null;
        }
        Object parsed = JSON.parse(response);
        return extractImageBase64FromObject(parsed);
    }

    @SuppressWarnings("unchecked")
    private String extractImageBase64FromObject(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof String)
        {
            Matcher matcher = DATA_IMAGE_PATTERN.matcher((String) value);
            return matcher.find() ? matcher.group(1) : null;
        }
        if (value instanceof JSONObject)
        {
            JSONObject object = (JSONObject) value;
            for (Map.Entry<String, Object> entry : object.entrySet())
            {
                String found = extractImageBase64FromObject(entry.getValue());
                if (StringUtils.isNotEmpty(found))
                {
                    return found;
                }
            }
            return null;
        }
        if (value instanceof JSONArray)
        {
            JSONArray array = (JSONArray) value;
            for (Object item : array)
            {
                String found = extractImageBase64FromObject(item);
                if (StringUtils.isNotEmpty(found))
                {
                    return found;
                }
            }
            return null;
        }
        if (value instanceof Map)
        {
            for (Object item : ((Map<String, Object>) value).values())
            {
                String found = extractImageBase64FromObject(item);
                if (StringUtils.isNotEmpty(found))
                {
                    return found;
                }
            }
            return null;
        }
        if (value instanceof List)
        {
            for (Object item : (List<?>) value)
            {
                String found = extractImageBase64FromObject(item);
                if (StringUtils.isNotEmpty(found))
                {
                    return found;
                }
            }
        }
        return null;
    }
}
