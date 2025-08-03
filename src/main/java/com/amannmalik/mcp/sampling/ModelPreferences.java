package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import java.util.List;

public record ModelPreferences(
        List<ModelHint> hints,
        Double costPriority,
        Double speedPriority,
        Double intelligencePriority
) {
    public static final JsonCodec<ModelPreferences> JSON = new Codec();

    public ModelPreferences {
        hints = hints == null || hints.isEmpty() ? List.of() : List.copyOf(hints);
        if (costPriority != null && (costPriority < 0.0 || costPriority > 1.0)) {
            throw new IllegalArgumentException("costPriority must be between 0.0 and 1.0");
        }
        if (speedPriority != null && (speedPriority < 0.0 || speedPriority > 1.0)) {
            throw new IllegalArgumentException("speedPriority must be between 0.0 and 1.0");
        }
        if (intelligencePriority != null && (intelligencePriority < 0.0 || intelligencePriority > 1.0)) {
            throw new IllegalArgumentException("intelligencePriority must be between 0.0 and 1.0");
        }
    }

    private static final class Codec extends AbstractEntityCodec<ModelPreferences> {
        @Override
        public JsonObject toJson(ModelPreferences entity) {
            var b = object();
            if (!entity.hints().isEmpty()) {
                JsonArrayBuilder arr = array();
                entity.hints().forEach(h -> arr.add(ModelHint.JSON.toJson(h)));
                b.add("hints", arr.build());
            }
            if (entity.costPriority() != null) b.add("costPriority", entity.costPriority());
            if (entity.speedPriority() != null) b.add("speedPriority", entity.speedPriority());
            if (entity.intelligencePriority() != null) b.add("intelligencePriority", entity.intelligencePriority());
            return b.build();
        }

        @Override
        public ModelPreferences fromJson(JsonObject json) {
            List<ModelHint> hints = json.containsKey("hints")
                    ? json.getJsonArray("hints").stream().map(v -> ModelHint.JSON.fromJson(v.asJsonObject())).toList()
                    : List.of();
            Double cost = getDouble(json, "costPriority");
            Double speed = getDouble(json, "speedPriority");
            Double intel = getDouble(json, "intelligencePriority");
            return new ModelPreferences(hints, cost, speed, intel);
        }
    }
}
