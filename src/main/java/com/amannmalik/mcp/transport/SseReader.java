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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

final class SseReader implements Runnable, AutoCloseable {
    private final InputStream input;
    private final BlockingQueue<JsonObject> queue;
    private final Set<SseReader> container;
    private volatile boolean closed;
    private Optional<String> lastEventId = Optional.empty();

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
            Optional<String> eventId = Optional.empty();
            while (!closed && (line = br.readLine()) != null) {
                if (line.startsWith("id:")) {
                    eventId = Optional.of(value(line));
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
                        eventId.ifPresent(id -> lastEventId = Optional.of(id));
                        eventId = Optional.empty();
                    }
                }
            }
        } catch (IOException ignore) {
        } finally {
            container.remove(this);
            close();
        }
    }

    private String value(String line) {
        return line.substring(line.indexOf(':') + 1).trim();
    }

    @Override
    public void close() {
        closed = true;
        try {
            input.close();
        } catch (IOException ignore) {
        }
    }

    Optional<String> lastEventId() {
        return lastEventId;
    }
}
