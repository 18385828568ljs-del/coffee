package com.ruoyi.project.common.storage;

public class StoredFileInfo
{
    private final String url;

    private final String fileName;

    private final String newFileName;

    private final String originalFilename;

    public StoredFileInfo(String url, String fileName, String newFileName, String originalFilename)
    {
        this.url = url;
        this.fileName = fileName;
        this.newFileName = newFileName;
        this.originalFilename = originalFilename;
    }

    public String getUrl()
    {
        return url;
    }

    public String getFileName()
    {
        return fileName;
    }

    public String getNewFileName()
    {
        return newFileName;
    }

    public String getOriginalFilename()
    {
        return originalFilename;
    }
}
