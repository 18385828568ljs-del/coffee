package com.ruoyi.project.coffee.image.client;

import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.image.config.ImageAiProperties;
import com.ruoyi.project.coffee.image.domain.ImageGenerationRequest;
import com.ruoyi.project.coffee.image.domain.ImageGenerationResult;

@Component
@ConditionalOnProperty(prefix = "ai.image", name = "provider", havingValue = "openai-images", matchIfMissing = true)
public class OpenAiImageGenerationClient implements ImageGenerationClient
{
    private static final Pattern DATA_IMAGE_PATTERN = Pattern.compile(
            "^data:(image/(?:png|jpeg|webp));base64,([A-Za-z0-9+/=\\r\\n]+)$");

    private final ImageAiProperties properties;
    private final ImageGenerationHttpTransport transport;

    public OpenAiImageGenerationClient(ImageAiProperties properties, ImageGenerationHttpTransport transport)
    {
        this.properties = properties;
        this.transport = transport;
    }

    @Override
    public ImageGenerationResult generate(ImageGenerationRequest request)
    {
        validateConfiguration();
        String response = StringUtils.isEmpty(request.getSourceImageDataUrl())
                ? generateFromText(request) : editImage(request);
        return new ImageGenerationResult(extractImageBase64(response));
    }

    private String generateFromText(ImageGenerationRequest request)
    {
        JSONObject body = new JSONObject();
        body.put("model", properties.getModel());
        body.put("prompt", prompt(request));
        body.put("n", 1);
        body.put("size", providerSize(request));
        body.put("output_format", "png");
        if (StringUtils.isNotEmpty(request.getBackground())) body.put("background", request.getBackground());
        Map<String, String> headers = authorizationHeaders();
        headers.put("Content-Type", "application/json");
        return transport.postJson(buildUrl(properties.getEndpoint()), headers, body,
                properties.getTimeoutSeconds());
    }

    private String editImage(ImageGenerationRequest request)
    {
        Matcher source = DATA_IMAGE_PATTERN.matcher(request.getSourceImageDataUrl());
        if (!source.matches()) throw new IllegalStateException("参考图必须是 PNG、JPEG 或 WebP Data URL");
        byte[] bytes;
        try { bytes = Base64.getMimeDecoder().decode(source.group(2)); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("参考图 Base64 无效"); }
        if (bytes.length == 0) throw new IllegalStateException("参考图内容为空");

        Map<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("model", properties.getModel());
        fields.put("prompt", prompt(request));
        fields.put("n", "1");
        fields.put("size", providerSize(request));
        fields.put("output_format", "png");
        if (StringUtils.isNotEmpty(request.getBackground())) fields.put("background", request.getBackground());
        return transport.postMultipart(buildUrl(properties.getEditEndpoint()), authorizationHeaders(), fields,
                "image[]", bytes, "reference." + extension(source.group(1)), properties.getTimeoutSeconds());
    }

    private String extractImageBase64(String response)
    {
        if (response != null && response.trim().toLowerCase().matches("^(<!doctype html|<html|<head).*"))
            throw new IllegalStateException("模型响应不是 JSON，AI_IMAGE_BASE_URL 可能被重定向到网站首页；请检查 API 地址和 endpoint");
        try
        {
            JSONObject root = JSONObject.parseObject(response);
            JSONArray data = root == null ? null : root.getJSONArray("data");
            String encoded = data == null || data.isEmpty() ? null : data.getJSONObject(0).getString("b64_json");
            if (StringUtils.isEmpty(encoded)) throw new IllegalStateException("模型未返回 data[0].b64_json");
            Base64.getMimeDecoder().decode(encoded);
            return encoded.replace("\r", "").replace("\n", "");
        }
        catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("模型响应格式无效: " + e.getMessage()); }
    }

    private Map<String, String> authorizationHeaders()
    {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + properties.getApiKey());
        return headers;
    }

    private void validateConfiguration()
    {
        if (StringUtils.isEmpty(properties.getApiKey()))
            throw new IllegalStateException("AI图片服务未配置，请设置 AI_IMAGE_API_KEY");
        if (StringUtils.isEmpty(properties.getBaseUrl())) throw new IllegalStateException("AI图片服务地址未配置");
        if (StringUtils.isEmpty(properties.getModel())) throw new IllegalStateException("AI图片模型未配置");
        validateUrl(properties.getBaseUrl(), "AI图片服务地址");
        validatePath(properties.getEndpoint(), "AI图片生成 endpoint");
        validatePath(properties.getEditEndpoint(), "AI图片编辑 endpoint");
    }

    private void validateUrl(String value, String label)
    {
        try
        {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IllegalArgumentException();
        }
        catch (RuntimeException e) { throw new IllegalStateException(label + "必须是 http(s) 的 API 主机地址，不能填写网站首页或带用户信息的 URL"); }
    }

    private void validatePath(String value, String label)
    {
        String path = value == null ? "" : value.trim();
        if (path.isEmpty() || !path.startsWith("/") || path.contains("#") || path.contains("//"))
            throw new IllegalStateException(label + "必须是以 / 开头的 API 路径");
    }

    private String prompt(ImageGenerationRequest request)
    {
        return StringUtils.isNotEmpty(request.getPrompt()) ? request.getPrompt() : properties.getPrompt();
    }

    private String providerSize(ImageGenerationRequest request)
    {
        if (request.getTargetWidth() <= 0 || request.getTargetHeight() <= 0) return "1536x1024";
        double ratio = (double) request.getTargetWidth() / (double) request.getTargetHeight();
        if (ratio < 0.85d) return "1024x1536";
        if (ratio > 1.18d) return "1536x1024";
        return "1024x1024";
    }

    private String buildUrl(String endpoint)
    {
        String base = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String path = endpoint == null ? "" : endpoint.trim();
        if (base.endsWith("/v1") && path.startsWith("/v1/")) path = path.substring(3);
        return base + (path.startsWith("/") ? path : "/" + path);
    }

    private String extension(String mimeType)
    {
        return "image/jpeg".equals(mimeType) ? "jpg" : "image/webp".equals(mimeType) ? "webp" : "png";
    }
}
