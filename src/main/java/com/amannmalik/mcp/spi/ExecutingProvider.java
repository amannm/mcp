package com.amannmalik.mcp.spi;

import jakarta.json.JsonObject;

import java.util.List;

sealed interface ExecutingProvider<T, R> extends Provider<T> permits
        CompletionProvider,
        ElicitationProvider,
        SamplingProvider,
        ToolProvider {
    @Override
    default Pagination.Page<T> list(Cursor cursor) {
        return new Pagination.Page<>(List.of(), Cursor.End.INSTANCE);
    }

    R execute(String name, JsonObject args) throws InterruptedException;
}
