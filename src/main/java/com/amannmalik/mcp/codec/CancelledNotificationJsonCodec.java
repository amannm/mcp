package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.CancelledNotification;
import com.amannmalik.mcp.jsonrpc.RequestId;
import jakarta.json.*;

public class CancelledNotificationJsonCodec implements JsonCodec<CancelledNotification> {
    @Override
    public JsonObject toJson(CancelledNotification note) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        b.add("requestId", RequestId.toJsonValue(note.requestId()));
        if (note.reason() != null) b.add("reason", note.reason());
        return b.build();
    }

    @Override
    public CancelledNotification fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        RequestId id = RequestId.from(obj.get("requestId"));
        String reason = obj.getString("reason", null);
        return new CancelledNotification(id, reason);
    }
}
