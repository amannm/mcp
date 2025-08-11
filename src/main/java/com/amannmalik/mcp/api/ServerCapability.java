package com.amannmalik.mcp.api;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum ServerCapability {
    PROMPTS("prompts"),
    RESOURCES("resources"),
    TOOLS("tools"),
    LOGGING("logging"),
    COMPLETIONS("completions"),
    EXPERIMENTAL("experimental");

    private static final Map<String, ServerCapability> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ServerCapability::code, c -> c));

    private final String code;

    ServerCapability(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<ServerCapability> from(String raw) {
        if (raw == null) return Optional.empty();
        return Optional.ofNullable(BY_CODE.get(raw));
    }
}
