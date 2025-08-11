package com.amannmalik.mcp.api.model;


import com.amannmalik.mcp.util.JsonRpcMethod;

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
}
