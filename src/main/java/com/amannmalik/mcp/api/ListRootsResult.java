package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.ListRootsResultAbstractEntityCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListRootsResult(List<Root> roots, JsonObject _meta) {
    public static final JsonCodec<ListRootsResult> CODEC = new ListRootsResultAbstractEntityCodec();

    public ListRootsResult {
        roots = roots == null || roots.isEmpty() ? List.of() : List.copyOf(roots);
        ValidationUtil.requireMeta(_meta);
    }

}
