package com.ruoyi.project.coffee.image.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.image.config.ImageAiProperties;
import com.ruoyi.project.coffee.image.domain.ImageGenerationRequest;
import com.ruoyi.project.coffee.image.domain.ImageGenerationResult;

@Component
@ConditionalOnProperty(prefix = "ai.image", name = "provider", havingValue = "images-edits")
public class ImagesEditsImageGenerationClient implements ImageGenerationClient
{
    private final ImageAiProperties properties;

    public ImagesEditsImageGenerationClient(ImageAiProperties properties)
    {
        this.properties = properties;
    }

    @Override
    public ImageGenerationResult generate(ImageGenerationRequest request)
    {
        validateConfiguration();
        SourceImage sourceImage = decodeSourceImage(request.getSourceImageDataUrl());
        String boundary = "----CoffeeImage" + UUID.randomUUID().toString().replace("-", "");
        HttpURLConnection connection = null;
        try
        {
            byte[] body = buildMultipartBody(request, sourceImage, boundary);
            connection = (HttpURLConnection) new URL(buildUrl()).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(toTimeoutMillis());
            connection.setReadTimeout(toTimeoutMillis());
            connection.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream())
            {
                output.write(body);
            }

            int status = connection.getResponseCode();
            InputStream responseStream = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
            String response = readBody(responseStream);
            if (status < 200 || status >= 300)
            {
                throw new IllegalStateException("AI图片服务请求失败，HTTP状态：" + status + "，响应：" + response);
            }
            String imageBase64 = extractImageBase64(response);
            if (StringUtils.isEmpty(imageBase64))
            {
                throw new IllegalStateException("模型未返回图片");
            }
            return new ImageGenerationResult(imageBase64.replace("\r", "").replace("\n", ""));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("AI图片服务请求失败：" + e.getMessage(), e);
        }
        finally
        {
            if (connection != null)
            {
                connection.disconnect();
            }
        }
    }

    private void validateConfiguration()
    {
        if (StringUtils.isEmpty(properties.getApiKey()))
        {
            throw new IllegalStateException("AI图片服务未配置，请先设置 API Key");
        }
    }

    private byte[] buildMultipartBody(ImageGenerationRequest request, SourceImage sourceImage, String boundary)
        throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeField(output, boundary, "model", properties.getModel());
        writeField(output, boundary, "prompt",
            StringUtils.isNotEmpty(request.getPrompt()) ? request.getPrompt() : properties.getPrompt());
        writeField(output, boundary, "response_format", "b64_json");
        writeImage(output, boundary, sourceImage);
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private void writeField(ByteArrayOutputStream output, String boundary, String name, String value)
        throws IOException
    {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
            .getBytes(StandardCharsets.UTF_8));
        output.write((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeImage(ByteArrayOutputStream output, String boundary, SourceImage sourceImage)
        throws IOException
    {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"image\"; filename=\""
            + sourceImage.filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + sourceImage.mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(sourceImage.bytes);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private SourceImage decodeSourceImage(String dataUrl)
    {
        if (StringUtils.isEmpty(dataUrl) || !dataUrl.startsWith("data:"))
        {
            throw new IllegalStateException("原图数据格式不正确");
        }
        int marker = dataUrl.indexOf(";base64,");
        if (marker < 0)
        {
            throw new IllegalStateException("原图数据格式不正确");
        }
        String mimeType = dataUrl.substring(5, marker);
        try
        {
            byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(marker + 8).replaceAll("\\s", ""));
            return new SourceImage(bytes, mimeType, fileNameFor(mimeType));
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalStateException("原图数据格式不正确", e);
        }
    }

    private String extractImageBase64(String response)
    {
        if (StringUtils.isEmpty(response))
        {
            return null;
        }
        JSONObject body = JSONObject.parseObject(response);
        JSONArray data = body.getJSONArray("data");
        if (data == null || data.isEmpty())
        {
            return null;
        }
        JSONObject first = data.getJSONObject(0);
        return first == null ? null : first.getString("b64_json");
    }

    private String readBody(InputStream input) throws IOException
    {
        if (input == null)
        {
            return "";
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                body.append(line);
            }
        }
        return body.toString();
    }

    private String buildUrl()
    {
        String baseUrl = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
        while (baseUrl.endsWith("/"))
        {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String endpoint = properties.getEndpoint();
        if (StringUtils.isEmpty(endpoint))
        {
            return baseUrl;
        }
        return baseUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
    }

    private int toTimeoutMillis()
    {
        long millis = Math.max(1, properties.getTimeoutSeconds()) * 1000L;
        return (int) Math.min(Integer.MAX_VALUE, millis);
    }

    private String fileNameFor(String mimeType)
    {
        if ("image/jpeg".equalsIgnoreCase(mimeType))
        {
            return "source.jpg";
        }
        if ("image/webp".equalsIgnoreCase(mimeType))
        {
            return "source.webp";
        }
        if ("image/gif".equalsIgnoreCase(mimeType))
        {
            return "source.gif";
        }
        return "source.png";
    }

    private static class SourceImage
    {
        private final byte[] bytes;

        private final String mimeType;

        private final String filename;

        private SourceImage(byte[] bytes, String mimeType, String filename)
        {
            this.bytes = bytes;
            this.mimeType = mimeType;
            this.filename = filename;
        }
    }
}
