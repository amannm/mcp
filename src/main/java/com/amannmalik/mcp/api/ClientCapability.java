package com.amannmalik.mcp.api;

import java.util.*;
import java.util.stream.Collectors;

public enum ClientCapability {
    ROOTS("roots"),
    SAMPLING("sampling"),
    ELICITATION("elicitation"),
    EXPERIMENTAL("experimental");

    private static final Map<String, ClientCapability> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ClientCapability::code, c -> c));

    private final String code;

    ClientCapability(String code) {
        this.code = code;
    }

    public static Optional<ClientCapability> from(String raw) {
        if (raw == null) return Optional.empty();
        return Optional.ofNullable(BY_CODE.get(raw));
    }

    public String code() {
        return code;
    }
}
