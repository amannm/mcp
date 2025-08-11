package com.amannmalik.mcp.codec;

import jakarta.json.JsonObject;

interface JsonCodec<T> {
    JsonObject toJson(T entity);

    T fromJson(JsonObject json);
}
