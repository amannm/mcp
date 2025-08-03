package com.amannmalik.mcp.core;

import jakarta.json.JsonObject;

public interface JsonCodec<T> {
    JsonObject toJson(T entity);
    T fromJson(JsonObject json);
}
