package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.core.*;
import com.amannmalik.mcp.util.JsonUtil;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.*;

public record ListRootsResult(List<Root> roots, JsonObject _meta) {
    public static final JsonCodec<ListRootsResult> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(ListRootsResult result) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            result.roots().forEach(r -> arr.add(Root.CODEC.toJson(r)));
            JsonObjectBuilder b = Json.createObjectBuilder().add("roots", arr);
            if (result._meta() != null) b.add("_meta", result._meta());
            return b.build();
        }

        @Override
        public ListRootsResult fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            JsonUtil.requireOnlyKeys(obj, Set.of("roots", "_meta"));
            JsonArray arr = obj.getJsonArray("roots");
            List<Root> roots = arr == null || arr.isEmpty()
                    ? List.of()
                    : arr.stream().map(JsonValue::asJsonObject).map(Root.CODEC::fromJson).toList();
            return new ListRootsResult(roots, getObject(obj, "_meta"));
        }
    };

    public ListRootsResult {
        roots = roots == null || roots.isEmpty() ? List.of() : List.copyOf(roots);
        MetaValidator.requireValid(_meta);
    }
}
