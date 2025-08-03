package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.PaginatedRequest;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ListResourceTemplatesRequest(String cursor, JsonObject _meta) {
    public static final JsonCodec<ListResourceTemplatesRequest> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ListResourceTemplatesRequest req) {
            return AbstractEntityCodec.toJson(new PaginatedRequest(req.cursor(), req._meta()));
        }

        @Override
        public ListResourceTemplatesRequest fromJson(JsonObject obj) {
            PaginatedRequest pr = AbstractEntityCodec.fromPaginatedRequest(obj);
            return new ListResourceTemplatesRequest(pr.cursor(), pr._meta());
        }
    };

    public ListResourceTemplatesRequest {
        MetaValidator.requireValid(_meta);
    }
}
