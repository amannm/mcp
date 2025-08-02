package com.amannmalik.mcp.transport;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

/**
 * Reads an SSE stream and enqueues each event as a {@link JsonObject}.
 * The last processed event ID is exposed for reconnection logic.
 */
final class SseReader implements Runnable {
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
                    eventId = line.substring(line.indexOf(':') + 1).trim();
                } else if (line.startsWith("data:")) {
                    if (!data.isEmpty()) data.append('\n');
                    data.append(line.substring(line.indexOf(':') + 1).trim());
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
