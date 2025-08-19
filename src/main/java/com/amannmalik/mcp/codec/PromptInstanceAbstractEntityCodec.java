package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.PromptInstance;
import com.amannmalik.mcp.spi.PromptMessage;
import jakarta.json.*;

import java.util.*;

public final class PromptInstanceAbstractEntityCodec extends AbstractEntityCodec<PromptInstance> {

    static final JsonCodec<PromptMessage> CODEC = new PromptMessageAbstractEntityCodec();

    @Override
    public JsonObject toJson(PromptInstance inst) {
        var msgs = Json.createArrayBuilder();
        inst.messages().forEach(m -> msgs.add(CODEC.toJson(m)));
        var obj = Json.createObjectBuilder().add("messages", msgs.build());
        if (inst.description() != null) obj.add("description", inst.description());
        return obj.build();
    }

    @Override
    public PromptInstance fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("messages", "description"));
        var arr = obj.getJsonArray("messages");
        if (arr == null) throw new IllegalArgumentException("messages required");
        List<PromptMessage> msgs = new ArrayList<>();
        for (var v : arr) {
            if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException("message must be object");
            }
            msgs.add(CODEC.fromJson(v.asJsonObject()));
        }
        var desc = obj.getString("description", null);
        return new PromptInstance(desc, msgs);
    }
}
