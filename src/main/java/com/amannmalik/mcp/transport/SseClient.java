package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.util.Base64Util;
import jakarta.json.JsonObject;
import jakarta.servlet.AsyncContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.System.Logger;

public final class SseClient implements AutoCloseable {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger LOG = System.getLogger(SseClient.class.getName());
    final String prefix;
    private final long historyLimit;
    private final Deque<SseEvent> history = new ArrayDeque<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private AsyncContext context;
    private PrintWriter out;
    private final AtomicBoolean closed = new AtomicBoolean();

    public SseClient(AsyncContext context, int clientPrefixByteLength, long historyLimit) throws IOException {
        var bytes = new byte[clientPrefixByteLength];
        RANDOM.nextBytes(bytes);
        this.prefix = Base64Util.encodeUrl(bytes);
        attach(context, 0);
        this.historyLimit = historyLimit;
    }

    void attach(AsyncContext ctx, long lastId) throws IOException {
        this.context = ctx;
        this.out = ctx.getResponse().getWriter();
        this.closed.set(false);
        sendHistory(lastId);
    }

    public boolean isActive() {
        return !closed.get() && context != null;
    }

    public void send(JsonObject msg) {
        var id = nextId.getAndIncrement();
        history.addLast(new SseEvent(id, msg));
        while (history.size() > historyLimit) {
            history.removeFirst();
        }
        if (closed.get() || context == null) {
            return;
        }
        try {
            out.write("id: " + prefix + '-' + id + "\n");
            out.write("data: " + msg + "\n\n");
            out.flush();
        } catch (Exception e) {
            LOG.log(Logger.Level.ERROR, "SSE send failed: " + e.getMessage());
            closed.set(true);
        }
    }

    private void sendHistory(long lastId) {
        if (context == null) {
            return;
        }
        for (var ev : history) {
            if (ev.id > lastId) {
                out.write("id: " + prefix + '-' + ev.id + "\n");
                out.write("data: " + ev.msg + "\n\n");
            }
        }
        out.flush();
    }

    @Override
    public void close() {
        if (closed.get()) {
            return;
        }
        closed.set(true);
        try {
            if (context != null && !context.hasOriginalRequestAndResponse()) {
                context.complete();
            }
        } catch (Exception e) {
            LOG.log(Logger.Level.ERROR, "SSE close failed: " + e.getMessage());
        } finally {
            context = null;
            out = null;
        }
    }

    private record SseEvent(long id, JsonObject msg) {
    }
}

