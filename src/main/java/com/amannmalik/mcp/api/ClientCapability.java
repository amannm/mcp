package com.amannmalik.mcp.api;

import java.util.Optional;

public enum ClientCapability {
    ROOTS("roots"),
    SAMPLING("sampling"),
    ELICITATION("elicitation"),
    EXPERIMENTAL("experimental");

    private final String code;

    ClientCapability(String code) {
        this.code = code;
    }

    public static Optional<ClientCapability> from(String raw) {
        if (raw == null) return Optional.empty();
        return switch (raw) {
            case "roots" -> Optional.of(ROOTS);
            case "sampling" -> Optional.of(SAMPLING);
            case "elicitation" -> Optional.of(ELICITATION);
            case "experimental" -> Optional.of(EXPERIMENTAL);
            default -> Optional.empty();
        };
    }

    public String code() {
        return code;
    }
}
