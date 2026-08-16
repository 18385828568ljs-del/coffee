package com.ruoyi.project.coffee.image.client;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
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
import com.ruoyi.project.coffee.image.service.ImageSourceDownloader;

@Component
@ConditionalOnProperty(prefix = "ai.image", name = "provider", havingValue = "images-edits")
public class ImagesEditsImageGenerationClient implements ImageGenerationClient
{
    private static final Pattern DATA_IMAGE_PATTERN = Pattern.compile(
        "data:image/[^;]+;base64,([A-Za-z0-9+/=\\r\\n]+)");
    private static final Pattern HTTP_IMAGE_PATTERN = Pattern.compile(
        "https?://[^\\s\\\"')]+", Pattern.CASE_INSENSITIVE);

    private final ImageAiProperties properties;
    private final ImageGenerationHttpTransport transport;
    private final ImageSourceDownloader downloader;

    public ImagesEditsImageGenerationClient(ImageAiProperties properties, ImageGenerationHttpTransport transport,
        ImageSourceDownloader downloader)
    {
        this.properties = properties;
        this.transport = transport;
        this.downloader = downloader;
    }

    @Override
    public ImageGenerationResult generate(ImageGenerationRequest request)
    {
        validate(request);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + properties.getApiKey());
        headers.put("Content-Type", "application/json");

        String response = transport.postJson(buildUrl(), headers, buildBody(request), properties.getTimeoutSeconds());
        Object parsed = JSON.parse(response);
        String imageBase64 = extractBase64(parsed);
        if (StringUtils.isNotEmpty(imageBase64))
        {
            return new ImageGenerationResult(normalizeBase64(imageBase64));
        }

        String imageUrl = extractImageUrl(parsed);
        if (StringUtils.isNotEmpty(imageUrl))
        {
            try
            {
                return new ImageGenerationResult(Base64.getEncoder().encodeToString(downloader.download(imageUrl)));
            }
            catch (IOException e)
            {
                throw new IllegalStateException("AI已返回图片地址，但下载失败：" + e.getMessage(), e);
            }
        }
        throw new IllegalStateException("模型未返回可识别的图片数据");
    }

    private void validate(ImageGenerationRequest request)
    {
        if (StringUtils.isEmpty(properties.getApiKey()))
        {
            throw new IllegalStateException("AI图片服务未配置，请填写 ai.image.api-key");
        }
        if (request == null || StringUtils.isEmpty(request.getSourceImageDataUrl()))
        {
            throw new IllegalStateException("图片编辑接口缺少参考图");
        }
    }

    private JSONObject buildBody(ImageGenerationRequest request)
    {
        JSONObject image = new JSONObject();
        image.put("image_url", request.getSourceImageDataUrl());
        JSONArray images = new JSONArray();
        images.add(image);

        JSONObject body = new JSONObject();
        body.put("model", properties.getModel());
        body.put("prompt", StringUtils.isNotEmpty(request.getPrompt()) ? request.getPrompt() : properties.getPrompt());
        body.put("images", images);
        return body;
    }

    private String buildUrl()
    {
        String baseUrl = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
        while (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        String endpoint = properties.getEndpoint();
        if (StringUtils.isEmpty(endpoint)) endpoint = "/v1/images/edits";
        if (!endpoint.startsWith("/")) endpoint = "/" + endpoint;
        return baseUrl + endpoint;
    }

    private String extractBase64(Object value)
    {
        if (value == null) return null;
        if (value instanceof String)
        {
            Matcher matcher = DATA_IMAGE_PATTERN.matcher((String) value);
            return matcher.find() ? matcher.group(1) : null;
        }
        if (value instanceof JSONObject)
        {
            JSONObject object = (JSONObject) value;
            String direct = firstString(object, "b64_json", "image_base64", "imageBase64");
            if (looksLikeBase64(direct)) return direct;
            for (Object child : object.values())
            {
                String found = extractBase64(child);
                if (StringUtils.isNotEmpty(found)) return found;
            }
        }
        if (value instanceof JSONArray)
        {
            for (Object child : (JSONArray) value)
            {
                String found = extractBase64(child);
                if (StringUtils.isNotEmpty(found)) return found;
            }
        }
        return null;
    }

    private String extractImageUrl(Object value)
    {
        if (value == null) return null;
        if (value instanceof String)
        {
            Matcher matcher = HTTP_IMAGE_PATTERN.matcher((String) value);
            return matcher.find() ? matcher.group() : null;
        }
        if (value instanceof JSONObject)
        {
            JSONObject object = (JSONObject) value;
            String direct = firstString(object, "url", "image_url", "imageUrl");
            if (isHttpUrl(direct)) return direct;
            for (Object child : object.values())
            {
                String found = extractImageUrl(child);
                if (StringUtils.isNotEmpty(found)) return found;
            }
        }
        if (value instanceof JSONArray)
        {
            for (Object child : (JSONArray) value)
            {
                String found = extractImageUrl(child);
                if (StringUtils.isNotEmpty(found)) return found;
            }
        }
        return null;
    }

    private String firstString(JSONObject object, String... keys)
    {
        for (String key : keys)
        {
            String value = object.getString(key);
            if (StringUtils.isNotEmpty(value)) return value;
        }
        return null;
    }

    private boolean looksLikeBase64(String value)
    {
        if (StringUtils.isEmpty(value) || value.length() < 100) return false;
        return normalizeBase64(value).matches("[A-Za-z0-9+/]+={0,2}");
    }

    private String normalizeBase64(String value)
    {
        return value.replace("\r", "").replace("\n", "").trim();
    }

    private boolean isHttpUrl(String value)
    {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }
}
