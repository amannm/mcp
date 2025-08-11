package com.amannmalik.mcp.api;


import java.util.*;
import java.util.stream.Collectors;


public enum RequestMethod implements JsonRpcMethod {
    INITIALIZE("initialize"),
    PING("ping"),
    RESOURCES_LIST("resources/list", EnumSet.of(ServerCapability.RESOURCES)),
    RESOURCES_TEMPLATES_LIST("resources/templates/list", EnumSet.of(ServerCapability.RESOURCES)),
    RESOURCES_READ("resources/read", EnumSet.of(ServerCapability.RESOURCES)),
    RESOURCES_SUBSCRIBE("resources/subscribe", EnumSet.of(ServerCapability.RESOURCES)),
    RESOURCES_UNSUBSCRIBE("resources/unsubscribe", EnumSet.of(ServerCapability.RESOURCES)),
    TOOLS_LIST("tools/list", EnumSet.of(ServerCapability.TOOLS)),
    TOOLS_CALL("tools/call", EnumSet.of(ServerCapability.TOOLS)),
    PROMPTS_LIST("prompts/list", EnumSet.of(ServerCapability.PROMPTS)),
    PROMPTS_GET("prompts/get", EnumSet.of(ServerCapability.PROMPTS)),
    LOGGING_SET_LEVEL("logging/setLevel", EnumSet.of(ServerCapability.LOGGING)),
    COMPLETION_COMPLETE("completion/complete", EnumSet.of(ServerCapability.COMPLETIONS)),
    SAMPLING_CREATE_MESSAGE("sampling/createMessage", EnumSet.noneOf(ServerCapability.class), EnumSet.of(ClientCapability.SAMPLING)),
    ROOTS_LIST("roots/list", EnumSet.noneOf(ServerCapability.class), EnumSet.of(ClientCapability.ROOTS)),
    ELICITATION_CREATE("elicitation/create", EnumSet.noneOf(ServerCapability.class), EnumSet.of(ClientCapability.ELICITATION));

    private final String method;
    private final EnumSet<ServerCapability> serverCapabilities;
    private final EnumSet<ClientCapability> clientCapabilities;

    RequestMethod(String method) {
        this(method, EnumSet.noneOf(ServerCapability.class), EnumSet.noneOf(ClientCapability.class));
    }

    RequestMethod(String method, EnumSet<ServerCapability> serverCaps) {
        this(method, serverCaps, EnumSet.noneOf(ClientCapability.class));
    }

    RequestMethod(String method, EnumSet<ServerCapability> serverCaps, EnumSet<ClientCapability> clientCaps) {
        this.method = method;
        this.serverCapabilities = serverCaps.clone();
        this.clientCapabilities = clientCaps.clone();
    }

    public static Optional<RequestMethod> from(String method) {
        if (method == null) return Optional.empty();
        return switch (method) {
            case "initialize" -> Optional.of(INITIALIZE);
            case "ping" -> Optional.of(PING);
            case "resources/list" -> Optional.of(RESOURCES_LIST);
            case "resources/templates/list" -> Optional.of(RESOURCES_TEMPLATES_LIST);
            case "resources/read" -> Optional.of(RESOURCES_READ);
            case "resources/subscribe" -> Optional.of(RESOURCES_SUBSCRIBE);
            case "resources/unsubscribe" -> Optional.of(RESOURCES_UNSUBSCRIBE);
            case "tools/list" -> Optional.of(TOOLS_LIST);
            case "tools/call" -> Optional.of(TOOLS_CALL);
            case "prompts/list" -> Optional.of(PROMPTS_LIST);
            case "prompts/get" -> Optional.of(PROMPTS_GET);
            case "logging/setLevel" -> Optional.of(LOGGING_SET_LEVEL);
            case "completion/complete" -> Optional.of(COMPLETION_COMPLETE);
            case "sampling/createMessage" -> Optional.of(SAMPLING_CREATE_MESSAGE);
            case "roots/list" -> Optional.of(ROOTS_LIST);
            case "elicitation/create" -> Optional.of(ELICITATION_CREATE);
            default -> Optional.empty();
        };
    }

    public String method() {
        return method;
    }

    public EnumSet<ServerCapability> serverCapabilities() {
        return serverCapabilities.clone();
    }

    @Override
    public EnumSet<ClientCapability> clientCapabilities() {
        return clientCapabilities.clone();
    }
}
