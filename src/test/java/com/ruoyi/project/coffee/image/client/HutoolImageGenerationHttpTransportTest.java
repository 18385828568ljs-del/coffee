package com.ruoyi.project.coffee.image.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.HttpServer;

class HutoolImageGenerationHttpTransportTest
{
    @Test
    void bypassesJvmProxySelector() throws Exception
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] response = "{\"data\":[]}".getBytes(StandardCharsets.UTF_8);
        server.createContext("/images", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream output = exchange.getResponseBody())
            {
                output.write(response);
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
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/images";
            String result = new HutoolImageGenerationHttpTransport().postJson(
                    url, Collections.emptyMap(), new JSONObject(), 5);

            assertEquals("{\"data\":[]}", result);
            assertEquals(0, selections.get());
        }
        finally
        {
            ProxySelector.setDefault(original);
            server.stop(0);
        }
    }
}
