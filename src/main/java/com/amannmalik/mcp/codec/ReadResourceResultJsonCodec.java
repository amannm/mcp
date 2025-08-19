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
        var b = Json.createObjectBuilder().add("contents", arr.build());
        if (r._meta() != null) b.add("_meta", r._meta());
        return b.build();
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
        var meta = obj.getJsonObject("_meta");
        return new ReadResourceResult(contents, meta);
    }
}
