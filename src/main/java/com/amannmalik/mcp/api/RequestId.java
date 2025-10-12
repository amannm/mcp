package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.PlatformLog;
import jakarta.json.*;

import java.lang.System.Logger;
import java.util.Optional;

public sealed interface RequestId permits
        RequestId.StringId,
        RequestId.NumericId,
        RequestId.NullId {
    Logger LOG = PlatformLog.get(RequestId.class);

    static RequestId parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("raw required");
        }
        if (raw.equals("null")) {
            return NullId.INSTANCE;
        }
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() > 1) {
            return new StringId(raw.substring(1, raw.length() - 1));
        }
        try {
            return new NumericId(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            LOG.log(Logger.Level.DEBUG, "Non-numeric request id: " + raw, e);
            return new StringId(raw);
        }
    }

    static JsonValue toJsonValue(RequestId id) {
        return switch (id) {
            case StringId s -> Json.createValue(s.value());
            case NumericId n -> Json.createValue(n.value());
            case NullId ignored -> JsonValue.NULL;
        };
    }

    static RequestId from(JsonValue value) {
        if (value == null || value == JsonValue.NULL) {
            throw new IllegalArgumentException("id is required");
        }
        return switch (value) {
            case JsonString js -> new StringId(js.getString());
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
                yield new NumericId(val);
            }
            default -> throw new IllegalArgumentException("Invalid id type");
        };
    }

    static Optional<RequestId> fromNullable(JsonValue value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value == JsonValue.NULL) {
            return Optional.of(NullId.INSTANCE);
        }
        return Optional.of(from(value));
    }

    enum NullId implements RequestId {
        INSTANCE;

        @Override
        public String toString() {
            return "null";
        }
    }

    record StringId(String value) implements RequestId {
        @Override
        public String toString() {
            return value;
        }
    }

    record NumericId(long value) implements RequestId {
        @Override
        public String toString() {
            return Long.toString(value);
        }
    }
}
