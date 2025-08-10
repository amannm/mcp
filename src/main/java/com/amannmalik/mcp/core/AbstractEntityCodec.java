package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.AnnotationsJsonCodec;
import com.amannmalik.mcp.util.ContentBlockJsonCodec;
import jakarta.json.*;

import java.util.*;
import java.util.function.*;

public abstract class AbstractEntityCodec<T> implements JsonCodec<T> {

    protected static final JsonCodec<Annotations> ANNOTATIONS_CODEC = new AnnotationsJsonCodec();
    protected static final JsonCodec<ContentBlock> CONTENT_BLOCK_CODEC = new ContentBlockJsonCodec();

    private static final Set<String> REQUEST_KEYS = Set.of("cursor", "_meta");
    private static final Set<String> META_KEYS = Set.of("_meta");

    public static <T> JsonObject paginated(String field, Pagination.Page<T> page, Function<T, JsonValue> encoder, JsonObject meta) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        page.items().forEach(item -> arr.add(encoder.apply(item)));
        JsonObjectBuilder b = Json.createObjectBuilder().add(field, arr.build());
        if (page.nextCursor() != null) b.add("nextCursor", page.nextCursor());
        if (meta != null) b.add("_meta", meta);
        return b.build();
    }

    public static <T> JsonCodec<T> paginatedRequest(
            Function<T, String> cursor,
            Function<T, JsonObject> meta,
            BiFunction<String, JsonObject, T> from) {
        return new AbstractEntityCodec<>() {
            @Override
            public JsonObject toJson(T value) {
                JsonObjectBuilder b = Json.createObjectBuilder();
                String c = cursor.apply(value);
                if (c != null) b.add("cursor", c);
                JsonObject m = meta.apply(value);
                if (m != null) b.add("_meta", m);
                return b.build();
            }

            @Override
            public T fromJson(JsonObject obj) {
                if (obj == null) return from.apply(null, null);
                requireOnlyKeys(obj, REQUEST_KEYS);
                return from.apply(obj.getString("cursor", null), obj.getJsonObject("_meta"));
            }
        };
    }

    public static <I, R> JsonCodec<R> paginatedResult(
            String field,
            String itemName,
            Function<R, Pagination.Page<I>> toPage,
            Function<R, JsonObject> meta,
            JsonCodec<I> itemCodec,
            BiFunction<Pagination.Page<I>, JsonObject, R> from) {
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
                String next = obj.getString("nextCursor", null);
                JsonObject m = obj.getJsonObject("_meta");
                requireOnlyKeys(obj, Set.of(field, "nextCursor", "_meta"));
                return from.apply(new Pagination.Page<>(items, next), m);
            }
        };
    }

    public static <T> JsonCodec<T> metaOnly(
            Function<T, JsonObject> meta,
            Function<JsonObject, T> from) {
        return new AbstractEntityCodec<>() {
            @Override
            public JsonObject toJson(T value) {
                JsonObjectBuilder b = Json.createObjectBuilder();
                JsonObject m = meta.apply(value);
                if (m != null) b.add("_meta", m);
                return b.build();
            }

            @Override
            public T fromJson(JsonObject obj) {
                if (obj == null) return from.apply(null);
                AbstractEntityCodec.requireOnlyKeys(obj, META_KEYS);
                return from.apply(obj.getJsonObject("_meta"));
            }
        };
    }

    public static <T> JsonCodec<T> empty(Supplier<T> from) {
        return new AbstractEntityCodec<>() {
            @Override
            public JsonObject toJson(T value) {
                return JsonValue.EMPTY_JSON_OBJECT;
            }

            @Override
            public T fromJson(JsonObject obj) {
                if (obj != null && !obj.isEmpty()) throw new IllegalArgumentException("unexpected fields");
                return from.get();
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

    protected static Role requireRole(JsonObject obj) {
        return Role.valueOf(requireString(obj, "role").toUpperCase());
    }

    protected static ContentBlock requireContent(JsonObject obj) {
        JsonObject c = getObject(obj, "content");
        if (c == null) throw new IllegalArgumentException("content required");
        return CONTENT_BLOCK_CODEC.fromJson(c);
    }

    public static void requireOnlyKeys(JsonObject obj, Set<String> allowed) {
        Objects.requireNonNull(obj);
        Objects.requireNonNull(allowed);
        for (String key : obj.keySet()) {
            if (!allowed.contains(key)) throw new IllegalArgumentException("unexpected field: " + key);
        }
    }
}
