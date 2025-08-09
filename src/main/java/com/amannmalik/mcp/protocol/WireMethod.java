package com.amannmalik.mcp.protocol;

import java.util.Optional;

public sealed interface WireMethod permits RequestMethod, NotificationMethod {
    String method();

    static <T extends Enum<T> & WireMethod> Optional<T> from(Class<T> type, String method) {
        if (method == null) return Optional.empty();
        for (T value : type.getEnumConstants()) {
            if (value.method().equals(method)) return Optional.of(value);
        }
        return Optional.empty();
    }
}
