package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.codec.AbstractEntityCodec;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ListRootsRequest(JsonObject _meta) {
    public static final JsonCodec<ListRootsRequest> CODEC =
            AbstractEntityCodec.metaOnly(ListRootsRequest::_meta, ListRootsRequest::new);

    public ListRootsRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
