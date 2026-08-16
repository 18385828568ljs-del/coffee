package com.ruoyi.project.coffee.card.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.springframework.web.multipart.MultipartFile;

class CardBytesMultipartFile implements MultipartFile
{
    private final String originalFilename;
    private final byte[] bytes;

    CardBytesMultipartFile(String originalFilename, byte[] bytes)
    {
        this.originalFilename = originalFilename;
        this.bytes = bytes == null ? new byte[0] : bytes;
    }

    public String getName() { return "file"; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return "image/png"; }
    public boolean isEmpty() { return bytes.length == 0; }
    public long getSize() { return bytes.length; }
    public byte[] getBytes() { return bytes; }
    public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
    public void transferTo(File dest) throws IOException { FileUtils.writeByteArrayToFile(dest, bytes); }
}
