package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.core.*;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.Set;

public record ListRootsRequest(JsonObject _meta) {
    public static final JsonCodec<ListRootsRequest> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(ListRootsRequest req) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            if (req._meta() != null) b.add("_meta", req._meta());
            return b.build();
        }

        @Override
        public ListRootsRequest fromJson(JsonObject obj) {
            if (obj != null) {
                requireOnlyKeys(obj, Set.of("_meta"));
                return new ListRootsRequest(getObject(obj, "_meta"));
            }
            return new ListRootsRequest(null);
        }
    };

    public ListRootsRequest {
        MetaValidator.requireValid(_meta);
    }
}
