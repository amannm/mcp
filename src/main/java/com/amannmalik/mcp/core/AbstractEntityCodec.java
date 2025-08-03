package com.amannmalik.mcp.core;

import com.amannmalik.mcp.util.*;
import jakarta.json.*;

import java.util.Set;
import java.util.function.Function;

public abstract class AbstractEntityCodec<T> implements JsonCodec<T> {
    private static final Set<String> REQUEST_KEYS = Set.of("cursor", "_meta");
    private static final Set<String> RESULT_KEYS = Set.of("nextCursor", "_meta");

    public static JsonObject toJson(PaginatedRequest req) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (req.cursor() != null) b.add("cursor", req.cursor());
        if (req._meta() != null) b.add("_meta", req._meta());
        return b.build();
    }

    public static PaginatedRequest fromPaginatedRequest(JsonObject obj) {
        if (obj == null) return new PaginatedRequest(null, null);
        JsonUtil.requireOnlyKeys(obj, REQUEST_KEYS);
        return new PaginatedRequest(obj.getString("cursor", null), obj.getJsonObject("_meta"));
    }

    public static JsonObject toJson(PaginatedResult result) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (result.nextCursor() != null) b.add("nextCursor", result.nextCursor());
        if (result._meta() != null) b.add("_meta", result._meta());
        return b.build();
    }

    public static PaginatedResult fromPaginatedResult(JsonObject obj) {
        if (obj == null) return new PaginatedResult(null, null);
        JsonUtil.requireOnlyKeys(obj, RESULT_KEYS);
        return new PaginatedResult(obj.getString("nextCursor", null), obj.getJsonObject("_meta"));
    }

    public static <T> JsonObject paginated(String field, Pagination.Page<T> page, Function<T, JsonValue> encoder, JsonObject meta) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        page.items().forEach(item -> arr.add(encoder.apply(item)));
        JsonObjectBuilder b = Json.createObjectBuilder().add(field, arr.build());
        toJson(new PaginatedResult(page.nextCursor(), meta)).forEach(b::add);
        return b.build();
    }

    protected static String requireString(JsonObject obj, String key) {
        String val = obj.getString(key, null);
        if (val == null) throw new IllegalArgumentException(key + " required");
        return val;
    }

    protected static JsonObject getObject(JsonObject obj, String key) {
        return obj.getJsonObject(key);
    }
}
