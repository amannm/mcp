package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.util.PlatformLog;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.*;
import java.lang.System.Logger;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

final class SseReader implements Runnable {
    private static final Logger LOG = PlatformLog.get(SseReader.class);

    private final InputStream input;
    private final BlockingQueue<JsonObject> queue;
    private final Set<SseReader> container;
    private final EventBuffer buffer = new EventBuffer();
    private final AtomicBoolean closed = new AtomicBoolean();
    private String lastEventId;

    SseReader(InputStream input, BlockingQueue<JsonObject> queue, Set<SseReader> container) {
        this.input = Objects.requireNonNull(input, "input");
        this.queue = Objects.requireNonNull(queue, "queue");
        this.container = container == null ? Set.of() : container;
    }

    @Override
    public void run() {
        try (var br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while (!closed.get() && (line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    buffer.flush();
                    continue;
                }
                var idx = line.indexOf(':');
                if (idx < 0) {
                    continue;
                }
                buffer.field(line.substring(0, idx), line.substring(idx + 1).trim());
            }
            buffer.flush();
        } catch (IOException e) {
            LOG.log(Logger.Level.WARNING, "SSE read failed", e);
        } finally {
            if (!container.isEmpty()) {
                container.remove(this);
            }
            close();
        }
    }

    private void dispatch(String payload, String eventId) {
        JsonObject message;
        try (var jr = Json.createReader(new StringReader(payload))) {
            message = jr.readObject();
        } catch (Exception e) {
            LOG.log(Logger.Level.WARNING, "Invalid SSE payload", e);
            return;
        }

        if (!queue.offer(message)) {
            LOG.log(Logger.Level.WARNING, "Dropping SSE message because queue is full");
            return;
        }

        if (eventId != null) {
            lastEventId = eventId;
        }
    }

    void close() {
        closed.set(true);
        try {
            input.close();
        } catch (IOException e) {
            LOG.log(Logger.Level.WARNING, "SSE close failed", e);
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
                    if (!data.isEmpty()) {
                        data.append('\n');
                    }
                    data.append(value);
                }
                default -> {
                    // do nothing
                }
            }
        }

        void flush() {
            if (data.isEmpty()) {
                return;
            }
            dispatch(data.toString(), eventId);
            data.setLength(0);
            eventId = null;
        }
    }
}
