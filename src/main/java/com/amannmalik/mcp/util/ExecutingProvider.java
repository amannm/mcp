package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.Provider;
import com.amannmalik.mcp.spi.*;
import jakarta.json.JsonObject;

public sealed interface ExecutingProvider<T, R> extends Provider<T> permits
        CompletionProvider,
        ElicitationProvider,
        SamplingProvider {
    R execute(String name, JsonObject args) throws InterruptedException;
}
