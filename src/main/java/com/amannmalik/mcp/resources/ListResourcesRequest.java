package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.PaginatedRequest;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ListResourcesRequest(String cursor, JsonObject _meta) {
    public static final JsonCodec<ListResourcesRequest> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ListResourcesRequest req) {
            return AbstractEntityCodec.toJson(new PaginatedRequest(req.cursor(), req._meta()));
        }

        @Override
        public ListResourcesRequest fromJson(JsonObject obj) {
            PaginatedRequest pr = AbstractEntityCodec.fromPaginatedRequest(obj);
            return new ListResourcesRequest(pr.cursor(), pr._meta());
        }
    };

    public ListResourcesRequest {
        MetaValidator.requireValid(_meta);
    }
}
