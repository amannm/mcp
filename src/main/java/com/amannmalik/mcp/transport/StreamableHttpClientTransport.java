package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.Transport;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/// - [Transports](specification/2025-06-18/basic/transports.mdx)
public final class StreamableHttpClientTransport implements Transport {
    private final HttpClient client = HttpClient.newHttpClient();
    private final URI endpoint;
    private final BlockingQueue<JsonObject> incoming = new LinkedBlockingQueue<>();
    private final Set<SseReader> streams = ConcurrentHashMap.newKeySet();
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private final AtomicReference<String> protocolVersion = new AtomicReference<>(Protocol.LATEST_VERSION);
    private final AtomicReference<String> authorization = new AtomicReference<>();

    public StreamableHttpClientTransport(URI endpoint) {
        this.endpoint = endpoint;
    }

    public void setProtocolVersion(String version) {
        protocolVersion.set(ValidationUtil.requireNonBlank(version));
    }

    public void setAuthorization(String token) {
        authorization.set(ValidationUtil.requireNonBlank(token));
    }

    public void clearAuthorization() {
        authorization.set(null);
    }

    public void listen() throws IOException {
        var request = builder()
                .header("Accept", "text/event-stream")
                .GET()
                .build();
        var response = exchange(request);
        AuthorizationUtil.checkUnauthorized(response);
        int status = response.statusCode();
        String ct = response.headers().firstValue("Content-Type").orElse("");
        if (status != 200 || !ct.startsWith("text/event-stream")) {
            response.body().close();
            throw new IOException("Unexpected response: " + status + " " + ct);
        }
        startReader(response.body());
    }

    @Override
    public void send(JsonObject message) throws IOException {
        var request = builder()
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(message.toString()))
                .build();
        var response = exchange(request);
        AuthorizationUtil.checkUnauthorized(response);
        int status = response.statusCode();
        String ct = response.headers().firstValue("Content-Type").orElse("");
        if (status == 202) {
            response.body().close();
            return;
        }
        if (ct.startsWith("application/json")) {
            try (JsonReader reader = Json.createReader(response.body())) {
                incoming.add(reader.readObject());
            }
            return;
        }
        if (ct.startsWith("text/event-stream")) {
            startReader(response.body());
            return;
        }
        response.body().close();
        throw new IOException("Unexpected response: " + status + " " + ct);
    }

    @Override
    public JsonObject receive() throws IOException {
        return receive(McpConfiguration.current().defaultMs());
    }

    @Override
    public JsonObject receive(long timeoutMillis) throws IOException {
        try {
            JsonObject result = incoming.poll(timeoutMillis, TimeUnit.MILLISECONDS);
            if (result == null) {
                throw new IOException("Timeout after " + timeoutMillis + "ms waiting for message");
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for message", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (sessionId.get() != null) {
            var request = builder().DELETE().build();
            try {
                client.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
        streams.forEach(SseReader::close);
        streams.clear();
    }

    private void startReader(InputStream body) {
        SseReader reader = new SseReader(body, incoming, streams);
        streams.add(reader);
        Thread t = new Thread(reader);
        t.setDaemon(true);
        t.start();
    }


    private HttpRequest.Builder builder() {
        var b = HttpRequest.newBuilder(endpoint)
                .header("Origin", "http://127.0.0.1")
                .header(TransportHeaders.PROTOCOL_VERSION, protocolVersion.get());
        Optional.ofNullable(authorization.get())
                .ifPresent(t -> b.header(TransportHeaders.AUTHORIZATION, "Bearer " + t));
        Optional.ofNullable(sessionId.get())
                .ifPresent(id -> b.header(TransportHeaders.SESSION_ID, id));
        return b;
    }

    private HttpResponse<InputStream> exchange(HttpRequest request) throws IOException {
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            sessionId.updateAndGet(old -> response.headers()
                    .firstValue(TransportHeaders.SESSION_ID)
                    .orElse(old));
            protocolVersion.updateAndGet(old -> response.headers()
                    .firstValue(TransportHeaders.PROTOCOL_VERSION)
                    .orElse(old));
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    static class SseReader implements Runnable {
        private final InputStream input;
        private final BlockingQueue<JsonObject> queue;
        private final Set<SseReader> container;
        private volatile boolean closed;
        private String lastEventId;

        SseReader(InputStream input, BlockingQueue<JsonObject> queue, Set<SseReader> container) {
            this.input = input;
            this.queue = queue;
            this.container = container;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                var data = new StringBuilder();
                String eventId = null;
                while (!closed && (line = br.readLine()) != null) {
                    if (line.isEmpty()) {
                        if (!data.isEmpty()) {
                            dispatch(data.toString(), eventId);
                            data.setLength(0);
                            eventId = null;
                        }
                        continue;
                    }
                    int idx = line.indexOf(':');
                    if (idx < 0) continue;
                    var field = line.substring(0, idx);
                    var value = line.substring(idx + 1).trim();
                    switch (field) {
                        case "id" -> eventId = value;
                        case "data" -> {
                            if (!data.isEmpty()) data.append('\n');
                            data.append(value);
                        }
                    }
                }
            } catch (IOException ignore) {
            } finally {
                if (container != null) container.remove(this);
                close();
            }
        }

        private void dispatch(String payload, String eventId) {
            try (JsonReader jr = Json.createReader(new StringReader(payload))) {
                queue.add(jr.readObject());
            } catch (Exception ignore) {
            }
            if (eventId != null) lastEventId = eventId;
        }

        void close() {
            closed = true;
            try {
                input.close();
            } catch (IOException ignore) {
            }
        }

        String lastEventId() {
            return lastEventId;
        }
    }
}
