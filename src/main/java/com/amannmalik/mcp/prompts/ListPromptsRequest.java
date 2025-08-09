package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.transport.AbstractEntityCodec;
import com.amannmalik.mcp.transport.JsonCodec;
import com.amannmalik.mcp.validation.ValidationUtil;
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
