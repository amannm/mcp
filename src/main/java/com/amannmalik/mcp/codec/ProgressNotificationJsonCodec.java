package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.ProgressNotification;
import com.amannmalik.mcp.api.ProgressToken;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

public class ProgressNotificationJsonCodec implements JsonCodec<ProgressNotification> {
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
}
