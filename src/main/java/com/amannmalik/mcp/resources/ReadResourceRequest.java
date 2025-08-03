package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.validation.MetaValidator;
import com.amannmalik.mcp.validation.UriValidator;
import jakarta.json.*;

import java.util.Set;

public record ReadResourceRequest(String uri, JsonObject _meta) {
    public static final JsonCodec<ReadResourceRequest> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(ReadResourceRequest req) {
            JsonObjectBuilder b = Json.createObjectBuilder().add("uri", req.uri());
            if (req._meta() != null) b.add("_meta", req._meta());
            return b.build();
        }

        @Override
        public ReadResourceRequest fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            requireOnlyKeys(obj, Set.of("uri", "_meta"));
            String uri = requireString(obj, "uri");
            JsonObject meta = obj.getJsonObject("_meta");
            return new ReadResourceRequest(uri, meta);
        }
    };

    public ReadResourceRequest {
        uri = UriValidator.requireAbsolute(uri);
        MetaValidator.requireValid(_meta);
    }
}
