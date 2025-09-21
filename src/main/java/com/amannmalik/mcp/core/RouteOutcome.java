package com.amannmalik.mcp.core;

/**
 * Result of attempting to deliver a JSON-RPC message to a connected client.
 */
public enum RouteOutcome {
    /**
     * Message has been delivered (or persisted for eventual replay).
     */
    DELIVERED,

    /**
     * Message could not be delivered yet but may succeed later.
     */
    PENDING,

    /**
     * No route exists for the message and it should be discarded.
     */
    NOT_FOUND;

    public boolean isDelivered() {
        return this == DELIVERED;
    }

    public boolean shouldRetry() {
        return this == PENDING;
    }
}

