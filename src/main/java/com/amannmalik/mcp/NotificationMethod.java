package com.amannmalik.mcp;

import java.util.Optional;

/**
 * JSON-RPC notification method names used by the protocol.
 */
public enum NotificationMethod {
    INITIALIZED("notifications/initialized"),
    CANCELLED("notifications/cancelled"),
    PROGRESS("notifications/progress"),
    RESOURCES_LIST_CHANGED("notifications/resources/list_changed"),
    RESOURCES_UPDATED("notifications/resources/updated"),
    TOOLS_LIST_CHANGED("notifications/tools/list_changed"),
    PROMPTS_LIST_CHANGED("notifications/prompts/list_changed"),
    MESSAGE("notifications/message"),
    ROOTS_LIST_CHANGED("notifications/roots/list_changed");

    private final String method;

    NotificationMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the JSON-RPC method string for this notification.
     */
    public String method() {
        return method;
    }

    /**
     * Parses a method string into a notification method.
     */
    public static Optional<NotificationMethod> from(String method) {
        for (NotificationMethod m : values()) {
            if (m.method.equals(method)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }
}

