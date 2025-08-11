package com.amannmalik.mcp.api;


import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

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

    private static final Map<RequestMethod, ServerCapability> SERVER_CAPABILITIES = Map.ofEntries(
            entry(RESOURCES_LIST, ServerCapability.RESOURCES),
            entry(RESOURCES_TEMPLATES_LIST, ServerCapability.RESOURCES),
            entry(RESOURCES_READ, ServerCapability.RESOURCES),
            entry(RESOURCES_SUBSCRIBE, ServerCapability.RESOURCES),
            entry(RESOURCES_UNSUBSCRIBE, ServerCapability.RESOURCES),
            entry(TOOLS_LIST, ServerCapability.TOOLS),
            entry(TOOLS_CALL, ServerCapability.TOOLS),
            entry(PROMPTS_LIST, ServerCapability.PROMPTS),
            entry(PROMPTS_GET, ServerCapability.PROMPTS),
            entry(LOGGING_SET_LEVEL, ServerCapability.LOGGING),
            entry(COMPLETION_COMPLETE, ServerCapability.COMPLETIONS)
    );

    public Optional<ServerCapability> serverCapability() {
        return Optional.ofNullable(SERVER_CAPABILITIES.get(this));
    }

    private static final Map<RequestMethod, ClientCapability> CLIENT_CAPABILITIES = Map.of(
            ROOTS_LIST, ClientCapability.ROOTS,
            SAMPLING_CREATE_MESSAGE, ClientCapability.SAMPLING,
            ELICITATION_CREATE, ClientCapability.ELICITATION
    );

    @Override
    public Optional<ClientCapability> clientCapability() {
        return Optional.ofNullable(CLIENT_CAPABILITIES.get(this));
    }
}
