package com.amannmalik.mcp.util;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.util.Optional;

public final class ProgressCodec {
    private ProgressCodec() {
    }

    public static JsonObject toJsonObject(ProgressNotification note) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        switch (note.token()) {
            case ProgressToken.StringToken s -> b.add("progressToken", s.value());
            case ProgressToken.NumericToken n -> b.add("progressToken", n.value());
        }
        b.add("progress", note.progress());
        if (note.total() != null) b.add("total", note.total());
        if (note.message() != null) b.add("message", note.message());
        return b.build();
    }

    public static ProgressNotification toProgressNotification(JsonObject obj) {
        ProgressToken token = toToken(obj.get("progressToken"));
        double progress = obj.getJsonNumber("progress").doubleValue();
        Double total = obj.containsKey("total") ? obj.getJsonNumber("total").doubleValue() : null;
        String message = obj.getString("message", null);
        return new ProgressNotification(token, progress, total, message);
    }

    private static ProgressToken toToken(JsonValue value) {
        return switch (value.getValueType()) {
            case STRING -> new ProgressToken.StringToken(InputSanitizer.requireClean(((JsonString) value).getString()));
            case NUMBER -> new ProgressToken.NumericToken(((JsonNumber) value).longValue());
            default -> throw new IllegalArgumentException("Invalid token type");
        };
    }

    public static Optional<ProgressToken> fromMeta(JsonObject params) {
        if (params == null || !params.containsKey("_meta")) return Optional.empty();
        JsonObject meta = params.getJsonObject("_meta");
        MetaValidator.requireValid(meta);
        if (!meta.containsKey("progressToken")) return Optional.empty();
        var val = meta.get("progressToken");
        ProgressToken token = switch (val.getValueType()) {
            case STRING -> new ProgressToken.StringToken(
                    InputSanitizer.requireClean(meta.getString("progressToken"))
            );
            case NUMBER -> new ProgressToken.NumericToken(meta.getJsonNumber("progressToken").longValue());
            default -> throw new IllegalArgumentException("progressToken must be a string or number");
        };
        return Optional.of(token);
    }
}
