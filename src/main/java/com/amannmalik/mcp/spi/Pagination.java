package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.api.McpHostConfiguration;
import com.amannmalik.mcp.core.CursorCodec;
import com.amannmalik.mcp.util.ValidationUtil;

import java.util.List;

/// - [Pagination](specification/2025-06-18/server/utilities/pagination.mdx)
public final class Pagination {
    public static final int DEFAULT_PAGE_SIZE =
            McpHostConfiguration.defaultConfiguration().defaultPageSize();

    private Pagination() {
    }

    public static <T> Page<T> page(List<T> items, Cursor cursor, int size) {
        var start = Cursor.index(cursor);
        if (start > items.size()) {
            throw new IllegalArgumentException("Invalid cursor");
        }
        var end = Math.min(items.size(), start + size);
        var slice = items.subList(start, end);
        var next = end < items.size() ? Cursor.fromIndex(end) : Cursor.End.INSTANCE;
        return new Page<>(slice, next);
    }

    public record Page<T>(List<T> items, Cursor nextCursor) {
        public Page {
            items = ValidationUtil.immutableList(items);
            nextCursor = CursorCodec.requireCursor(nextCursor);
        }

        @Override
        public List<T> items() {
            return ValidationUtil.copyList(items);
        }
    }
}
