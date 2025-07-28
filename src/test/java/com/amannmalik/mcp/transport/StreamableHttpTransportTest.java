package com.amannmalik.mcp.transport;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamableHttpTransportTest {
    @Test
    void roundTrip() throws Exception {
        StreamableHttpTransport transport = new StreamableHttpTransport();
        try {
            JsonObject msg = Json.createObjectBuilder().add("ping", true).build();

            CompletableFuture<JsonObject> responseFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:" + transport.port() + "/"))
                            .header("Accept", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(msg.toString()))
                            .build();
                    HttpClient client = HttpClient.newHttpClient();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    return Json.createReader(new java.io.StringReader(resp.body())).readObject();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            JsonObject received = transport.receive();
            assertEquals(msg, received);
            transport.send(msg);
            JsonObject resp = responseFuture.get(2, TimeUnit.SECONDS);
            assertEquals(msg, resp);
        } finally {
            transport.close();
        }
    }

    @Test
    void sse() throws Exception {
        StreamableHttpTransport transport = new StreamableHttpTransport();
        try {
            JsonObject msg = Json.createObjectBuilder().add("hello", "world").build();

            CompletableFuture<JsonObject> eventFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:" + transport.port()))
                            .header("Accept", "text/event-stream")
                            .GET()
                            .build();
                    HttpClient client = HttpClient.newHttpClient();
                    HttpResponse<java.io.InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                        String line;
                        StringBuilder data = new StringBuilder();
                        while ((line = r.readLine()) != null) {
                            if (line.isEmpty()) break;
                            if (line.startsWith("data: ")) data.append(line.substring(6));
                        }
                        return Json.createReader(new java.io.StringReader(data.toString())).readObject();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            TimeUnit.MILLISECONDS.sleep(100); // wait for GET
            transport.send(msg);
            JsonObject event = eventFuture.get(2, TimeUnit.SECONDS);
            assertEquals(msg, event);
        } finally {
            transport.close();
        }
    }

    @Test
    void invalidOriginRejected() throws Exception {
        StreamableHttpTransport transport = new StreamableHttpTransport();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + transport.port() + "/"))
                    .header("Origin", "http://evil.com")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals(403, resp.statusCode());
        } finally {
            transport.close();
        }
    }
}
