package com.amannmalik.mcp.codec;

import jakarta.json.*;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class AbstractEntityPageCodec<T> extends AbstractEntityCodec<T> {
    private final Function<T, String> cursor;
    private final Function<T, JsonObject> meta;
    private final BiFunction<String, JsonObject, T> from;

    public AbstractEntityPageCodec(Function<T, String> cursor, Function<T, JsonObject> meta, BiFunction<String, JsonObject, T> from) {
        this.cursor = cursor;
        this.meta = meta;
        this.from = from;
    }

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
}
