package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.PaginatedRequest;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ListResourceTemplatesRequest(String cursor, JsonObject _meta) {
    public static final JsonCodec<ListResourceTemplatesRequest> CODEC =
            AbstractEntityCodec.paginatedRequest(
                    r -> new PaginatedRequest(r.cursor(), r._meta()),
                    pr -> new ListResourceTemplatesRequest(pr.cursor(), pr._meta()));

    public ListResourceTemplatesRequest {
        MetaValidator.requireValid(_meta);
    }
}
