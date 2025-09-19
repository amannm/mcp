package com.amannmalik.mcp.core;

import jakarta.json.JsonObject;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MessageDispatcher {
    private final MessageRouter router;
    private final Queue<JsonObject> backlog = new ConcurrentLinkedQueue<>();

    public MessageDispatcher(MessageRouter router) {
        this.router = Objects.requireNonNull(router, "router");
    }

    public void dispatch(JsonObject message) {
        Objects.requireNonNull(message, "message");
        if (router.route(message)) {
            flush();
        } else {
            backlog.add(message);
        }
    }

    public void flush() {
        JsonObject message;
        while ((message = backlog.peek()) != null) {
            if (!router.route(message)) {
                return;
            }
            backlog.poll();
        }
    }
}
