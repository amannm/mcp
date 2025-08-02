package com.amannmalik.mcp.wire;

import com.amannmalik.mcp.lifecycle.ServerCapability;

import java.util.EnumSet;
import java.util.Optional;

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
    private final EnumSet<ServerCapability> capabilities;

    RequestMethod(String method) {
        this.method = method;
        this.capabilities = EnumSet.noneOf(ServerCapability.class);
    }

    RequestMethod(String method, ServerCapability capability) {
        this.method = method;
        this.capabilities = EnumSet.of(capability);
    }

    public String method() {
        return method;
    }

    public static Optional<RequestMethod> from(String method) {
        return WireMethod.from(RequestMethod.class, method);
    }

    public Optional<ServerCapability> requiredCapability() {
        return capabilities.stream().findFirst();
    }
}
