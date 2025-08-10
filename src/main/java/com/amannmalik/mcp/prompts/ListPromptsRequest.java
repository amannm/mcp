package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ListPromptsRequest(String cursor, JsonObject _meta) {
    public static final JsonCodec<ListPromptsRequest> CODEC =
            AbstractEntityCodec.paginatedRequest(
                    ListPromptsRequest::cursor,
                    ListPromptsRequest::_meta,
                    ListPromptsRequest::new);

    public ListPromptsRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
