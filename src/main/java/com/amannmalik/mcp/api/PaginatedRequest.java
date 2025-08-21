package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.AbstractEntityCodec;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record PaginatedRequest(String cursor, JsonObject _meta) {
    public static final JsonCodec<PaginatedRequest> CODEC =
            AbstractEntityCodec.paginatedRequest(
                    PaginatedRequest::cursor,
                    PaginatedRequest::_meta,
                    PaginatedRequest::new);

    public PaginatedRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
