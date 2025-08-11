package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.api.McpHostConfiguration;
import com.amannmalik.mcp.util.Immutable;

import java.util.List;

/// - [Pagination](specification/2025-06-18/server/utilities/pagination.mdx)
public final class Pagination {
    public static final int DEFAULT_PAGE_SIZE =
            McpHostConfiguration.defaultConfiguration().defaultPageSize();

    private Pagination() {
    }

    public static <T> Page<T> page(List<T> items, Cursor cursor, int size) {
        int start = Cursor.index(cursor);
        if (start > items.size()) throw new IllegalArgumentException("Invalid cursor");
        int end = Math.min(items.size(), start + size);
        List<T> slice = items.subList(start, end);
        Cursor next = end < items.size() ? Cursor.fromIndex(end) : Cursor.End.INSTANCE;
        return new Page<>(slice, next);
    }

    public record Page<T>(List<T> items, Cursor nextCursor) {
        public Page {
            items = Immutable.list(items);
            nextCursor = nextCursor == null ? Cursor.End.INSTANCE : nextCursor;
        }
    }
}
