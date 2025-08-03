package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.JsonUtil;
import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.*;

import java.util.Set;

public record ModelHint(String name) {
    public static final JsonCodec<ModelHint> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ModelHint h) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            if (h.name() != null) b.add("name", h.name());
            return b.build();
        }

        @Override
        public ModelHint fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            JsonUtil.requireOnlyKeys(obj, Set.of("name"));
            return new ModelHint(obj.getString("name", null));
        }
    };

    public ModelHint {
        if (name != null) name = InputSanitizer.requireClean(name);
    }
}
