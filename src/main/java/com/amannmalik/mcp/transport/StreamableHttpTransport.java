package com.amannmalik.mcp.transport;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class StreamableHttpTransport implements Transport {
    private final HttpClient client;
    private final URI endpoint;
    private final BlockingQueue<JsonObject> queue = new ArrayBlockingQueue<>(16);
    private volatile InputStream sseStream;

    public StreamableHttpTransport(URI endpoint) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.endpoint = endpoint;
    }

    @Override
    public void send(JsonObject message) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(message.toString()))
                .build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (contentType.startsWith("text/event-stream")) {
                if (sseStream != null) sseStream.close();
                sseStream = response.body();
                new Thread(() -> readSse(sseStream)).start();
            } else if (contentType.startsWith("application/json")) {
                try (JsonReader reader = Json.createReader(response.body())) {
                    queue.offer(reader.readObject());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private void readSse(InputStream stream) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            StringBuilder data = new StringBuilder();
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) {
                    if (data.length() > 0) {
                        try (JsonReader reader = Json.createReader(new StringReader(data.toString()))) {
                            queue.offer(reader.readObject());
                        }
                        data.setLength(0);
                    }
                    continue;
                }
                if (line.startsWith("data:")) {
                    data.append(line.substring(5).trim());
                }
            }
        } catch (IOException ignore) {
        }
    }

    @Override
    public JsonObject receive() throws IOException {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (sseStream != null) sseStream.close();
    }
}
