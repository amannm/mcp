package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.Optional;

public sealed interface ProgressToken permits
        ProgressToken.StringToken,
        ProgressToken.NumericToken {
    static Optional<ProgressToken> fromMeta(JsonObject params) {
        if (params == null || !params.containsKey("_meta")) return Optional.empty();
        JsonObject meta = params.getJsonObject("_meta");
        ValidationUtil.requireMeta(meta);
        if (!meta.containsKey("progressToken")) return Optional.empty();
        JsonValue val = meta.get("progressToken");
        ProgressToken token = switch (val.getValueType()) {
            case STRING -> new ProgressToken.StringToken(ValidationUtil.requireClean(meta.getString("progressToken")));
            case NUMBER -> {
                var num = meta.getJsonNumber("progressToken");
                if (!num.isIntegral()) {
                    throw new IllegalArgumentException("progressToken must be a string or integer");
                }
                yield new ProgressToken.NumericToken(num.longValueExact());
            }
            default -> throw new IllegalArgumentException("progressToken must be a string or number");
        };
        return Optional.of(token);
    }

    String asString();

    record StringToken(String value) implements ProgressToken {
        public StringToken {
            value = ValidationUtil.requireClean(value);
        }

        @Override
        public String asString() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    record NumericToken(long value) implements ProgressToken {
        @Override
        public String asString() {
            return Long.toString(value);
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }
    }
}
