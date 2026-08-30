package com.ruoyi.project.coffee.image.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.project.coffee.image.config.ImageAiProperties;
import com.ruoyi.project.coffee.image.domain.ImageGenerationRequest;

class OpenAiImageGenerationClientTest
{
    @Mock private ImageGenerationHttpTransport transport;
    private ImageAiProperties properties;
    private OpenAiImageGenerationClient client;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        properties = new ImageAiProperties();
        properties.setBaseUrl("https://api.example.com/");
        properties.setEndpoint("/v1/images/generations");
        properties.setEditEndpoint("/v1/images/edits");
        properties.setApiKey("test-key");
        properties.setModel("gpt-image-2");
        client = new OpenAiImageGenerationClient(properties, transport);
    }

    @Test
    void textPromptUsesImageGenerationsEndpoint()
    {
        String encoded = Base64.getEncoder().encodeToString("image".getBytes(StandardCharsets.UTF_8));
        when(transport.postJson(anyString(), anyMap(), any(JSONObject.class), anyInt()))
                .thenReturn("{\"data\":[{\"b64_json\":\"" + encoded + "\"}]}");

        assertEquals(encoded, client.generate(new ImageGenerationRequest(null, "coffee", "opaque")).getImageBase64());

        ArgumentCaptor<JSONObject> body = ArgumentCaptor.forClass(JSONObject.class);
        verify(transport).postJson(eq("https://api.example.com/v1/images/generations"), anyMap(),
                body.capture(), eq(300));
        assertEquals("gpt-image-2", body.getValue().getString("model"));
        assertEquals("coffee", body.getValue().getString("prompt"));
        assertEquals("opaque", body.getValue().getString("background"));
        assertFalse(body.getValue().containsKey("messages"));
    }

    @Test
    void generationEndpointAliasBuildsThirdPartyGatewayUrl()
    {
        properties.setBaseUrl("https://apiclaude.cc");
        properties.setGenerationEndpoint("/v1/images/generations");
        String encoded = Base64.getEncoder().encodeToString("image".getBytes(StandardCharsets.UTF_8));
        when(transport.postJson(anyString(), anyMap(), any(JSONObject.class), anyInt()))
                .thenReturn("{\"data\":[{\"b64_json\":\"" + encoded + "\"}]}");

        client.generate(new ImageGenerationRequest(null, "coffee"));

        verify(transport).postJson(eq("https://apiclaude.cc/v1/images/generations"), anyMap(),
                any(JSONObject.class), eq(300));
    }

    @Test
    void doesNotDuplicateV1WhenBaseUrlAlreadyContainsApiPrefix()
    {
        properties.setBaseUrl("https://apiclaude.cc/v1");
        String encoded = Base64.getEncoder().encodeToString("image".getBytes(StandardCharsets.UTF_8));
        when(transport.postJson(anyString(), anyMap(), any(JSONObject.class), anyInt()))
                .thenReturn("{\"data\":[{\"b64_json\":\"" + encoded + "\"}]}");

        client.generate(new ImageGenerationRequest(null, "coffee"));

        verify(transport).postJson(eq("https://apiclaude.cc/v1/images/generations"), anyMap(),
                any(JSONObject.class), eq(300));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void referenceImageUsesMultipartEditEndpoint()
    {
        byte[] source = "source".getBytes(StandardCharsets.UTF_8);
        String generated = Base64.getEncoder().encodeToString("generated".getBytes(StandardCharsets.UTF_8));
        when(transport.postMultipart(anyString(), anyMap(), anyMap(), anyString(), any(byte[].class),
                anyString(), anyInt())).thenReturn("{\"data\":[{\"b64_json\":\"" + generated + "\"}]}");

        String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(source);
        assertEquals(generated, client.generate(new ImageGenerationRequest(dataUrl, "polish", null)).getImageBase64());

        ArgumentCaptor<Map> fields = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        verify(transport).postMultipart(eq("https://api.example.com/v1/images/edits"), anyMap(),
                fields.capture(), eq("image[]"), bytes.capture(), eq("reference.png"), eq(300));
        assertEquals("polish", fields.getValue().get("prompt"));
        assertArrayEquals(source, bytes.getValue());
    }

    @Test
    void portraitTargetUsesPortraitProviderSize()
    {
        String encoded = Base64.getEncoder().encodeToString("image".getBytes(StandardCharsets.UTF_8));
        when(transport.postJson(anyString(), anyMap(), any(JSONObject.class), anyInt()))
                .thenReturn("{\"data\":[{\"b64_json\":\"" + encoded + "\"}]}");

        client.generate(new ImageGenerationRequest(null, "portrait", "opaque", 800, 1421));

        ArgumentCaptor<JSONObject> body = ArgumentCaptor.forClass(JSONObject.class);
        verify(transport).postJson(anyString(), anyMap(), body.capture(), anyInt());
        assertEquals("1024x1536", body.getValue().getString("size"));
    }

    @Test
    void rejectsMissingImagePayload()
    {
        when(transport.postJson(anyString(), anyMap(), any(JSONObject.class), anyInt()))
                .thenReturn("{\"data\":[]}");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> client.generate(new ImageGenerationRequest(null, "coffee")));
        assertTrue(error.getMessage().contains("data[0].b64_json"));
    }

    @Test
    void rejectsHtmlHomepageResponseWithConfigurationHint()
    {
        when(transport.postJson(anyString(), anyMap(), any(JSONObject.class), anyInt()))
                .thenReturn("<!doctype html><html><head><title>Bing</title></head></html>");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> client.generate(new ImageGenerationRequest(null, "coffee")));
        assertTrue(error.getMessage().contains("不是 JSON"));
        assertTrue(error.getMessage().contains("AI_IMAGE_BASE_URL"));
    }

    @Test
    void extractsSafeProviderErrorMessage()
    {
        HutoolImageGenerationHttpTransport http = new HutoolImageGenerationHttpTransport();
        assertEquals("，服务商信息：This model is not supported on this endpoint",
                http.providerMessage("{\"error\":{\"message\":\"This model is not supported on this endpoint\"}}"));
        assertFalse(http.providerMessage("bad\r\nresponse").contains("\n"));
    }
}
