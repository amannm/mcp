package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ListRootsRequest(JsonObject _meta) {
    public static final JsonCodec<ListRootsRequest> CODEC =
            AbstractEntityCodec.metaOnly(ListRootsRequest::_meta, ListRootsRequest::new);

    public ListRootsRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
