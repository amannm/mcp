package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.PaginatedRequest;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ListToolsRequest(String cursor, JsonObject _meta) {
    public static final JsonCodec<ListToolsRequest> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ListToolsRequest req) {
            return AbstractEntityCodec.toJson(new PaginatedRequest(req.cursor(), req._meta()));
        }

        @Override
        public ListToolsRequest fromJson(JsonObject obj) {
            PaginatedRequest pr = AbstractEntityCodec.fromPaginatedRequest(obj);
            return new ListToolsRequest(pr.cursor(), pr._meta());
        }
    };

    public ListToolsRequest {
        MetaValidator.requireValid(_meta);
    }
}
