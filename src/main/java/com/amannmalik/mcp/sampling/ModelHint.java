package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.JsonObject;

public record ModelHint(String name) {
    public static final JsonCodec<ModelHint> JSON = new Codec();

    public ModelHint {
        if (name != null) {
            name = InputSanitizer.requireClean(name);
        }
    }

    private static final class Codec extends AbstractEntityCodec<ModelHint> {
        @Override
        public JsonObject toJson(ModelHint entity) {
            var b = object();
            if (entity.name() != null) b.add("name", entity.name());
            return b.build();
        }

        @Override
        public ModelHint fromJson(JsonObject json) {
            String name = getString(json, "name");
            return new ModelHint(name);
        }
    }
}
