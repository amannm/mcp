package com.amannmalik.mcp.api;

import java.util.EnumSet;
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
    ROOTS_LIST_CHANGED("notifications/roots/list_changed", EnumSet.of(ClientCapability.ROOTS));

    private final String method;
    private final EnumSet<ClientCapability> clientCapabilities;

    NotificationMethod(String method) {
        this(method, EnumSet.noneOf(ClientCapability.class));
    }

    NotificationMethod(String method, EnumSet<ClientCapability> clientCaps) {
        this.method = method;
        this.clientCapabilities = clientCaps.clone();
    }

    public static Optional<NotificationMethod> from(String method) {
        if (method == null) {
            return Optional.empty();
        }
        return switch (method) {
            case "notifications/initialized" -> Optional.of(INITIALIZED);
            case "notifications/cancelled" -> Optional.of(CANCELLED);
            case "notifications/progress" -> Optional.of(PROGRESS);
            case "notifications/resources/list_changed" -> Optional.of(RESOURCES_LIST_CHANGED);
            case "notifications/resources/updated" -> Optional.of(RESOURCES_UPDATED);
            case "notifications/tools/list_changed" -> Optional.of(TOOLS_LIST_CHANGED);
            case "notifications/prompts/list_changed" -> Optional.of(PROMPTS_LIST_CHANGED);
            case "notifications/message" -> Optional.of(MESSAGE);
            case "notifications/roots/list_changed" -> Optional.of(ROOTS_LIST_CHANGED);
            default -> Optional.empty();
        };
    }

    public String method() {
        return method;
    }

    @Override
    public EnumSet<ClientCapability> clientCapabilities() {
        return clientCapabilities.clone();
    }
}

