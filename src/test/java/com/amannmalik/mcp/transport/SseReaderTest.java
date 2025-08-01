package com.amannmalik.mcp.transport;

import jakarta.json.JsonObject;
import jakarta.json.Json;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

public class SseReaderTest {
    @Test
    public void parsesEvents() throws Exception {
        String input = "id: test-1\n" +
                "data: {\"a\":1}\n" +
                "\n" +
                "id: test-2\n" +
                "data: {\"b\":2}\n" +
                "\n";
        BlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();
        java.util.Set<StreamableHttpClientTransport.SseReader> container = new java.util.HashSet<>();
        StreamableHttpClientTransport.SseReader reader = new StreamableHttpClientTransport.SseReader(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                queue,
                container
        );
        Thread t = new Thread(reader);
        t.start();
        t.join(100);
        reader.close();
        t.join(100);
        assertEquals(2, queue.size());
        JsonObject first = queue.take();
        assertEquals(Json.createObjectBuilder().add("a", 1).build(), first);
        JsonObject second = queue.take();
        assertEquals(Json.createObjectBuilder().add("b", 2).build(), second);
    }
}
