package com.amannmalik.mcp.api;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum NotificationMethod implements JsonRpcMethod {
    INITIALIZED("notifications/initialized"),
    CANCELLED("notifications/cancelled"),
    PROGRESS("notifications/progress"),
    RESOURCES_LIST_CHANGED("notifications/resources/list_changed"),
    RESOURCES_UPDATED("notifications/resources/updated"),
    TOOLS_LIST_CHANGED("notifications/tools/list_changed"),
    PROMPTS_LIST_CHANGED("notifications/prompts/list_changed"),
    MESSAGE("notifications/message"),
    ROOTS_LIST_CHANGED("notifications/roots/list_changed");

    private static final Map<String, NotificationMethod> BY_METHOD = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(NotificationMethod::method, m -> m));

    private final String method;

    NotificationMethod(String method) {
        this.method = method;
    }

    public static Optional<NotificationMethod> from(String method) {
        if (method == null) return Optional.empty();
        return Optional.ofNullable(BY_METHOD.get(method));
    }

    public String method() {
        return method;
    }

    @Override
    public Optional<ClientCapability> clientCapability() {
        return this == ROOTS_LIST_CHANGED ? Optional.of(ClientCapability.ROOTS) : Optional.empty();
    }
}

