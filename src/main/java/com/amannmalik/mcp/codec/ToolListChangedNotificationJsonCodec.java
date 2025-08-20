package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.ToolListChangedNotification;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public final class ToolListChangedNotificationJsonCodec implements JsonCodec<ToolListChangedNotification> {
    @Override
    public JsonObject toJson(ToolListChangedNotification notification) {
        return JsonValue.EMPTY_JSON_OBJECT;
    }

    @Override
    public ToolListChangedNotification fromJson(JsonObject obj) {
        if (obj != null && !obj.isEmpty()) {
            throw new IllegalArgumentException("unexpected fields");
        }
        return new ToolListChangedNotification();
    }
}

