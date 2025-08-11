package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.api.ResourceBlock;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.ReadResourceResultJsonCodec;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ReadResourceResult(List<ResourceBlock> contents, JsonObject _meta) {


    public static final JsonCodec<ReadResourceResult> CODEC = new ReadResourceResultJsonCodec();

    public ReadResourceResult {
        contents = Immutable.list(contents);
        ValidationUtil.requireMeta(_meta);
    }

}
