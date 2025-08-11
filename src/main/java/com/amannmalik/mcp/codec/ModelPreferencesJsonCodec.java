package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.sampling.ModelHint;
import com.amannmalik.mcp.sampling.ModelPreferences;
import jakarta.json.*;

import java.util.List;
import java.util.Set;

public class ModelPreferencesJsonCodec implements JsonCodec<ModelPreferences> {
    @Override
    public JsonObject toJson(ModelPreferences p) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (!p.hints().isEmpty()) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            p.hints().forEach(h -> arr.add(ModelHint.CODEC.toJson(h)));
            b.add("hints", arr.build());
        }
        if (p.costPriority() != null) b.add("costPriority", p.costPriority());
        if (p.speedPriority() != null) b.add("speedPriority", p.speedPriority());
        if (p.intelligencePriority() != null) b.add("intelligencePriority", p.intelligencePriority());
        return b.build();
    }

    @Override
    public ModelPreferences fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        AbstractEntityCodec.requireOnlyKeys(obj, Set.of("hints", "costPriority", "speedPriority", "intelligencePriority"));
        List<ModelHint> hints = obj.containsKey("hints")
                ? obj.getJsonArray("hints").stream()
                .map(v -> ModelHint.CODEC.fromJson(v.asJsonObject()))
                .toList()
                : List.of();
        Double cost = obj.containsKey("costPriority") ? obj.getJsonNumber("costPriority").doubleValue() : null;
        Double speed = obj.containsKey("speedPriority") ? obj.getJsonNumber("speedPriority").doubleValue() : null;
        Double intel = obj.containsKey("intelligencePriority") ? obj.getJsonNumber("intelligencePriority").doubleValue() : null;
        return new ModelPreferences(hints, cost, speed, intel);
    }
}
