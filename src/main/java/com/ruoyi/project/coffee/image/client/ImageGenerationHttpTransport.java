package com.ruoyi.project.coffee.image.client;

import java.util.Map;
import com.alibaba.fastjson.JSONObject;

public interface ImageGenerationHttpTransport
{
    String postJson(String url, Map<String, String> headers, JSONObject body, int timeoutSeconds);

    String postMultipart(String url, Map<String, String> headers, Map<String, String> fields,
            String fileField, byte[] fileBytes, String fileName, int timeoutSeconds);
}
