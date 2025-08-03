package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.PaginatedRequest;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ListToolsRequest(String cursor, JsonObject _meta) {
    public static final JsonCodec<ListToolsRequest> CODEC =
            AbstractEntityCodec.paginatedRequest(
                    r -> new PaginatedRequest(r.cursor(), r._meta()),
                    pr -> new ListToolsRequest(pr.cursor(), pr._meta()));

    public ListToolsRequest {
        MetaValidator.requireValid(_meta);
    }
}
