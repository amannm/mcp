package com.amannmalik.mcp.api;

import jakarta.json.*;

public sealed interface RequestId permits
        RequestId.StringId,
        RequestId.NumericId,
        RequestId.NullId {

    static RequestId parse(String raw) {
        if (raw == null) throw new IllegalArgumentException("raw required");
        if (raw.equals("null")) return NullId.INSTANCE;
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() > 1) {
            return new StringId(raw.substring(1, raw.length() - 1));
        }
        try {
            return new NumericId(Long.parseLong(raw));
        } catch (NumberFormatException ignore) {
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
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
            throw new IllegalArgumentException("id is required");
        }
        return switch (value.getValueType()) {
            case STRING -> new StringId(((JsonString) value).getString());
            case NUMBER -> {
                JsonNumber num = (JsonNumber) value;
                if (!num.isIntegral()) throw new IllegalArgumentException("id must be an integer");
                yield new NumericId(num.longValue());
            }
            default -> throw new IllegalArgumentException("Invalid id type");
        };
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
