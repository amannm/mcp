package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.Ref;
import jakarta.json.Json;
import jakarta.json.JsonObject;

public class RefJsonCodec implements JsonCodec<Ref> {
    @Override
    public JsonObject toJson(Ref ref) {
        return switch (ref) {
            case Ref.PromptRef p -> {
                var b = Json.createObjectBuilder()
                        .add("type", p.type())
                        .add("name", p.name());
                if (p.title() != null) b.add("title", p.title());
                if (p._meta() != null) b.add("_meta", p._meta());
                yield b.build();
            }
            case Ref.ResourceRef r -> Json.createObjectBuilder()
                    .add("type", r.type())
                    .add("uri", r.uri())
                    .build();
        };
    }

    @Override
    public Ref fromJson(JsonObject obj) {
        var type = obj.getString("type", null);
        if (type == null) throw new IllegalArgumentException("type required");
        return switch (type) {
            case "ref/prompt" -> new Ref.PromptRef(
                    obj.getString("name"),
                    obj.getString("title", null),
                    obj.getJsonObject("_meta")
            );
            case "ref/resource" -> new Ref.ResourceRef(obj.getString("uri"));
            default -> throw new IllegalArgumentException("unknown ref type");
        };
    }
}
