package com.ruoyi.project.coffee.image.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import com.ruoyi.framework.config.RuoYiConfig;

class HttpImageSourceDownloaderTest
{
    @Test
    void bypassesJvmProxySelector() throws Exception
    {
        byte[] image = new byte[] { 1, 2, 3, 4 };
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/reference.png", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, image.length);
            try (OutputStream output = exchange.getResponseBody())
            {
                output.write(image);
            }
        });
        server.start();

        ProxySelector original = ProxySelector.getDefault();
        AtomicInteger selections = new AtomicInteger();
        ProxySelector.setDefault(new ProxySelector()
        {
            @Override
            public List<Proxy> select(URI uri)
            {
                selections.incrementAndGet();
                return Collections.singletonList(new Proxy(Proxy.Type.HTTP,
                        new InetSocketAddress("127.0.0.1", 1)));
            }

            @Override
            public void connectFailed(URI uri, SocketAddress address, IOException error)
            {
            }
        });

        try
        {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/reference.png";
            assertArrayEquals(image, new HttpImageSourceDownloader().download(url));
            assertEquals(0, selections.get());
        }
        finally
        {
            ProxySelector.setDefault(original);
            server.stop(0);
        }
    }

    @Test
    void readsLocalProfileResourceWithoutOpeningHttpConnection() throws Exception
    {
        Path profile = Files.createTempDirectory("coffee-profile");
        Path image = profile.resolve("2026/08/reference.png");
        Files.createDirectories(image.getParent());
        byte[] expected = new byte[] { 9, 8, 7 };
        Files.write(image, expected);

        String previousProfile = RuoYiConfig.getProfile();
        new RuoYiConfig().setProfile(profile.toString());
        try
        {
            assertArrayEquals(expected, new HttpImageSourceDownloader().download("/profile/2026/08/reference.png"));
        }
        finally
        {
            new RuoYiConfig().setProfile(previousProfile);
            assertTrue(Files.deleteIfExists(image));
            Files.deleteIfExists(image.getParent());
            Files.deleteIfExists(image.getParent().getParent());
            Files.deleteIfExists(profile);
        }
    }
}
