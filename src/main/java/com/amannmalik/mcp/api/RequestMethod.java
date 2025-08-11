package com.amannmalik.mcp.api;


import java.util.Optional;

public enum RequestMethod implements JsonRpcMethod {
    INITIALIZE("initialize"),
    PING("ping"),
    RESOURCES_LIST("resources/list"),
    RESOURCES_TEMPLATES_LIST("resources/templates/list"),
    RESOURCES_READ("resources/read"),
    RESOURCES_SUBSCRIBE("resources/subscribe"),
    RESOURCES_UNSUBSCRIBE("resources/unsubscribe"),
    TOOLS_LIST("tools/list"),
    TOOLS_CALL("tools/call"),
    PROMPTS_LIST("prompts/list"),
    PROMPTS_GET("prompts/get"),
    LOGGING_SET_LEVEL("logging/setLevel"),
    COMPLETION_COMPLETE("completion/complete"),
    SAMPLING_CREATE_MESSAGE("sampling/createMessage"),
    ROOTS_LIST("roots/list"),
    ELICITATION_CREATE("elicitation/create");

    private final String method;

    RequestMethod(String method) {
        this.method = method;
    }

    public static Optional<RequestMethod> from(String method) {
        return JsonRpcMethod.from(RequestMethod.class, method);
    }

    public String method() {
        return method;
    }

    public Optional<ServerCapability> serverCapability() {
        return switch (this) {
            case RESOURCES_LIST, RESOURCES_TEMPLATES_LIST, RESOURCES_READ, RESOURCES_SUBSCRIBE, RESOURCES_UNSUBSCRIBE ->
                    Optional.of(ServerCapability.RESOURCES);
            case TOOLS_LIST, TOOLS_CALL -> Optional.of(ServerCapability.TOOLS);
            case PROMPTS_LIST, PROMPTS_GET -> Optional.of(ServerCapability.PROMPTS);
            case LOGGING_SET_LEVEL -> Optional.of(ServerCapability.LOGGING);
            case COMPLETION_COMPLETE -> Optional.of(ServerCapability.COMPLETIONS);
            default -> Optional.empty();
        };
    }

    @Override
    public Optional<ClientCapability> clientCapability() {
        return switch (this) {
            case ROOTS_LIST -> Optional.of(ClientCapability.ROOTS);
            case SAMPLING_CREATE_MESSAGE -> Optional.of(ClientCapability.SAMPLING);
            case ELICITATION_CREATE -> Optional.of(ClientCapability.ELICITATION);
            default -> Optional.empty();
        };
    }
}
