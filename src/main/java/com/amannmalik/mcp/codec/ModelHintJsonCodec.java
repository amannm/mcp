package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.sampling.ModelHint;
import jakarta.json.*;

import java.util.Set;

public class ModelHintJsonCodec implements JsonCodec<ModelHint> {
    @Override
    public JsonObject toJson(ModelHint h) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (h.name() != null) b.add("name", h.name());
        return b.build();
    }

    @Override
    public ModelHint fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        AbstractEntityCodec.requireOnlyKeys(obj, Set.of("name"));
        return new ModelHint(obj.getString("name", null));
    }
}
