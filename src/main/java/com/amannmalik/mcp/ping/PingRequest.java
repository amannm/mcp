package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.Set;

public record PingRequest(JsonObject _meta) {
    public static final JsonCodec<PingRequest> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(PingRequest req) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            if (req._meta() != null) b.add("_meta", req._meta());
            return b.build();
        }

        @Override
        public PingRequest fromJson(JsonObject obj) {
            if (obj != null) AbstractEntityCodec.requireOnlyKeys(obj, Set.of("_meta"));
            return new PingRequest(obj == null ? null : obj.getJsonObject("_meta"));
        }
    };

    public PingRequest {
        MetaValidator.requireValid(_meta);
    }
}
