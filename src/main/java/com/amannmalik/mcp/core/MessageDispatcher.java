package com.amannmalik.mcp.core;

import com.amannmalik.mcp.util.PlatformLog;
import jakarta.json.JsonObject;

import java.lang.System.Logger;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MessageDispatcher {
    private static final Logger LOG = PlatformLog.get(MessageDispatcher.class);
    private final MessageRouter router;
    private final Queue<JsonObject> backlog = new ConcurrentLinkedQueue<>();

    public MessageDispatcher(MessageRouter router) {
        this.router = Objects.requireNonNull(router, "router");
    }

    public void dispatch(JsonObject message) {
        Objects.requireNonNull(message, "message");
        switch (router.route(message)) {
            case DELIVERED -> flush();
            case PENDING -> backlog.add(message);
            case NOT_FOUND -> logDrop("Dropping unroutable message: {0}", message);
        }
    }

    public void flush() {
        JsonObject message;
        while ((message = backlog.peek()) != null) {
            switch (router.route(message)) {
                case DELIVERED -> backlog.poll();
                case PENDING -> {
                    return;
                }
                case NOT_FOUND -> {
                    backlog.poll();
                    logDrop("Dropping unroutable message from backlog: {0}", message);
                }
            }
        }
    }

    private void logDrop(String template, JsonObject message) {
        LOG.log(Logger.Level.WARNING, template, message);
    }
}
