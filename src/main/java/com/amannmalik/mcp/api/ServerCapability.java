package com.amannmalik.mcp.api;

import java.util.Optional;

public enum ServerCapability {
    PROMPTS("prompts"),
    RESOURCES("resources"),
    TOOLS("tools"),
    LOGGING("logging"),
    COMPLETIONS("completions"),
    EXPERIMENTAL("experimental");

    private final String code;

    ServerCapability(String code) {
        this.code = code;
    }

    public static Optional<ServerCapability> from(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return switch (raw) {
            case "prompts" -> Optional.of(PROMPTS);
            case "resources" -> Optional.of(RESOURCES);
            case "tools" -> Optional.of(TOOLS);
            case "logging" -> Optional.of(LOGGING);
            case "completions" -> Optional.of(COMPLETIONS);
            case "experimental" -> Optional.of(EXPERIMENTAL);
            default -> Optional.empty();
        };
    }

    public String code() {
        return code;
    }
}
