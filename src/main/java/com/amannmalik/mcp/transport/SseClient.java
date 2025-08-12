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

public final class SseClient implements AutoCloseable {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final long historyLimit;
    final String prefix;
    private final Deque<SseEvent> history = new ArrayDeque<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private AsyncContext context;
    private PrintWriter out;
    private volatile boolean closed;

    public SseClient(AsyncContext context, int clientPrefixByteLength) throws IOException {
        byte[] bytes = new byte[clientPrefixByteLength];
        RANDOM.nextBytes(bytes);
        this.prefix = Base64Util.encodeUrl(bytes);
        attach(context, 0);
        // TODO: config
        historyLimit = 1L;
    }

    void attach(AsyncContext ctx, long lastId) throws IOException {
        this.context = ctx;
        this.out = ctx.getResponse().getWriter();
        this.closed = false;
        sendHistory(lastId);
    }

    public boolean isActive() {
        return !closed && context != null;
    }

    public void send(JsonObject msg) {
        long id = nextId.getAndIncrement();
        history.addLast(new SseEvent(id, msg));
        while (history.size() > historyLimit) {
            history.removeFirst();
        }
        if (closed || context == null) {
            return;
        }
        try {
            out.write("id: " + prefix + '-' + id + "\n");
            out.write("data: " + msg + "\n\n");
            out.flush();
        } catch (Exception e) {
            System.err.println("SSE send failed: " + e.getMessage());
            closed = true;
        }
    }

    private void sendHistory(long lastId) {
        if (context == null) {
            return;
        }
        for (SseEvent ev : history) {
            if (ev.id > lastId) {
                out.write("id: " + prefix + '-' + ev.id + "\n");
                out.write("data: " + ev.msg + "\n\n");
            }
        }
        out.flush();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (context != null && !context.hasOriginalRequestAndResponse()) {
                context.complete();
            }
        } catch (Exception e) {
            System.err.println("SSE close failed: " + e.getMessage());
        } finally {
            context = null;
            out = null;
        }
    }

    private record SseEvent(long id, JsonObject msg) {
    }
}

