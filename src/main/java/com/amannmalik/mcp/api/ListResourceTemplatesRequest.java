package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ListResourceTemplatesRequest(String cursor, JsonObject _meta) {
    static final JsonCodec<ListResourceTemplatesRequest> CODEC =
            AbstractEntityCodec.paginatedRequest(
                    ListResourceTemplatesRequest::cursor,
                    ListResourceTemplatesRequest::_meta,
                    ListResourceTemplatesRequest::new);

    public ListResourceTemplatesRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
