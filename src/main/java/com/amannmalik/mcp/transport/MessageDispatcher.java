package com.amannmalik.mcp.transport;

import jakarta.json.JsonObject;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

final class MessageDispatcher {
    private final MessageRouter router;
    private final Queue<JsonObject> backlog = new ConcurrentLinkedQueue<>();

    MessageDispatcher(MessageRouter router) {
        this.router = router;
    }

    void dispatch(JsonObject message) {
        if (router.route(message)) {
            flush();
        } else {
            backlog.add(message);
        }
    }

    void flush() {
        while (true) {
            JsonObject msg = backlog.peek();
            if (msg == null) return;
            if (router.route(msg)) {
                backlog.poll();
            } else {
                return;
            }
        }
    }
}
