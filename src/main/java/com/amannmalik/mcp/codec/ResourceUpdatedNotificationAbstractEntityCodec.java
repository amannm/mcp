package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.model.ResourceUpdatedNotification;
import jakarta.json.*;

public final class ResourceUpdatedNotificationAbstractEntityCodec extends AbstractEntityCodec<ResourceUpdatedNotification> {
    @Override
    public JsonObject toJson(ResourceUpdatedNotification n) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("uri", n.uri());
        if (n.title() != null) b.add("title", n.title());
        return b.build();
    }

    @Override
    public ResourceUpdatedNotification fromJson(JsonObject obj) {
        String uri = requireString(obj, "uri");
        String title = obj.getString("title", null);
        return new ResourceUpdatedNotification(uri, title);
    }
}
