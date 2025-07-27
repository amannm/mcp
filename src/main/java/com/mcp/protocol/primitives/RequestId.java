package com.mcp.protocol.primitives;

import java.util.Objects;

public sealed interface RequestId permits RequestId.StringId, RequestId.IntegerId {
    record StringId(String value) implements RequestId {
        public StringId {
            Objects.requireNonNull(value);
        }
    }
    record IntegerId(long value) implements RequestId {}
}
