package com.amannmalik.mcp.core;

import jakarta.json.JsonObject;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MessageDispatcher {
    private final MessageRouter router;
    private final Queue<JsonObject> backlog = new ConcurrentLinkedQueue<>();

    public MessageDispatcher(MessageRouter router) {
        this.router = router;
    }

    public void dispatch(JsonObject message) {
        if (router.route(message)) {
            flush();
        } else {
            backlog.add(message);
        }
    }

    public void flush() {
        while (true) {
            var msg = backlog.peek();
            if (msg == null) {
                return;
            }
            if (router.route(msg)) {
                backlog.poll();
            } else {
                return;
            }
        }
    }
}
