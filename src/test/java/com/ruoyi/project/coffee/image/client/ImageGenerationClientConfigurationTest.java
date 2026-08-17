package com.ruoyi.project.coffee.image.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.ruoyi.project.coffee.image.config.ImageAiProperties;

class ImageGenerationClientConfigurationTest
{
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(ClientConfiguration.class)
        .withPropertyValues("ai.image.provider=images-edits");

    @Test
    void imagesEditsProviderCreatesImageGenerationClient()
    {
        contextRunner.run(context -> assertEquals(1,
            context.getBeansOfType(ImageGenerationClient.class).size()));
    }

    @Configuration
    @Import(ImageAiProperties.class)
    @ComponentScan(basePackageClasses = ImageGenerationClient.class)
    static class ClientConfiguration
    {
    }
}
