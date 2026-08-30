package com.ruoyi.project.coffee.decorator.ai.provider;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.project.coffee.image.client.ImageGenerationClient;
import com.ruoyi.project.coffee.image.domain.ImageGenerationRequest;
import com.ruoyi.project.coffee.image.domain.ImageGenerationResult;
import com.ruoyi.project.coffee.image.service.ImageSourceDownloader;

class ImageApiDecoratorProviderTest
{
    @Test
    void passesValidatedDataUrlDirectlyToImageClient() throws Exception
    {
        ImageGenerationClient client = mock(ImageGenerationClient.class);
        ImageSourceDownloader downloader = mock(ImageSourceDownloader.class);
        ImageApiDecoratorProvider provider = new ImageApiDecoratorProvider();
        ReflectionTestUtils.setField(provider, "client", client);
        ReflectionTestUtils.setField(provider, "downloader", downloader);
        byte[] generated = new byte[] {1, 2, 3};
        when(client.generate(any(ImageGenerationRequest.class)))
                .thenReturn(new ImageGenerationResult(Base64.getEncoder().encodeToString(generated)));
        String guide = "data:image/png;base64,iVBORw0KGgo=";

        byte[] result = provider.generate("prompt", guide, false, 1500, 720);

        assertArrayEquals(generated, result);
        ArgumentCaptor<ImageGenerationRequest> request = ArgumentCaptor.forClass(ImageGenerationRequest.class);
        verify(client).generate(request.capture());
        org.junit.jupiter.api.Assertions.assertEquals(guide, request.getValue().getSourceImageDataUrl());
        verify(downloader, never()).download(any(String.class));
    }
}
