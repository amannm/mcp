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

final class SseReader implements Runnable {
    private final InputStream input;
    private final BlockingQueue<JsonObject> queue;
    private final Set<SseReader> container;
    private final EventBuffer buffer = new EventBuffer();
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
            while (!closed && (line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    buffer.flush();
                    continue;
                }
                int idx = line.indexOf(':');
                if (idx < 0) continue;
                buffer.field(line.substring(0, idx), line.substring(idx + 1).trim());
            }
            buffer.flush();
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

    private final class EventBuffer {
        private final StringBuilder data = new StringBuilder();
        private String eventId;

        void field(String name, String value) {
            switch (name) {
                case "id" -> eventId = value;
                case "data" -> {
                    if (!data.isEmpty()) data.append('\n');
                    data.append(value);
                }
            }
        }

        void flush() {
            if (data.isEmpty()) return;
            dispatch(data.toString(), eventId);
            data.setLength(0);
            eventId = null;
        }
    }
}
