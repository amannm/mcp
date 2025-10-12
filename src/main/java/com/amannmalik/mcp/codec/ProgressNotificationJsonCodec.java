package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.Notification.ProgressNotification;
import com.amannmalik.mcp.api.ProgressToken;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

public class ProgressNotificationJsonCodec implements JsonCodec<ProgressNotification> {
    public ProgressNotificationJsonCodec() {
    }

    @Override
    public JsonObject toJson(ProgressNotification note) {
        var b = Json.createObjectBuilder();
        switch (note.token()) {
            case ProgressToken.StringToken s -> b.add("progressToken", s.value());
            case ProgressToken.NumericToken n -> b.add("progressToken", n.value());
        }
        b.add("progress", note.progress());
        if (note.total() != null) {
            b.add("total", note.total());
        }
        if (note.message() != null) {
            b.add("message", note.message());
        }
        return b.build();
    }

    @Override
    public ProgressNotification fromJson(JsonObject obj) {
        var pt = obj.get("progressToken");
        ProgressToken token = switch (pt) {
            case JsonString js -> new ProgressToken.StringToken(ValidationUtil.requireNonBlank(js.getString()));
            case JsonNumber num -> {
                if (!num.isIntegral()) {
                    throw new IllegalArgumentException("progressToken must be an integer");
                }
                yield new ProgressToken.NumericToken(num.bigIntegerValueExact());
            }
            default -> throw new IllegalArgumentException("progressToken must be string or number");
        };
        var progress = obj.getJsonNumber("progress").doubleValue();
        var total = obj.containsKey("total") ? obj.getJsonNumber("total").doubleValue() : null;
        var message = obj.getString("message", null);
        return new ProgressNotification(token, progress, total, message);
    }
}
