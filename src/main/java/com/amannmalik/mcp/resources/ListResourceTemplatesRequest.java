package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.transport.AbstractEntityCodec;
import com.amannmalik.mcp.transport.JsonCodec;
import com.amannmalik.mcp.validation.ValidationUtil;
import jakarta.json.JsonObject;

public record ListResourceTemplatesRequest(String cursor, JsonObject _meta) {
    public static final JsonCodec<ListResourceTemplatesRequest> CODEC =
            AbstractEntityCodec.paginatedRequest(
                    ListResourceTemplatesRequest::cursor,
                    ListResourceTemplatesRequest::_meta,
                    ListResourceTemplatesRequest::new);

    public ListResourceTemplatesRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
