package com.ruoyi.project.common.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import com.ruoyi.framework.config.RuoYiConfig;
import com.ruoyi.framework.config.properties.CosStorageProperties;

class CosFileStorageServiceTest
{
    @TempDir
    Path tempDir;

    @Test
    void uploadFallsBackToLocalProfileWhenCosCredentialsAreMissing() throws Exception
    {
        String previousProfile = RuoYiConfig.getProfile();
        try
        {
            new RuoYiConfig().setProfile(tempDir.toString());
            CosStorageProperties properties = new CosStorageProperties();
            properties.setBucket("configured-bucket");
            properties.setRegion("ap-guangzhou");
            properties.setBaseUrl("https://example.invalid");
            CosFileStorageService service = new CosFileStorageService(properties);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "fallback.png", "image/png", new byte[] { 1, 2, 3, 4 });

            StoredFileInfo result = service.upload(file);

            assertTrue(result.getUrl().startsWith("/profile/upload/"));
            assertEquals("fallback.png", result.getOriginalFilename());
            try (Stream<Path> paths = Files.walk(tempDir.resolve("upload")))
            {
                assertEquals(1L, paths.filter(Files::isRegularFile).count());
            }
        }
        finally
        {
            new RuoYiConfig().setProfile(previousProfile);
        }
    }
}
