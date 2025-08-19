package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.ProgressNotification;
import com.amannmalik.mcp.api.ProgressToken;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

public class ProgressNotificationJsonCodec implements JsonCodec<ProgressNotification> {
    @Override
    public JsonObject toJson(ProgressNotification note) {
        var b = Json.createObjectBuilder();
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
            case NUMBER -> {
                var num = obj.getJsonNumber("progressToken");
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
