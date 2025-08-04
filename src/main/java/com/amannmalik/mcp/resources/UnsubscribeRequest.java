package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.validation.ValidationUtil;
import jakarta.json.*;

import java.util.Set;

public record UnsubscribeRequest(String uri, JsonObject _meta) {
    public static final JsonCodec<UnsubscribeRequest> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(UnsubscribeRequest req) {
            JsonObjectBuilder b = Json.createObjectBuilder().add("uri", req.uri());
            if (req._meta() != null) b.add("_meta", req._meta());
            return b.build();
        }

        @Override
        public UnsubscribeRequest fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            requireOnlyKeys(obj, Set.of("uri", "_meta"));
            String uri = requireString(obj, "uri");
            JsonObject meta = obj.getJsonObject("_meta");
            return new UnsubscribeRequest(uri, meta);
        }
    };

    public UnsubscribeRequest {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        ValidationUtil.requireMeta(_meta);
    }
}
