package com.mcp.protocol.foundation;

import java.util.Objects;
import java.util.Optional;

public record Implementation(String name, String version, Optional<String> title) {
    public Implementation {
        Objects.requireNonNull(name);
        Objects.requireNonNull(version);
        Objects.requireNonNull(title);
    }
}
