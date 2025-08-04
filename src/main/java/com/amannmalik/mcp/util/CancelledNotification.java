package com.amannmalik.mcp.util;

import com.amannmalik.mcp.core.*;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.*;
import java.util.Set;

public record CancelledNotification(RequestId requestId, String reason) {
    public static final JsonCodec<CancelledNotification> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(CancelledNotification note) {
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("requestId", RequestId.toJsonValue(note.requestId()));
            if (note.reason() != null) b.add("reason", note.reason());
            return b.build();
        }

        @Override
        public CancelledNotification fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            requireOnlyKeys(obj, Set.of("requestId", "reason"));
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
