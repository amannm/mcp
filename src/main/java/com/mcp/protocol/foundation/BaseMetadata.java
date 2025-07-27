package com.mcp.protocol.foundation;

import java.util.Objects;
import java.util.Optional;

public record BaseMetadata(String name, Optional<String> title) {
    public BaseMetadata {
        Objects.requireNonNull(name);
        Objects.requireNonNull(title);
    }
}
