package com.amannmalik.mcp.api;

import jakarta.json.JsonObject;

public interface ExecutingProvider<T, R> extends Provider<T> {
    R execute(String name, JsonObject args) throws InterruptedException;
}
