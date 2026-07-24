package com.ruoyi.project.coffee.image.service;

import java.io.IOException;

public interface ImageSourceDownloader
{
    byte[] download(String imageUrl) throws IOException;
}
