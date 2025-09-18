package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.util.PlatformLog;
import jakarta.json.JsonObject;
import jakarta.servlet.AsyncContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.System.Logger;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class SseClient implements AutoCloseable {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger LOG = PlatformLog.get(SseClient.class);
    private final String prefix;
    private final long historyLimit;
    private final Deque<SseEvent> history = new ArrayDeque<>();
    private final Object historyLock = new Object();
    private final Object transmissionLock = new Object();
    private final AtomicLong nextId = new AtomicLong(1);
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile AsyncContext context;
    private volatile PrintWriter out;

    public SseClient(AsyncContext context, int clientPrefixByteLength, long historyLimit) throws IOException {
        Objects.requireNonNull(context, "context");
        var bytes = new byte[clientPrefixByteLength];
        RANDOM.nextBytes(bytes);
        this.prefix = Base64Util.encodeUrl(bytes);
        attach(context, 0);
        this.historyLimit = historyLimit;
    }

    public String prefix() {
        return prefix;
    }

    void attach(AsyncContext ctx, long lastId) throws IOException {
        Objects.requireNonNull(ctx, "ctx");
        var writer = ctx.getResponse().getWriter();
        synchronized (transmissionLock) {
            this.context = ctx;
            this.out = writer;
            this.closed.set(false);
        }
        sendHistory(lastId);
    }

    public boolean isActive() {
        return !closed.get() && context != null;
    }

    public void send(JsonObject msg) {
        Objects.requireNonNull(msg, "msg");
        var event = recordEvent(msg);
        transmit(List.of(event), "SSE send failed");
    }

    private void sendHistory(long lastId) {
        var replay = historyAfter(lastId);
        transmit(replay, "SSE history send failed");
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        synchronized (transmissionLock) {
            try {
                if (context != null && !context.hasOriginalRequestAndResponse()) {
                    context.complete();
                }
            } catch (Exception e) {
                LOG.log(Logger.Level.ERROR, "SSE close failed", e);
            } finally {
                context = null;
                out = null;
            }
        }
    }

    private record SseEvent(long id, JsonObject msg) {
    }

    private SseEvent recordEvent(JsonObject msg) {
        var event = new SseEvent(nextId.getAndIncrement(), msg);
        synchronized (historyLock) {
            history.addLast(event);
            trimHistory();
        }
        return event;
    }

    private List<SseEvent> historyAfter(long lastId) {
        synchronized (historyLock) {
            if (history.isEmpty()) {
                return List.of();
            }
            var replay = new ArrayList<SseEvent>(history.size());
            for (var event : history) {
                if (event.id > lastId) {
                    replay.add(event);
                }
            }
            return replay.isEmpty() ? List.of() : replay;
        }
    }

    private void trimHistory() {
        while (history.size() > historyLimit) {
            history.removeFirst();
        }
    }

    private void transmit(List<SseEvent> events, String failureMessage) {
        if (events.isEmpty()) {
            return;
        }
        synchronized (transmissionLock) {
            if (!canTransmit()) {
                return;
            }
            try {
                for (var event : events) {
                    writeEvent(out, event);
                }
                out.flush();
            } catch (Exception e) {
                handleTransmissionFailure(failureMessage, e);
            }
        }
    }

    private boolean canTransmit() {
        return !closed.get() && context != null && out != null;
    }

    private void writeEvent(PrintWriter writer, SseEvent event) {
        writer.write("id: " + prefix + '-' + event.id + "\n");
        writer.write("data: " + event.msg + "\n\n");
    }

    private void handleTransmissionFailure(String message, Exception e) {
        LOG.log(Logger.Level.ERROR, message, e);
        closed.set(true);
    }
}
