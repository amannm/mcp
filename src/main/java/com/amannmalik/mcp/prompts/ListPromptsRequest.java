package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.PaginatedRequest;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ListPromptsRequest(String cursor, JsonObject _meta) {
    public static final JsonCodec<ListPromptsRequest> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ListPromptsRequest req) {
            return AbstractEntityCodec.toJson(new PaginatedRequest(req.cursor(), req._meta()));
        }

        @Override
        public ListPromptsRequest fromJson(JsonObject obj) {
            PaginatedRequest pr = AbstractEntityCodec.fromPaginatedRequest(obj);
            return new ListPromptsRequest(pr.cursor(), pr._meta());
        }
    };

    public ListPromptsRequest {
        MetaValidator.requireValid(_meta);
    }
}
