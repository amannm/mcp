package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.*;

public record ListToolsResult(List<Tool> tools,
                              String nextCursor,
                              JsonObject _meta) {
    public static final JsonCodec<ListToolsResult> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ListToolsResult r) {
            return AbstractEntityCodec.paginated(
                    "tools",
                    new Pagination.Page<>(r.tools(), r.nextCursor()),
                    Tool.CODEC::toJson,
                    r._meta());
        }

        @Override
        public ListToolsResult fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            JsonArray arr = obj.getJsonArray("tools");
            if (arr == null) throw new IllegalArgumentException("tools required");
            List<Tool> tools = new ArrayList<>();
            for (JsonValue v : arr) {
                if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                    throw new IllegalArgumentException("tool must be object");
                }
                tools.add(Tool.CODEC.fromJson(v.asJsonObject()));
            }
            PaginatedResult pr = AbstractEntityCodec.fromPaginatedResult(obj);
            return new ListToolsResult(tools, pr.nextCursor(), pr._meta());
        }
    };

    public ListToolsResult {
        tools = Immutable.list(tools);
        MetaValidator.requireValid(_meta);
    }
}
