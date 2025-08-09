package com.amannmalik.mcp.transport;

import jakarta.json.*;

import java.util.List;

public record ResourceMetadata(String resource, List<String> authorizationServers) {
    public static final JsonCodec<ResourceMetadata> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ResourceMetadata meta) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            meta.authorizationServers().forEach(arr::add);
            return Json.createObjectBuilder()
                    .add("resource", meta.resource())
                    .add("authorization_servers", arr.build())
                    .build();
        }

        @Override
        public ResourceMetadata fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            String resource = obj.getString("resource", null);
            JsonArray arr = obj.getJsonArray("authorization_servers");
            if (resource == null || arr == null) throw new IllegalArgumentException("resource and authorization_servers required");
            List<String> servers = arr.getValuesAs(JsonString.class).stream().map(JsonString::getString).toList();
            return new ResourceMetadata(resource, servers);
        }
    };

    public ResourceMetadata {
        if (resource == null || resource.isBlank()) throw new IllegalArgumentException("resource required");
        if (authorizationServers == null || authorizationServers.isEmpty()) {
            throw new IllegalArgumentException("authorizationServers required");
        }
        authorizationServers = List.copyOf(authorizationServers);
    }
}
