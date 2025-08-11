package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.model.ResourceBlock;
import com.amannmalik.mcp.resources.ReadResourceResult;
import jakarta.json.*;

import java.util.ArrayList;
import java.util.List;

public class ReadResourceResultJsonCodec implements JsonCodec<ReadResourceResult> {

    private static final ResourceBlockJsonCodec RESOURCE_BLOCK_CODEC = new ResourceBlockJsonCodec();

    @Override
    public JsonObject toJson(ReadResourceResult r) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        r.contents().forEach(c -> arr.add(RESOURCE_BLOCK_CODEC.toJson(c)));
        JsonObjectBuilder b = Json.createObjectBuilder().add("contents", arr.build());
        if (r._meta() != null) b.add("_meta", r._meta());
        return b.build();
    }

    @Override
    public ReadResourceResult fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        JsonArray arr = obj.getJsonArray("contents");
        if (arr == null) throw new IllegalArgumentException("contents required");
        List<ResourceBlock> contents = new ArrayList<>();
        for (JsonValue v : arr) {
            if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException("content must be object");
            }
            contents.add(RESOURCE_BLOCK_CODEC.fromJson(v.asJsonObject()));
        }
        JsonObject meta = obj.getJsonObject("_meta");
        return new ReadResourceResult(contents, meta);
    }
}
