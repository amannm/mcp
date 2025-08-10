package com.amannmalik.mcp.api.tools;

import com.amannmalik.mcp.api.JsonCodec;
import com.amannmalik.mcp.core.AbstractEntityCodec;
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
