package com.amannmalik.mcp.wire;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Common interface for request and notification method enums. */
public sealed interface WireMethod permits RequestMethod, NotificationMethod {
    String method();

    /**
     * Lookup the enum constant with the provided wire method name.
     * @param type enum class implementing {@code WireMethod}
     * @param method wire method string
     * @return optional enum constant
     */
    static <T extends Enum<T> & WireMethod> Optional<T> from(Class<T> type, String method) {
        if (method == null) return Optional.empty();
        Map<String, T> byMethod = Arrays.stream(type.getEnumConstants())
                .collect(Collectors.toUnmodifiableMap(WireMethod::method, e -> e));
        return Optional.ofNullable(byMethod.get(method));
    }
}
