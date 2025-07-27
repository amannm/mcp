package com.mcp.protocol.foundation;

import java.util.Objects;
import java.util.Optional;

public record ModelHint(Optional<String> name) {
    public ModelHint {
        Objects.requireNonNull(name);
    }
}
