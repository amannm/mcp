package com.amannmalik.mcp.spi;

import jakarta.json.JsonObject;

sealed interface ExecutingProvider<T, R> extends Provider<T> permits
        CompletionProvider,
        ElicitationProvider,
        SamplingProvider {
    R execute(String name, JsonObject args) throws InterruptedException;
}
