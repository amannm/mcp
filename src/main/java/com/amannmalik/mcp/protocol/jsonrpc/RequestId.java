package com.amannmalik.mcp.protocol.jsonrpc;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.util.Optional;

public sealed interface RequestId permits RequestId.IntId, RequestId.StringId {
    JsonValue json();
    record IntId(long value) implements RequestId {
        @Override public JsonValue json() { return Json.createValue(value); }
    }
    record StringId(String value) implements RequestId {
        @Override public JsonValue json() { return Json.createValue(value); }
    }
    static Optional<RequestId> from(JsonValue v) {
        return switch (v.getValueType()) {
            case NUMBER -> Optional.of(new IntId(((JsonNumber) v).longValue()));
            case STRING -> Optional.of(new StringId(((JsonString) v).getString()));
            default -> Optional.empty();
        };
    }
}
