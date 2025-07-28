package com.amannmalik.mcp.server.tools;

import com.sun.net.httpserver.HttpServer;
import jakarta.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WebApiToolProviderTest {
    private HttpServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] data = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void listAndCall() {
        WebApiToolProvider p = new WebApiToolProvider();
        ToolPage page = p.list(null);
        assertEquals(1, page.tools().size());
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        ToolResult result = p.call("http_get", Json.createObjectBuilder().add("url", url).build());
        assertFalse(result.isError());
        assertEquals("ok", result.content().getJsonObject(0).getString("text"));
    }
}
