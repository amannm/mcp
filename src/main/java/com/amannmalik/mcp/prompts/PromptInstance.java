package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import jakarta.json.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record PromptInstance(String description, List<PromptMessage> messages) {
    public static final JsonCodec<PromptInstance> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(PromptInstance inst) {
            JsonArrayBuilder msgs = Json.createArrayBuilder();
            inst.messages().forEach(m -> msgs.add(PromptMessage.CODEC.toJson(m)));
            JsonObjectBuilder obj = Json.createObjectBuilder().add("messages", msgs.build());
            if (inst.description() != null) obj.add("description", inst.description());
            return obj.build();
        }

        @Override
        public PromptInstance fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            AbstractEntityCodec.requireOnlyKeys(obj, Set.of("messages", "description"));
            JsonArray arr = obj.getJsonArray("messages");
            if (arr == null) throw new IllegalArgumentException("messages required");
            List<PromptMessage> msgs = new ArrayList<>();
            for (JsonValue v : arr) {
                if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                    throw new IllegalArgumentException("message must be object");
                }
                msgs.add(PromptMessage.CODEC.fromJson(v.asJsonObject()));
            }
            String desc = obj.getString("description", null);
            return new PromptInstance(desc, msgs);
        }
    };

    public PromptInstance {
        messages = messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
    }
}
