package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.NotificationMethod;
import com.amannmalik.mcp.api.RequestMethod;

import java.util.Optional;

public sealed interface JsonRpcMethod permits RequestMethod, NotificationMethod {
    static <T extends Enum<T> & JsonRpcMethod> Optional<T> from(Class<T> type, String method) {
        if (method == null) return Optional.empty();
        for (T value : type.getEnumConstants()) {
            if (value.method().equals(method)) return Optional.of(value);
        }
        return Optional.empty();
    }

    String method();
}
