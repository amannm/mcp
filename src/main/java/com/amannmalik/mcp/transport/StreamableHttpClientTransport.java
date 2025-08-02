package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.lifecycle.Protocol;
import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.*;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

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
        protocolVersion.set(InputSanitizer.requireNonBlank(version));
    }

    public void setAuthorization(String token) {
        authorization.set(InputSanitizer.requireNonBlank(token));
    }

    public void clearAuthorization() {
        authorization.set(null);
    }

    public void listen() throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .header("Accept", "text/event-stream")
                .header("Origin", "http://127.0.0.1")
                .header(TransportHeaders.PROTOCOL_VERSION, protocolVersion.get());
        Optional.ofNullable(authorization.get())
                .ifPresent(t -> builder.header(TransportHeaders.AUTHORIZATION, "Bearer " + t));
        Optional.ofNullable(sessionId.get()).ifPresent(id -> builder.header(TransportHeaders.SESSION_ID, id));
        HttpRequest request = builder.GET().build();
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }

        sessionId.updateAndGet(old -> response.headers()
                .firstValue(TransportHeaders.SESSION_ID)
                .orElse(old));

        protocolVersion.updateAndGet(old -> response.headers()
                .firstValue(TransportHeaders.PROTOCOL_VERSION)
                .orElse(old));

        int status = response.statusCode();
        String ct = response.headers().firstValue("Content-Type").orElse("");
        if (status == 401) {
            String header = response.headers().firstValue("WWW-Authenticate").orElse("");
            response.body().close();
            throw new UnauthorizedException(header);
        }
        if (status != 200 || !ct.startsWith("text/event-stream")) {
            response.body().close();
            throw new IOException("Unexpected response: " + status + " " + ct);
        }
        SseReader reader = new SseReader(response.body(), incoming, streams);
        streams.add(reader);
        Thread t = new Thread(reader);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void send(JsonObject message) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .header("Origin", "http://127.0.0.1")
                .header(TransportHeaders.PROTOCOL_VERSION, protocolVersion.get());
        Optional.ofNullable(authorization.get())
                .ifPresent(t -> builder.header(TransportHeaders.AUTHORIZATION, "Bearer " + t));
        Optional.ofNullable(sessionId.get()).ifPresent(id -> builder.header(TransportHeaders.SESSION_ID, id));
        HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(message.toString())).build();
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }

        sessionId.updateAndGet(old -> response.headers()
                .firstValue(TransportHeaders.SESSION_ID)
                .orElse(old));

        protocolVersion.updateAndGet(old -> response.headers()
                .firstValue(TransportHeaders.PROTOCOL_VERSION)
                .orElse(old));

        int status = response.statusCode();
        String ct = response.headers().firstValue("Content-Type").orElse("");
        if (status == 401) {
            String header = response.headers().firstValue("WWW-Authenticate").orElse("");
            response.body().close();
            throw new UnauthorizedException(header);
        }
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
            SseReader reader = new SseReader(response.body(), incoming, streams);
            streams.add(reader);
            Thread t = new Thread(reader);
            t.setDaemon(true);
            t.start();
            return;
        }
        response.body().close();
        throw new IOException("Unexpected response: " + status + " " + ct);
    }

    @Override
    public JsonObject receive() throws IOException {
        try {
            return incoming.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        String sid = sessionId.get();
        if (sid != null) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                    .header(TransportHeaders.SESSION_ID, sid)
                    .header(TransportHeaders.PROTOCOL_VERSION, protocolVersion.get())
                    .DELETE();
            try {
                client.send(builder.build(), HttpResponse.BodyHandlers.discarding());
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
        streams.forEach(SseReader::close);
        streams.clear();
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
                StringBuilder data = new StringBuilder();
                String eventId = null;
                while (!closed && (line = br.readLine()) != null) {
                    if (line.startsWith("id:")) {
                        eventId = value(line);
                    } else if (line.startsWith("data:")) {
                        if (!data.isEmpty()) data.append('\n');
                        data.append(value(line));
                    } else if (line.isEmpty()) {
                        if (!data.isEmpty()) {
                            try (JsonReader jr = Json.createReader(new StringReader(data.toString()))) {
                                queue.add(jr.readObject());
                            } catch (Exception ignore) {
                            }
                            data.setLength(0);
                            if (eventId != null) {
                                lastEventId = eventId;
                                eventId = null;
                            }
                        }
                    }
                }
            } catch (IOException ignore) {
            } finally {
                if (container != null) container.remove(this);
                close();
            }
        }

        private String value(String line) {
            return line.substring(line.indexOf(':') + 1).trim();
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
