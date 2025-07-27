package com.amannmalik.mcp.protocol.jsonrpc;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.util.Optional;

public sealed interface ProgressToken permits ProgressToken.IntToken, ProgressToken.StringToken {
    JsonValue json();
    record IntToken(long value) implements ProgressToken {
        @Override public JsonValue json() { return Json.createValue(value); }
    }
    record StringToken(String value) implements ProgressToken {
        @Override public JsonValue json() { return Json.createValue(value); }
    }
    static Optional<ProgressToken> from(JsonValue v) {
        return switch (v.getValueType()) {
            case NUMBER -> Optional.of(new IntToken(((JsonNumber) v).longValue()));
            case STRING -> Optional.of(new StringToken(((JsonString) v).getString()));
            default -> Optional.empty();
        };
    }
}
