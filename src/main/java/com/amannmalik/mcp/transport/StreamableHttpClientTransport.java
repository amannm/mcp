package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.lifecycle.Protocol;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public final class StreamableHttpClientTransport implements Transport {
    private final HttpClient client = HttpClient.newHttpClient();
    private final URI endpoint;
    private final BlockingQueue<JsonObject> incoming = new LinkedBlockingQueue<>();
    private final Set<SseReader> streams = ConcurrentHashMap.newKeySet();
    private volatile String sessionId;
    private volatile String protocolVersion = Protocol.LATEST_VERSION;

    public StreamableHttpClientTransport(URI endpoint) {
        this.endpoint = endpoint;
    }

    public void setProtocolVersion(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version required");
        }
        this.protocolVersion = version;
    }

    @Override
    public void send(JsonObject message) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .header(TransportHeaders.PROTOCOL_VERSION, protocolVersion);
        Optional.ofNullable(sessionId).ifPresent(id -> builder.header(TransportHeaders.SESSION_ID, id));
        HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(message.toString())).build();
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }

        sessionId = response.headers().firstValue(TransportHeaders.SESSION_ID).orElse(sessionId);

        protocolVersion = response.headers()
                .firstValue(TransportHeaders.PROTOCOL_VERSION)
                .orElse(protocolVersion);

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
            SseReader reader = new SseReader(response.body());
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
        if (sessionId != null) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                    .header(TransportHeaders.SESSION_ID, sessionId)
                    .header(TransportHeaders.PROTOCOL_VERSION, protocolVersion)
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

    private final class SseReader implements Runnable {
        private final InputStream input;
        private volatile boolean closed;

        SseReader(InputStream input) {
            this.input = input;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                StringBuilder data = new StringBuilder();
                while (!closed && (line = br.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        data.append(line.substring(6));
                    } else if (line.isEmpty()) {
                        if (!data.isEmpty()) {
                            try (JsonReader jr = Json.createReader(new StringReader(data.toString()))) {
                                incoming.add(jr.readObject());
                            } catch (Exception ignore) {
                            }
                            data.setLength(0);
                        }
                    }
                }
            } catch (IOException ignore) {
            } finally {
                streams.remove(this);
                close();
            }
        }

        void close() {
            closed = true;
            try {
                input.close();
            } catch (IOException ignore) {
            }
        }
    }
}
