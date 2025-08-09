package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.transport.AbstractEntityCodec;
import com.amannmalik.mcp.transport.JsonCodec;
import com.amannmalik.mcp.validation.ValidationUtil;
import jakarta.json.*;

import java.util.Set;

public record SubscribeRequest(String uri, JsonObject _meta) {
    public static final JsonCodec<SubscribeRequest> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(SubscribeRequest req) {
            JsonObjectBuilder b = Json.createObjectBuilder().add("uri", req.uri());
            if (req._meta() != null) b.add("_meta", req._meta());
            return b.build();
        }

        @Override
        public SubscribeRequest fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            requireOnlyKeys(obj, Set.of("uri", "_meta"));
            String uri = requireString(obj, "uri");
            JsonObject meta = obj.getJsonObject("_meta");
            return new SubscribeRequest(uri, meta);
        }
    };

    public SubscribeRequest {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        ValidationUtil.requireMeta(_meta);
    }
}
