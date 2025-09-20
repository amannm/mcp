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
        handleDispatchOutcome(message, router.route(message));
    }

    public void flush() {
        drainBacklog();
    }

    private void handleDispatchOutcome(JsonObject message, RouteOutcome outcome) {
        switch (outcome) {
            case DELIVERED -> drainBacklog();
            case PENDING -> backlog.add(message);
            case NOT_FOUND -> logDrop(message, false);
        }
    }

    private void drainBacklog() {
        JsonObject next;
        while ((next = backlog.peek()) != null) {
            var outcome = router.route(next);
            if (outcome == RouteOutcome.PENDING) {
                return;
            }
            backlog.poll();
            if (outcome == RouteOutcome.NOT_FOUND) {
                logDrop(next, true);
            }
            // Successful delivery, continue draining remaining backlog entries.
        }
    }

    private void logDrop(JsonObject message, boolean fromBacklog) {
        if (fromBacklog) {
            LOG.log(Logger.Level.WARNING, "Dropping unroutable message from backlog: {0}", message);
        } else {
            LOG.log(Logger.Level.WARNING, "Dropping unroutable message: {0}", message);
        }
    }
}
