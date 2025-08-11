package com.amannmalik.mcp.api;

import java.util.Optional;

sealed interface JsonRpcMethod permits RequestMethod, NotificationMethod {
    static <T extends Enum<T> & JsonRpcMethod> Optional<T> from(Class<T> type, String method) {
        if (method == null) return Optional.empty();
        for (T value : type.getEnumConstants()) {
            if (value.method().equals(method)) return Optional.of(value);
        }
        return Optional.empty();
    }

    String method();
}
