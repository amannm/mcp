package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.math.BigInteger;
import java.util.Optional;

public sealed interface ProgressToken permits
        ProgressToken.StringToken,
        ProgressToken.NumericToken {

    static Optional<ProgressToken> fromMeta(JsonObject params) {
        if (params == null || !params.containsKey("_meta")) {
            return Optional.empty();
        }
        var metaValue = params.get("_meta");
        if (!(metaValue instanceof JsonObject meta)) {
            throw new IllegalArgumentException("_meta must be an object");
        }
        ValidationUtil.requireMeta(meta);
        if (!meta.containsKey("progressToken")) {
            return Optional.empty();
        }
        var val = meta.get("progressToken");
        ProgressToken token = switch (val) {
            case JsonString js -> new ProgressToken.StringToken(js.getString());
            case JsonNumber jn -> {
                if (!jn.isIntegral()) {
                    throw new IllegalArgumentException("progressToken must be a string or integer");
                }
                yield new ProgressToken.NumericToken(jn.bigIntegerValueExact());
            }
            default -> throw new IllegalArgumentException("progressToken must be a string or number");
        };
        return Optional.of(token);
    }

    record StringToken(String value) implements ProgressToken {
        public StringToken {
            value = ValidationUtil.requireNonBlank(value);
        }
    }

    record NumericToken(BigInteger value) implements ProgressToken {
    }
}
