package com.ruoyi.project.common.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService
{
    StoredFileInfo upload(MultipartFile file) throws IOException;

    default List<StoredFileInfo> upload(List<MultipartFile> files) throws IOException
    {
        if (files == null || files.isEmpty())
        {
            return Collections.emptyList();
        }
        List<StoredFileInfo> storedFiles = new ArrayList<StoredFileInfo>(files.size());
        for (MultipartFile file : files)
        {
            storedFiles.add(upload(file));
        }
        return storedFiles;
    }
}
