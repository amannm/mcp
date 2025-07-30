package com.amannmalik.mcp;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum RequestMethod {
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

    private static final Map<String, RequestMethod> BY_METHOD;
    private final String method;

    static {
        BY_METHOD = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(m -> m.method, m -> m));
    }

    RequestMethod(String method) {
        this.method = method;
    }

    public String method() {
        return method;
    }

    public static Optional<RequestMethod> from(String method) {
        if (method == null) return Optional.empty();
        return Optional.ofNullable(BY_METHOD.get(method));
    }
}
