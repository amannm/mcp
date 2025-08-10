package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ListResourcesRequest(String cursor, JsonObject _meta) {
    static final JsonCodec<ListResourcesRequest> CODEC =
            AbstractEntityCodec.paginatedRequest(
                    ListResourcesRequest::cursor,
                    ListResourcesRequest::_meta,
                    ListResourcesRequest::new);

    public ListResourcesRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
