package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.*;

public record ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates,
                                          String nextCursor,
                                          JsonObject _meta) {
    public static final JsonCodec<ListResourceTemplatesResult> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ListResourceTemplatesResult r) {
            return AbstractEntityCodec.paginated(
                    "resourceTemplates",
                    new Pagination.Page<>(r.resourceTemplates(), r.nextCursor()),
                    ResourceTemplate.CODEC::toJson,
                    r._meta());
        }

        @Override
        public ListResourceTemplatesResult fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            JsonArray arr = obj.getJsonArray("resourceTemplates");
            if (arr == null) throw new IllegalArgumentException("resourceTemplates required");
            List<ResourceTemplate> templates = new ArrayList<>();
            for (JsonValue v : arr) {
                if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                    throw new IllegalArgumentException("resourceTemplate must be object");
                }
                templates.add(ResourceTemplate.CODEC.fromJson(v.asJsonObject()));
            }
            PaginatedResult pr = AbstractEntityCodec.fromPaginatedResult(obj);
            return new ListResourceTemplatesResult(templates, pr.nextCursor(), pr._meta());
        }
    };

    public ListResourceTemplatesResult {
        resourceTemplates = Immutable.list(resourceTemplates);
        MetaValidator.requireValid(_meta);
    }
}
