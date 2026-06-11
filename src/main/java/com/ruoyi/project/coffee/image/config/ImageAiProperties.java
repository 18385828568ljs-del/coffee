package com.ruoyi.project.coffee.image.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.image")
public class ImageAiProperties
{
    private String provider = "chat-completions";

    private String baseUrl = "https://newapi.lxhei.xyz";

    private String endpoint = "/v1/chat/completions";

    private String apiKey = System.getenv("NEWAPI_API_KEY");

    private String model = "gpt-image-2";

    private int timeoutSeconds = 300;

    private int maxSourceImageBytes = 10 * 1024 * 1024;

    private String prompt = "请基于这张商品图生成一张更适合电商展示的咖啡商品主图：保留商品主体、包装轮廓和关键文字，明显提升清晰度、光影、色彩层次和高级质感；背景改为干净、有咖啡氛围的商业摄影背景；画面要比原图更精致、更有购买欲，不要生成低清、变形或多余文字。";

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public int getTimeoutSeconds()
    {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds)
    {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxSourceImageBytes()
    {
        return maxSourceImageBytes;
    }

    public void setMaxSourceImageBytes(int maxSourceImageBytes)
    {
        this.maxSourceImageBytes = maxSourceImageBytes;
    }

    public String getPrompt()
    {
        return prompt;
    }

    public void setPrompt(String prompt)
    {
        this.prompt = prompt;
    }
}
