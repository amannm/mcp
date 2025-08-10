package com.amannmalik.mcp.codec;

import jakarta.json.*;

import java.util.function.Function;

public final class ResourceEntityMetaCodec<T> extends AbstractEntityCodec<T> {
    private final Function<T, JsonObject> meta;
    private final Function<JsonObject, T> from;

    public ResourceEntityMetaCodec(Function<T, JsonObject> meta, Function<JsonObject, T> from) {
        this.meta = meta;
        this.from = from;
    }

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
}
