package com.ruoyi.project.coffee.image.client;

import java.util.Map;
import com.alibaba.fastjson.JSONObject;

public interface ImageGenerationHttpTransport
{
    String postJson(String url, Map<String, String> headers, JSONObject body, int timeoutSeconds);
}
