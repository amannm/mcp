package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

/// - [Pagination](specification/2025-06-18/server/utilities/pagination.mdx)
public sealed interface PaginatedResult<T> permits
        ListResourcesResult,
        ListResourceTemplatesResult,
        ListToolsResult {

    static <U> List<U> items(List<U> items) {
        return Immutable.list(items);
    }

    static JsonObject meta(JsonObject meta) {
        ValidationUtil.requireMeta(meta);
        return meta;
    }

    List<T> items();

    String nextCursor();

    JsonObject _meta();
}
