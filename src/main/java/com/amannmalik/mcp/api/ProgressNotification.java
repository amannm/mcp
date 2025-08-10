package com.amannmalik.mcp.api;

import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.Optional;

public record ProgressNotification(
        ProgressToken token,
        double progress,
        Double total,
        String message
) {
    static final JsonCodec<ProgressNotification> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ProgressNotification note) {
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

        @Override
        public ProgressNotification fromJson(JsonObject obj) {
            ProgressToken token = switch (obj.get("progressToken").getValueType()) {
                case STRING -> new ProgressToken.StringToken(ValidationUtil.requireClean(obj.getString("progressToken")));
                case NUMBER -> new ProgressToken.NumericToken(obj.getJsonNumber("progressToken").longValue());
                default -> throw new IllegalArgumentException("progressToken must be string or number");
            };
            double progress = obj.getJsonNumber("progress").doubleValue();
            Double total = obj.containsKey("total") ? obj.getJsonNumber("total").doubleValue() : null;
            String message = obj.getString("message", null);
            return new ProgressNotification(token, progress, total, message);
        }
    };

    public static JsonObject toJson(ProgressNotification note) {
        return CODEC.toJson(note);
    }

    public static Optional<ProgressToken> fromMeta(JsonObject params) {
        if (params == null || !params.containsKey("_meta")) return Optional.empty();
        JsonObject meta = params.getJsonObject("_meta");
        ValidationUtil.requireMeta(meta);
        if (!meta.containsKey("progressToken")) return Optional.empty();
        JsonValue val = meta.get("progressToken");
        ProgressToken token = switch (val.getValueType()) {
            case STRING -> new ProgressToken.StringToken(ValidationUtil.requireClean(meta.getString("progressToken")));
            case NUMBER -> new ProgressToken.NumericToken(meta.getJsonNumber("progressToken").longValue());
            default -> throw new IllegalArgumentException("progressToken must be a string or number");
        };
        return Optional.of(token);
    }

    public ProgressNotification {
        if (token == null) throw new IllegalArgumentException("token is required");
        progress = ValidationUtil.requireNonNegative(progress, "progress");
        if (total != null) {
            total = ValidationUtil.requirePositive(total, "total");
            if (progress > total) {
                throw new IllegalArgumentException("progress must not exceed total");
            }
        }
        message = ValidationUtil.cleanNullable(message);
    }
}
