package com.amannmalik.mcp.wire;

import com.amannmalik.mcp.lifecycle.ServerCapability;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum RequestMethod implements WireMethod {
    INITIALIZE("initialize"),
    PING("ping"),
    RESOURCES_LIST("resources/list", ServerCapability.RESOURCES),
    RESOURCES_TEMPLATES_LIST("resources/templates/list", ServerCapability.RESOURCES),
    RESOURCES_READ("resources/read", ServerCapability.RESOURCES),
    RESOURCES_SUBSCRIBE("resources/subscribe", ServerCapability.RESOURCES),
    RESOURCES_UNSUBSCRIBE("resources/unsubscribe", ServerCapability.RESOURCES),
    TOOLS_LIST("tools/list", ServerCapability.TOOLS),
    TOOLS_CALL("tools/call", ServerCapability.TOOLS),
    PROMPTS_LIST("prompts/list", ServerCapability.PROMPTS),
    PROMPTS_GET("prompts/get", ServerCapability.PROMPTS),
    LOGGING_SET_LEVEL("logging/setLevel", ServerCapability.LOGGING),
    COMPLETION_COMPLETE("completion/complete", ServerCapability.COMPLETIONS),
    SAMPLING_CREATE_MESSAGE("sampling/createMessage"),
    ROOTS_LIST("roots/list"),
    ELICITATION_CREATE("elicitation/create");

    private final String method;
    private final ServerCapability capability;
    private static final Map<String, RequestMethod> BY_METHOD =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(RequestMethod::method, m -> m));

    RequestMethod(String method) {
        this(method, null);
    }

    RequestMethod(String method, ServerCapability capability) {
        this.method = method;
        this.capability = capability;
    }

    public String method() {
        return method;
    }

    public static Optional<RequestMethod> from(String method) {
        if (method == null) return Optional.empty();
        return Optional.ofNullable(BY_METHOD.get(method));
    }

    public Optional<ServerCapability> requiredCapability() {
        return Optional.ofNullable(capability);
    }
}
