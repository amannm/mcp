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

    private void logDrop(String template, JsonObject message) {
        LOG.log(Logger.Level.WARNING, template, message);
    }

    private boolean handleOutcome(JsonObject message, RouteOutcome outcome, boolean fromBacklog) {
        return switch (outcome) {
            case DELIVERED -> {
                if (fromBacklog) {
                    backlog.poll();
                } else {
                    flush();
                }
                yield true;
            }
            case PENDING -> {
                if (!fromBacklog) {
                    backlog.add(message);
                }
                yield false;
            }
            case NOT_FOUND -> {
                if (fromBacklog) {
                    backlog.poll();
                    logDrop("Dropping unroutable message from backlog: {0}", message);
                } else {
                    logDrop("Dropping unroutable message: {0}", message);
                }
                yield true;
            }
        };
    }
}
