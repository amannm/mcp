package com.amannmalik.mcp.api;


import java.util.Map;
import java.util.Optional;

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

    private final String method;

    NotificationMethod(String method) {
        this.method = method;
    }

    public static Optional<NotificationMethod> from(String method) {
        return JsonRpcMethod.from(NotificationMethod.class, method);
    }

    public String method() {
        return method;
    }

    private static final Map<NotificationMethod, ClientCapability> CLIENT_CAPABILITIES = Map.of(
            ROOTS_LIST_CHANGED, ClientCapability.ROOTS
    );

    @Override
    public Optional<ClientCapability> clientCapability() {
        return Optional.ofNullable(CLIENT_CAPABILITIES.get(this));
    }
}

