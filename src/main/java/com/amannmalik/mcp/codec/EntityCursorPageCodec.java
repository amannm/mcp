package com.amannmalik.mcp.codec;

import jakarta.json.*;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class EntityCursorPageCodec<T> extends AbstractEntityCodec<T> {
    private final Function<T, String> cursor;
    private final Function<T, JsonObject> meta;
    private final BiFunction<String, JsonObject, T> from;

    public EntityCursorPageCodec(Function<T, String> cursor, Function<T, JsonObject> meta, BiFunction<String, JsonObject, T> from) {
        this.cursor = cursor;
        this.meta = meta;
        this.from = from;
    }

    @Override
    public JsonObject toJson(T value) {
        var b = Json.createObjectBuilder();
        var c = cursor.apply(value);
        if (c != null) b.add("cursor", c);
        var m = meta.apply(value);
        if (m != null) b.add("_meta", m);
        return b.build();
    }

    @Override
    public T fromJson(JsonObject obj) {
        if (obj == null) return from.apply(null, null);
        requireOnlyKeys(obj, REQUEST_KEYS);
        String c = null;
        if (obj.containsKey("cursor")) {
            try {
                c = obj.getString("cursor");
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("cursor must be a string");
            }
        }
        return from.apply(c, obj.getJsonObject("_meta"));
    }
}
