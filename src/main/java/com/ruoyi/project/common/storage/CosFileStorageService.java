package com.ruoyi.project.common.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.framework.config.properties.CosStorageProperties;

@Service
public class CosFileStorageService implements FileStorageService
{
    private final CosStorageProperties cosStorageProperties;

    public CosFileStorageService(CosStorageProperties cosStorageProperties)
    {
        this.cosStorageProperties = cosStorageProperties;
    }

    @Override
    public StoredFileInfo upload(MultipartFile file) throws IOException
    {
        validateConfiguration();

        String key = buildObjectKey(file);
        COSClient cosClient = createCosClient();
        try (InputStream inputStream = file.getInputStream())
        {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            if (StringUtils.isNotEmpty(file.getContentType()))
            {
                metadata.setContentType(file.getContentType());
            }

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                cosStorageProperties.getBucket(),
                key,
                inputStream,
                metadata);
            cosClient.putObject(putObjectRequest);
        }
        catch (CosServiceException e)
        {
            throw new IOException("Upload to Tencent COS failed: " + e.getMessage(), e);
        }
        finally
        {
            cosClient.shutdown();
        }

        return new StoredFileInfo(
            buildFileUrl(key),
            key,
            FileUtils.getName(key),
            file.getOriginalFilename());
    }

    private COSClient createCosClient()
    {
        COSCredentials credentials = new BasicCOSCredentials(
            cosStorageProperties.getSecretId(),
            cosStorageProperties.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cosStorageProperties.getRegion()));
        return new COSClient(credentials, clientConfig);
    }

    private void validateConfiguration()
    {
        if (StringUtils.isEmpty(cosStorageProperties.getBucket())
            || StringUtils.isEmpty(cosStorageProperties.getRegion())
            || StringUtils.isEmpty(cosStorageProperties.getSecretId())
            || StringUtils.isEmpty(cosStorageProperties.getSecretKey())
            || StringUtils.isEmpty(cosStorageProperties.getBaseUrl()))
        {
            throw new IllegalStateException("COS is enabled, but bucket/region/secretId/secretKey/baseUrl is incomplete");
        }
    }

    private String buildObjectKey(MultipartFile file)
    {
        String prefix = normalizePrefix(cosStorageProperties.getKeyPrefix());
        String fileName = FileUtils.getName(FileUploadUtils.extractFilename(file));
        String dateFolder = DateUtils.dateTime();
        String relativePath = dateFolder + "/" + fileName;
        if (StringUtils.isEmpty(prefix))
        {
            return relativePath;
        }
        return prefix + "/" + relativePath;
    }

    private String buildFileUrl(String key)
    {
        return trimTrailingSlash(cosStorageProperties.getBaseUrl()) + "/" + key;
    }

    private String normalizePrefix(String prefix)
    {
        if (StringUtils.isEmpty(prefix))
        {
            return "";
        }
        String normalized = prefix.trim().replace("\\", "/");
        while (normalized.startsWith("/"))
        {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/"))
        {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String trimTrailingSlash(String value)
    {
        String normalized = StringUtils.trimToEmpty(value);
        while (normalized.endsWith("/"))
        {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
