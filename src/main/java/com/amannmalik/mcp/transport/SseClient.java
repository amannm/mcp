package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.util.PlatformLog;
import jakarta.json.JsonObject;
import jakarta.servlet.AsyncContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.System.Logger;
import java.security.SecureRandom;
import java.util.*;
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
        if (clientPrefixByteLength <= 0) {
            throw new IllegalArgumentException("clientPrefixByteLength must be positive");
        }
        if (historyLimit < 0) {
            throw new IllegalArgumentException("historyLimit must be non-negative");
        }
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
        transmitEvent(recordEvent(msg), "SSE send failed");
    }

    private void sendHistory(long lastId) {
        var replay = historyAfter(lastId);
        transmitEvents(replay, "SSE history send failed");
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        synchronized (transmissionLock) {
            completeContext(Logger.Level.ERROR, "SSE close failed");
        }
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
                if (event.id() > lastId) {
                    replay.add(event);
                }
            }
            return replay.isEmpty() ? List.of() : List.copyOf(replay);
        }
    }

    private void trimHistory() {
        while (history.size() > historyLimit) {
            history.removeFirst();
        }
    }

    private void transmitEvent(SseEvent event, String failureMessage) {
        Objects.requireNonNull(event, "event");
        transmit(failureMessage, writer -> writeEvent(writer, event));
    }

    private void transmitEvents(List<SseEvent> events, String failureMessage) {
        if (events.isEmpty()) {
            return;
        }
        transmit(failureMessage, writer -> {
            for (var event : events) {
                writeEvent(writer, event);
            }
        });
    }

    private void transmit(String failureMessage, TransmissionTask task) {
        synchronized (transmissionLock) {
            if (!canTransmit()) {
                return;
            }
            try {
                task.accept(out);
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
        writer.write("id: " + prefix + '-' + event.id() + "\n");
        writer.write("data: " + event.msg() + "\n\n");
    }

    private void handleTransmissionFailure(String message, Exception e) {
        LOG.log(Logger.Level.ERROR, message, e);
        closed.set(true);
        completeContext(Logger.Level.WARNING, "SSE context completion failed");
    }

    private void completeContext(Logger.Level failureLevel, String failureMessage) {
        var currentContext = context;
        try {
            if (currentContext != null && !currentContext.hasOriginalRequestAndResponse()) {
                currentContext.complete();
            }
        } catch (Exception completionFailure) {
            LOG.log(failureLevel, failureMessage, completionFailure);
        } finally {
            context = null;
            out = null;
        }
    }

    @FunctionalInterface
    private interface TransmissionTask {
        void accept(PrintWriter writer) throws Exception;
    }

    private record SseEvent(long id, JsonObject msg) {
    }
}
