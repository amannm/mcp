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
        handleOutcome(message, router.route(message), false);
    }

    public void flush() {
        JsonObject message;
        while ((message = backlog.peek()) != null) {
            if (!handleOutcome(message, router.route(message), true)) {
                return;
            }
        }
    }

    private boolean handleOutcome(JsonObject message, RouteOutcome outcome, boolean fromBacklog) {
        return switch (outcome) {
            case DELIVERED -> handleDelivered(fromBacklog);
            case PENDING -> handlePending(message, fromBacklog);
            case NOT_FOUND -> handleNotFound(message, fromBacklog);
        };
    }

    private boolean handleDelivered(boolean fromBacklog) {
        if (fromBacklog) {
            backlog.poll();
        } else {
            flush();
        }
        return true;
    }

    private boolean handlePending(JsonObject message, boolean fromBacklog) {
        if (!fromBacklog) {
            backlog.add(message);
        }
        return false;
    }

    private boolean handleNotFound(JsonObject message, boolean fromBacklog) {
        dropMessage(message, fromBacklog);
        return true;
    }

    private void dropMessage(JsonObject message, boolean fromBacklog) {
        if (fromBacklog) {
            backlog.poll();
            LOG.log(Logger.Level.WARNING, "Dropping unroutable message from backlog: {0}", message);
        } else {
            LOG.log(Logger.Level.WARNING, "Dropping unroutable message: {0}", message);
        }
    }
}
