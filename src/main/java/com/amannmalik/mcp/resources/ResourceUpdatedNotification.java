package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.transport.AbstractEntityCodec;
import com.amannmalik.mcp.transport.JsonCodec;
import com.amannmalik.mcp.validation.ValidationUtil;
import jakarta.json.*;

public record ResourceUpdatedNotification(String uri, String title) {
    public static final JsonCodec<ResourceUpdatedNotification> CODEC = new AbstractEntityCodec<>() {
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
    };

    public ResourceUpdatedNotification {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        title = ValidationUtil.cleanNullable(title);
    }
}
