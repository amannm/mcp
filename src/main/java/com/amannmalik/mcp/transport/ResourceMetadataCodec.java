package com.amannmalik.mcp.transport;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;

import java.util.ArrayList;
import java.util.List;

public final class ResourceMetadataCodec {
    private ResourceMetadataCodec() {
    }

    public static JsonObject toJsonObject(ResourceMetadata meta) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        meta.authorizationServers().forEach(arr::add);
        return Json.createObjectBuilder()
                .add("resource", meta.resource())
                .add("authorization_servers", arr.build())
                .build();
    }

    public static ResourceMetadata fromJsonObject(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String resource = obj.getString("resource", null);
        var arr = obj.getJsonArray("authorization_servers");
        if (arr == null) throw new IllegalArgumentException("authorization_servers required");
        List<String> servers = new ArrayList<>(arr.size());
        for (JsonString js : arr.getValuesAs(JsonString.class)) {
            servers.add(js.getString());
        }
        return new ResourceMetadata(resource, servers);
    }
}
