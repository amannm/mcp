package com.amannmalik.mcp;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private static final Map<String, NotificationMethod> BY_METHOD;
    private final String method;

    NotificationMethod(String method) {
        this.method = method;
    }

    static {
        BY_METHOD = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(m -> m.method, m -> m));
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
        if (method == null) return Optional.empty();
        return Optional.ofNullable(BY_METHOD.get(method));
    }
}

