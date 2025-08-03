package com.amannmalik.mcp.core;

import com.amannmalik.mcp.util.*;
import jakarta.json.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
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
        requireOnlyKeys(obj, REQUEST_KEYS);
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
        requireOnlyKeys(obj, RESULT_KEYS);
        return new PaginatedResult(obj.getString("nextCursor", null), obj.getJsonObject("_meta"));
    }

    public static <T> JsonObject paginated(String field, Pagination.Page<T> page, Function<T, JsonValue> encoder, JsonObject meta) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        page.items().forEach(item -> arr.add(encoder.apply(item)));
        JsonObjectBuilder b = Json.createObjectBuilder().add(field, arr.build());
        toJson(new PaginatedResult(page.nextCursor(), meta)).forEach(b::add);
        return b.build();
    }

    public static <T> JsonCodec<T> paginatedRequest(
            Function<T, PaginatedRequest> to,
            Function<PaginatedRequest, T> from) {
        return new AbstractEntityCodec<>() {
            @Override
            public JsonObject toJson(T value) {
                return AbstractEntityCodec.toJson(to.apply(value));
            }

            @Override
            public T fromJson(JsonObject obj) {
                return from.apply(fromPaginatedRequest(obj));
            }
        };
    }

    public static <I, R> JsonCodec<R> paginatedResult(
            String field,
            String itemName,
            Function<R, Pagination.Page<I>> toPage,
            Function<R, JsonObject> meta,
            JsonCodec<I> itemCodec,
            BiFunction<List<I>, PaginatedResult, R> from) {
        return new AbstractEntityCodec<>() {
            @Override
            public JsonObject toJson(R value) {
                return paginated(field, toPage.apply(value), itemCodec::toJson, meta.apply(value));
            }

            @Override
            public R fromJson(JsonObject obj) {
                if (obj == null) throw new IllegalArgumentException("object required");
                JsonArray arr = obj.getJsonArray(field);
                if (arr == null) throw new IllegalArgumentException(field + " required");
                List<I> items = new ArrayList<>();
                for (JsonValue v : arr) {
                    if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                        throw new IllegalArgumentException(itemName + " must be object");
                    }
                    items.add(itemCodec.fromJson(v.asJsonObject()));
                }
                PaginatedResult pr = fromPaginatedResult(obj);
                return from.apply(items, pr);
            }
        };
    }

    protected static String requireString(JsonObject obj, String key) {
        String val = obj.getString(key, null);
        if (val == null) throw new IllegalArgumentException(key + " required");
        return val;
    }

    protected static JsonObject getObject(JsonObject obj, String key) {
        return obj.getJsonObject(key);
    }

    public static void requireOnlyKeys(JsonObject obj, Set<String> allowed) {
        Objects.requireNonNull(obj);
        Objects.requireNonNull(allowed);
        for (String key : obj.keySet()) {
            if (!allowed.contains(key)) throw new IllegalArgumentException("unexpected field: " + key);
        }
    }
}
