package com.amannmalik.mcp.util;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.*;

public record CancelledNotification(RequestId requestId, String reason) {
    public static final JsonCodec<CancelledNotification> CODEC = new JsonCodec<>() {
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
    };

    public CancelledNotification {
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        reason = InputSanitizer.cleanNullable(reason);
    }
}
