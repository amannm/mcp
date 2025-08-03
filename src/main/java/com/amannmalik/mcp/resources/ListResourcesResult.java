package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.*;

public record ListResourcesResult(List<Resource> resources,
                                  String nextCursor,
                                  JsonObject _meta) {
    public static final JsonCodec<ListResourcesResult> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ListResourcesResult r) {
            return AbstractEntityCodec.paginated(
                    "resources",
                    new Pagination.Page<>(r.resources(), r.nextCursor()),
                    Resource.CODEC::toJson,
                    r._meta());
        }

        @Override
        public ListResourcesResult fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            JsonArray arr = obj.getJsonArray("resources");
            if (arr == null) throw new IllegalArgumentException("resources required");
            List<Resource> resources = new ArrayList<>();
            for (JsonValue v : arr) {
                if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                    throw new IllegalArgumentException("resource must be object");
                }
                resources.add(Resource.CODEC.fromJson(v.asJsonObject()));
            }
            PaginatedResult pr = AbstractEntityCodec.fromPaginatedResult(obj);
            return new ListResourcesResult(resources, pr.nextCursor(), pr._meta());
        }
    };

    public ListResourcesResult {
        resources = Immutable.list(resources);
        MetaValidator.requireValid(_meta);
    }
}
