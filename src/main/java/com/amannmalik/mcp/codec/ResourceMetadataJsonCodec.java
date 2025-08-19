package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.core.ResourceMetadata;
import jakarta.json.*;

public class ResourceMetadataJsonCodec implements JsonCodec<ResourceMetadata> {
    @Override
    public JsonObject toJson(ResourceMetadata meta) {
        var arr = Json.createArrayBuilder();
        meta.authorizationServers().forEach(arr::add);
        return Json.createObjectBuilder()
                .add("resource", meta.resource())
                .add("authorization_servers", arr.build())
                .build();
    }

    @Override
    public ResourceMetadata fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        var resource = obj.getString("resource", null);
        var arr = obj.getJsonArray("authorization_servers");
        if (resource == null || arr == null) throw new IllegalArgumentException("resource and authorization_servers required");
        var servers = arr.getValuesAs(JsonString.class).stream().map(JsonString::getString).toList();
        return new ResourceMetadata(resource, servers);
    }
}
