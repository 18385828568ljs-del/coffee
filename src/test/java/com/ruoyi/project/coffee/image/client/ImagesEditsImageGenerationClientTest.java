package com.ruoyi.project.coffee.image.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.project.coffee.image.config.ImageAiProperties;
import com.ruoyi.project.coffee.image.domain.ImageGenerationRequest;
import com.ruoyi.project.coffee.image.domain.ImageGenerationResult;
import com.ruoyi.project.coffee.image.service.ImageSourceDownloader;

@ExtendWith(MockitoExtension.class)
class ImagesEditsImageGenerationClientTest
{
    @Mock private ImageGenerationHttpTransport transport;
    @Mock private ImageSourceDownloader downloader;
    private ImageAiProperties properties;

    @BeforeEach
    void setUp()
    {
        properties = new ImageAiProperties();
        properties.setBaseUrl("https://apivibecoding.com/");
        properties.setEndpoint("/v1/images/edits");
        properties.setApiKey("test-key");
        properties.setModel("gpt-image-2");
    }

    @Test
    void sendsImagesEditsRequestAndReadsBase64Response()
    {
        String expected = Base64.getEncoder().encodeToString(new byte[128]);
        when(transport.postJson(eq("https://apivibecoding.com/v1/images/edits"), any(Map.class),
            any(JSONObject.class), eq(300))).thenReturn("{\"data\":[{\"b64_json\":\"" + expected + "\"}]}");
        ImagesEditsImageGenerationClient client = new ImagesEditsImageGenerationClient(properties, transport, downloader);

        ImageGenerationResult result = client.generate(new ImageGenerationRequest("data:image/png;base64,AAAA", "coffee"));

        assertEquals(expected, result.getImageBase64());
        verify(transport).postJson(eq("https://apivibecoding.com/v1/images/edits"), any(Map.class),
            any(JSONObject.class), eq(300));
    }

    @Test
    void downloadsUrlResponseAndReturnsBase64() throws Exception
    {
        byte[] bytes = "generated-image".getBytes(StandardCharsets.UTF_8);
        when(transport.postJson(any(), any(), any(), eq(300)))
            .thenReturn("{\"data\":[{\"url\":\"https://cdn.example/card.png\"}]}");
        when(downloader.download("https://cdn.example/card.png")).thenReturn(bytes);
        ImagesEditsImageGenerationClient client = new ImagesEditsImageGenerationClient(properties, transport, downloader);

        ImageGenerationResult result = client.generate(new ImageGenerationRequest("data:image/png;base64,AAAA", "coffee"));

        assertEquals(Base64.getEncoder().encodeToString(bytes), result.getImageBase64());
    }
}
