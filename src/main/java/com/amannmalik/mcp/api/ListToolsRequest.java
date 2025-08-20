package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.AbstractEntityCodec;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ListToolsRequest(String cursor, JsonObject _meta) {
    public static final JsonCodec<ListToolsRequest> CODEC =
            AbstractEntityCodec.paginatedRequest(
                    ListToolsRequest::cursor,
                    ListToolsRequest::_meta,
                    ListToolsRequest::new);

    public ListToolsRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
