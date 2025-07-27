package com.amannmalik.mcp.transport;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamableHttpTransportTest {
    private HttpServer server;
    private URI endpoint;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/mcp", new Handler());
        server.start();
        endpoint = URI.create("http://localhost:" + server.getAddress().getPort() + "/mcp");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void simplePost() throws Exception {
        try (StreamableHttpTransport transport = new StreamableHttpTransport(endpoint)) {
            JsonObject req = Json.createObjectBuilder().add("ping", true).build();
            transport.send(req);
            JsonObject resp = transport.receive();
            assertEquals(Json.createObjectBuilder().add("pong", true).build(), resp);
        }
    }

    private static class Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream body = exchange.getRequestBody();
                try (JsonReader reader = Json.createReader(body)) {
                    JsonObject obj = reader.readObject();
                    if (obj.containsKey("ping")) {
                        JsonObject resp = Json.createObjectBuilder().add("pong", true).build();
                        byte[] bytes = resp.toString().getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(bytes);
                        }
                        return;
                    }
                }
                exchange.sendResponseHeaders(202, -1);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.close();
        }
    }
}
