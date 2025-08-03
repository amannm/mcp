package com.amannmalik.mcp.core;

import com.amannmalik.mcp.util.PaginatedRequest;
import com.amannmalik.mcp.util.PaginatedResult;
import com.amannmalik.mcp.util.Pagination;
import jakarta.json.*;

public abstract class AbstractEntityCodec<T> implements JsonCodec<T> {
    protected IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message);
    }

    protected JsonObjectBuilder object() {
        return Json.createObjectBuilder();
    }

    protected JsonArrayBuilder array() {
        return Json.createArrayBuilder();
    }

    protected String requireString(JsonObject obj, String key) {
        if (!obj.containsKey(key)) throw error(key + " required");
        return obj.getString(key);
    }

    protected String getString(JsonObject obj, String key) {
        return obj.getString(key, null);
    }

    protected JsonObject requireObject(JsonObject obj, String key) {
        if (!obj.containsKey(key)) throw error(key + " required");
        return obj.getJsonObject(key);
    }

    protected JsonObject getObject(JsonObject obj, String key) {
        return obj.containsKey(key) ? obj.getJsonObject(key) : null;
    }

    protected JsonArray requireArray(JsonObject obj, String key) {
        if (!obj.containsKey(key)) throw error(key + " required");
        return obj.getJsonArray(key);
    }

    protected int requireInt(JsonObject obj, String key) {
        if (!obj.containsKey(key)) throw error(key + " required");
        return obj.getInt(key);
    }

    protected Double getDouble(JsonObject obj, String key) {
        return obj.containsKey(key) ? obj.getJsonNumber(key).doubleValue() : null;
    }

    public static JsonObject toJson(PaginatedRequest req) {
        var b = Json.createObjectBuilder();
        if (req.cursor() != null) b.add("cursor", req.cursor());
        if (req._meta() != null) b.add("_meta", req._meta());
        return b.build();
    }

    public static PaginatedRequest fromPaginatedRequest(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String cursor = obj.getString("cursor", null);
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        return new PaginatedRequest(cursor, meta);
    }

    public static PaginatedResult fromPaginatedResult(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String cursor = obj.getString("nextCursor", null);
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        return new PaginatedResult(cursor, meta);
    }

    public static <T> JsonObject paginated(String key, Pagination.Page<T> page, java.util.function.Function<T, JsonObject> map, JsonObject meta) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        page.items().forEach(t -> arr.add(map.apply(t)));
        JsonObjectBuilder b = Json.createObjectBuilder().add(key, arr.build());
        if (page.nextCursor() != null) b.add("nextCursor", page.nextCursor());
        if (meta != null) b.add("_meta", meta);
        return b.build();
    }
}
