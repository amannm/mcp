package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.RequestId;
import jakarta.json.*;

import java.util.Optional;

public final class RequestIdCodec {
    private RequestIdCodec() {
    }

    public static JsonValue toJsonValue(RequestId id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return switch (id) {
            case RequestId.StringId s -> Json.createValue(s.value());
            case RequestId.NumericId n -> Json.createValue(n.value());
            case RequestId.NullId ignored -> JsonValue.NULL;
        };
    }

    public static RequestId from(JsonValue value) {
        if (value == null || value == JsonValue.NULL) {
            throw new IllegalArgumentException("id is required");
        }
        return switch (value) {
            case JsonString js -> new RequestId.StringId(js.getString());
            case JsonNumber num -> {
                if (!num.isIntegral()) {
                    throw new IllegalArgumentException("id must be integer");
                }
                long val;
                try {
                    val = num.longValueExact();
                } catch (ArithmeticException e) {
                    throw new IllegalArgumentException("id out of range", e);
                }
                yield new RequestId.NumericId(val);
            }
            default -> throw new IllegalArgumentException("Invalid id type");
        };
    }

    public static Optional<RequestId> fromNullable(JsonValue value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value == JsonValue.NULL) {
            return Optional.of(RequestId.NullId.INSTANCE);
        }
        return Optional.of(from(value));
    }
}
