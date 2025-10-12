package com.amannmalik.mcp.spi;

import jakarta.json.JsonObject;

sealed interface ExecutingProvider<T, R> extends Provider<T> permits
        CompletionProvider,
        ElicitationProvider,
        SamplingProvider,
        ToolProvider {
    @Override
    Pagination.Page<T> list(Cursor cursor);

    R execute(String name, JsonObject args) throws InterruptedException;
}
