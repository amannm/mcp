package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.resources.ReadResourceResult;
import com.amannmalik.mcp.spi.ResourceBlock;
import jakarta.json.*;

import java.util.ArrayList;
import java.util.List;

public class ReadResourceResultJsonCodec implements JsonCodec<ReadResourceResult> {

    private static final ResourceBlockJsonCodec RESOURCE_BLOCK_CODEC = new ResourceBlockJsonCodec();

    @Override
    public JsonObject toJson(ReadResourceResult r) {
        var arr = Json.createArrayBuilder();
        r.contents().forEach(c -> arr.add(RESOURCE_BLOCK_CODEC.toJson(c)));
        return AbstractEntityCodec.addMeta(Json.createObjectBuilder().add("contents", arr.build()), r._meta()).build();
    }

    @Override
    public ReadResourceResult fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        var arr = obj.getJsonArray("contents");
        if (arr == null) throw new IllegalArgumentException("contents required");
        List<ResourceBlock> contents = new ArrayList<>();
        for (var v : arr) {
            if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException("content must be object");
            }
            contents.add(RESOURCE_BLOCK_CODEC.fromJson(v.asJsonObject()));
        }
        return new ReadResourceResult(contents, AbstractEntityCodec.meta(obj));
    }
}
