package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.jsonrpc.RequestIdCodec;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

public final class CancellationCodec {
    private CancellationCodec() {
    }

    public static JsonObject toJsonObject(CancelledNotification note) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        RequestIdCodec.add(b, "requestId", note.requestId());
        if (note.reason() != null) b.add("reason", note.reason());
        return b.build();
    }

    public static CancelledNotification toCancelledNotification(JsonObject obj) {
        RequestId id = RequestIdCodec.from(obj.get("requestId"));
        String reason = obj.getString("reason", null);
        return new CancelledNotification(id, reason);
    }
}
