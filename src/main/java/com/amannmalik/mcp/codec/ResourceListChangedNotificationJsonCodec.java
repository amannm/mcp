package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.core.ResourceListChangedNotification;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public final class ResourceListChangedNotificationJsonCodec implements JsonCodec<ResourceListChangedNotification> {
    @Override
    public JsonObject toJson(ResourceListChangedNotification notification) {
        return JsonValue.EMPTY_JSON_OBJECT;
    }

    @Override
    public ResourceListChangedNotification fromJson(JsonObject obj) {
        if (obj != null && !obj.isEmpty()) {
            throw new IllegalArgumentException("unexpected fields");
        }
        return new ResourceListChangedNotification();
    }
}
