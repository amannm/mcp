package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.Notification.CancelledNotification;
import com.amannmalik.mcp.api.RequestId;
import jakarta.json.Json;
import jakarta.json.JsonObject;

public class CancelledNotificationJsonCodec implements JsonCodec<CancelledNotification> {
    @Override
    public JsonObject toJson(CancelledNotification note) {
        var b = Json.createObjectBuilder();
        b.add("requestId", RequestId.toJsonValue(note.requestId()));
        if (note.reason() != null) {
            b.add("reason", note.reason());
        }
        return b.build();
    }

    @Override
    public CancelledNotification fromJson(JsonObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("object required");
        }
        var id = RequestId.from(obj.get("requestId"));
        var reason = obj.getString("reason", null);
        return new CancelledNotification(id, reason);
    }
}
