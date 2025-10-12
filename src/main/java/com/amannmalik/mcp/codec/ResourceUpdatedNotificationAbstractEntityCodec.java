package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.Notification.ResourceUpdatedNotification;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.net.URI;

public final class ResourceUpdatedNotificationAbstractEntityCodec extends AbstractEntityCodec<ResourceUpdatedNotification> {
    public ResourceUpdatedNotificationAbstractEntityCodec() {
    }

    @Override
    public JsonObject toJson(ResourceUpdatedNotification n) {
        var b = Json.createObjectBuilder().add("uri", n.uri().toString());
        if (n.title() != null) {
            b.add("title", n.title());
        }
        return b.build();
    }

    @Override
    public ResourceUpdatedNotification fromJson(JsonObject obj) {
        var uriString = requireString(obj, "uri");
        var uri = URI.create(uriString);
        var title = obj.getString("title", null);
        return new ResourceUpdatedNotification(uri, title);
    }
}
