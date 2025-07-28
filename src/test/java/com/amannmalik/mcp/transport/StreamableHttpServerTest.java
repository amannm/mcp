package com.amannmalik.mcp.transport;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamableHttpServerTest {
    private StreamableHttpServer server;
    private URI endpoint;
    private Thread worker;

    @BeforeEach
    void start() throws Exception {
        server = new StreamableHttpServer(0, "/mcp");
        endpoint = server.endpoint();
        worker = new Thread(() -> {
            try {
                JsonObject req = server.receive();
                if (req.containsKey("ping")) {
                    JsonObject resp = Json.createObjectBuilder().add("pong", true).build();
                    server.send(resp);
                }
            } catch (IOException ignored) {}
        });
        worker.start();
    }

    @AfterEach
    void stop() throws Exception {
        server.close();
        worker.join();
    }

    @Test
    void simplePost() throws Exception {
        try (StreamableHttpTransport client = new StreamableHttpTransport(endpoint)) {
            JsonObject req = Json.createObjectBuilder().add("ping", true).build();
            client.send(req);
            JsonObject resp = client.receive();
            assertEquals(Json.createObjectBuilder().add("pong", true).build(), resp);
        }
    }

    @Test
    void sseGet() throws Exception {
        try (StreamableHttpTransport client = new StreamableHttpTransport(endpoint)) {
            client.listen();
            JsonObject msg = Json.createObjectBuilder().add("hello", "world").build();
            server.send(msg);
            JsonObject recv = client.receive();
            assertEquals(msg, recv);
        }
    }
}
