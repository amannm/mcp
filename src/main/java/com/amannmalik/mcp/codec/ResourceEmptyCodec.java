package com.amannmalik.mcp.codec;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.function.Supplier;

public final class ResourceEmptyCodec<T> extends AbstractEntityCodec<T> {
    private final Supplier<T> from;

    public ResourceEmptyCodec(Supplier<T> from) {
        this.from = from;
    }

    @Override
    public JsonObject toJson(T value) {
        return JsonValue.EMPTY_JSON_OBJECT;
    }

    @Override
    public T fromJson(JsonObject obj) {
        if (obj != null && !obj.isEmpty()) throw new IllegalArgumentException("unexpected fields");
        return from.get();
    }
}
