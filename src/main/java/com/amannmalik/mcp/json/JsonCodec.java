package com.amannmalik.mcp.json;

import jakarta.json.JsonValue;

public interface JsonCodec<T> {
    JsonValue toJson(T value);
    T fromJson(JsonValue json);
}
